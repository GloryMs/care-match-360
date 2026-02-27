package com.careprofileservice.service;


import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Standalone file storage service — no AWS, no external dependencies.
 *
 * Storage layout on disk:
 *   ${app.storage.base-dir}/
 *     <folder>/
 *       <uuid>.<ext>.gz.enc   ← compressed + AES-256-GCM encrypted file
 *       <uuid>.<ext>.meta     ← JSON sidecar with IV + original metadata
 *
 * Environment / application.properties keys:
 *   app.storage.base-dir          (default: ./carematch-storage)
 *   app.storage.encryption-key    (32-byte base64 AES key; auto-generated if absent)
 *   app.storage.max-file-size     (default: 10485760 = 10 MB)
 *   app.storage.base-url          (e.g. http://localhost:8082  – used to build download URLs)
 */
@Service
@Slf4j
public class FileStorageService {

    // ── Constants ────────────────────────────────────────────────────────────

    private static final String AES_ALGO     = "AES/GCM/NoPadding";
    private static final int    GCM_TAG_BITS = 128;
    private static final int    IV_BYTES     = 12;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
    private static final List<String> ALLOWED_VIDEO_TYPES = List.of(
            "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo");
    private static final List<String> ALLOWED_DOCUMENT_TYPES = List.of(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    // ── Configuration ────────────────────────────────────────────────────────

    @Value("${app.storage.base-dir:./carematch-storage}")
    private String baseDir;

    /** Base64-encoded 32-byte AES-256 key. Generated once and logged on first use if absent. */
    @Value("${app.storage.encryption-key:}")
    private String encryptionKeyBase64;

    @Value("${app.storage.max-file-size:10485760}")
    private long maxFileSize;

    /** Public-facing base URL for building download links (no trailing slash). */
    @Value("${app.storage.base-url:http://localhost:8082}")
    private String baseUrl;

    // ── Lazy-initialised key ─────────────────────────────────────────────────

    private SecretKey secretKey;

    private SecretKey getSecretKey() {
        if (secretKey != null) return secretKey;

        if (encryptionKeyBase64 != null && !encryptionKeyBase64.isBlank()) {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64.trim());
                if (keyBytes.length != 32) {
                    log.warn("Encryption key must be exactly 32 bytes for AES-256, got {}. " +
                             "Falling back to auto-generation.", keyBytes.length);
                } else {
                    secretKey = new SecretKeySpec(keyBytes, "AES");
                    return secretKey;
                }
            } catch (IllegalArgumentException e) {
                log.warn("app.storage.encryption-key is not valid Base64 ({}). " +
                         "Falling back to auto-generation.", e.getMessage());
            }
        }

        // Auto-generate and log — operator must persist this key!
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256, new SecureRandom());
            secretKey = kg.generateKey();
            String b64 = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            log.warn("===================================================");
            log.warn("No valid encryption key configured (app.storage.encryption-key).");
            log.warn("Auto-generated key (save this in application.properties!):");
            log.warn("app.storage.encryption-key={}", b64);
            log.warn("===================================================");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
        return secretKey;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Upload, compress, and encrypt a file.
     *
     * @param file       the multipart upload
     * @param folder     logical folder (e.g. "patients/&lt;uuid&gt;")
     * @param fileType   "image" | "video" | "document"
     * @return           relative storage key (folder/uuid.ext.gz.enc)
     */
    public String uploadFile(MultipartFile file, String folder, String fileType) {
        validateFile(file, fileType);

        String fileName   = generateFileName(file.getOriginalFilename());
        String key        = folder + "/" + fileName;           // logical key
        Path   targetPath = resolveStoragePath(key + ".gz.enc");
        Path   metaPath   = resolveStoragePath(key + ".meta");

        try {
            Files.createDirectories(targetPath.getParent());

            byte[] raw         = file.getBytes();
            byte[] compressed  = compress(raw);
            byte[] iv          = generateIv();
            byte[] encrypted   = encrypt(compressed, iv);

            Files.write(targetPath, encrypted, StandardOpenOption.CREATE_NEW);

            // Write sidecar metadata (IV + original info)
            String meta = buildMeta(iv, file.getOriginalFilename(),
                    file.getContentType(), file.getSize());
            Files.writeString(metaPath, meta, StandardOpenOption.CREATE_NEW);

            log.info("Stored file: key={}, originalSize={}, encryptedSize={}",
                    key, file.getSize(), encrypted.length);
            return key;

        } catch (Exception e) {
            log.error("Failed to store file: key={}", key, e);
            // Clean up partial writes
            silentDelete(targetPath);
            silentDelete(metaPath);
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    /**
     * Build a public download URL from a storage key.
     * The URL points to the /files/download endpoint in FileDownloadController.
     */
    public String getPublicUrl(String key) {
        // Encode the key so slashes survive URL routing
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(key.getBytes());
        return baseUrl + "/api/v1/files/download/" + encoded;
    }

    /**
     * Decrypt and decompress a file, returning its raw bytes.
     *
     * @param key  the logical key returned by uploadFile
     * @return     original, uncompressed file bytes
     */
    public byte[] downloadFile(String key) {
        Path encPath  = resolveStoragePath(key + ".gz.enc");
        Path metaPath = resolveStoragePath(key + ".meta");

        if (!Files.exists(encPath)) {
            throw new RuntimeException("File not found: " + key);
        }

        try {
            byte[] iv         = readIvFromMeta(metaPath);
            byte[] encrypted  = Files.readAllBytes(encPath);
            byte[] compressed = decrypt(encrypted, iv);
            byte[] raw        = decompress(compressed);
            log.debug("Downloaded file: key={}, size={}", key, raw.length);
            return raw;
        } catch (Exception e) {
            log.error("Failed to download file: key={}", key, e);
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }

    /**
     * Return the content-type for the stored file (read from .meta sidecar).
     */
    public String getContentType(String key) {
        Path metaPath = resolveStoragePath(key + ".meta");
        try {
            String meta = Files.readString(metaPath);
            // Simple key=value parser (avoids a JSON library dependency)
            return extractMetaField(meta, "contentType").orElse("application/octet-stream");
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    /**
     * Return the original filename from the sidecar.
     */
    public String getOriginalFilename(String key) {
        Path metaPath = resolveStoragePath(key + ".meta");
        try {
            String meta = Files.readString(metaPath);
            return extractMetaField(meta, "originalFilename").orElse("file");
        } catch (IOException e) {
            return "file";
        }
    }

    /**
     * Delete the encrypted file and its sidecar from disk.
     */
    public void deleteFile(String key) {
        silentDelete(resolveStoragePath(key + ".gz.enc"));
        silentDelete(resolveStoragePath(key + ".meta"));
        log.info("Deleted file: key={}", key);
    }

    /**
     * Check whether a stored file exists on disk.
     */
    public boolean fileExists(String key) {
        return Files.exists(resolveStoragePath(key + ".gz.enc"));
    }

    // ── Spring Resource helper (for streaming downloads) ─────────────────────

    /**
     * Load the raw (decrypted + decompressed) bytes as a Spring Resource.
     * Writes a temp file so callers can stream it without holding everything in memory.
     */
    public Resource loadAsResource(String key) throws IOException {
        byte[] raw = downloadFile(key);
        Path tmp   = Files.createTempFile("carematch-dl-", getExtension(key));
        tmp.toFile().deleteOnExit();
        Files.write(tmp, raw);
        return new UrlResource(tmp.toUri());
    }

    // ── Compression ──────────────────────────────────────────────────────────

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }

    private byte[] decompress(byte[] compressed) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            byte[] buf = new byte[8192];
            int    n;
            while ((n = gzip.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
        }
        return baos.toByteArray();
    }

    // ── Encryption / Decryption ───────────────────────────────────────────────

    private byte[] encrypt(byte[] data, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(),
                new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(data);
    }

    private byte[] decrypt(byte[] data, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(),
                new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(data);
    }

    private byte[] generateIv() {
        byte[] iv = new byte[IV_BYTES];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    // ── Metadata sidecar helpers ──────────────────────────────────────────────

    private String buildMeta(byte[] iv, String originalFilename,
                             String contentType, long fileSize) {
        return "iv=" + Base64.getEncoder().encodeToString(iv) + "\n"
                + "originalFilename=" + nvl(originalFilename) + "\n"
                + "contentType=" + nvl(contentType) + "\n"
                + "fileSize=" + fileSize + "\n"
                + "storedAt=" + java.time.Instant.now() + "\n";
    }

    private byte[] readIvFromMeta(Path metaPath) throws IOException {
        String meta = Files.readString(metaPath);
        String ivB64 = extractMetaField(meta, "iv")
                .orElseThrow(() -> new IOException("Missing IV in metadata"));
        return Base64.getDecoder().decode(ivB64);
    }

    private Optional<String> extractMetaField(String meta, String field) {
        return Arrays.stream(meta.split("\n"))
                .filter(line -> line.startsWith(field + "="))
                .map(line -> line.substring(field.length() + 1).trim())
                .findFirst();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file, String fileType) {
        if (file.isEmpty()) {
            throw new ValidationException("File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new ValidationException(
                    "File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + " MB");
        }
        String ct = file.getContentType();
        if (ct == null) {
            throw new ValidationException("File content type is unknown");
        }
        boolean valid = switch (fileType.toLowerCase()) {
            case "image"    -> ALLOWED_IMAGE_TYPES.contains(ct.toLowerCase());
            case "video"    -> ALLOWED_VIDEO_TYPES.contains(ct.toLowerCase());
            case "document" -> ALLOWED_DOCUMENT_TYPES.contains(ct.toLowerCase());
            default         -> false;
        };
        if (!valid) {
            throw new ValidationException("File type not allowed: " + ct);
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Resolve a logical key to an absolute path under baseDir. */
    private Path resolveStoragePath(String relativePath) {
        Path base = Path.of(baseDir).toAbsolutePath().normalize();
        Path full = base.resolve(relativePath).normalize();
        // Guard against path traversal
        if (!full.startsWith(base)) {
            throw new SecurityException("Illegal path traversal attempt: " + relativePath);
        }
        return full;
    }

    private String generateFileName(String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + ext;
    }

    private String getExtension(String key) {
        int dot = key.lastIndexOf('.');
        return dot >= 0 ? key.substring(dot) : "";
    }

    private void silentDelete(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
