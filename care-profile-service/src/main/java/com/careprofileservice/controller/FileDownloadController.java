package com.careprofileservice.controller;

import com.careprofileservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Base64;

/**
 * Serves encrypted-on-disk files to authenticated callers.
 *
 * GET /api/v1/files/download/{encodedKey}
 *   encodedKey = Base64-URL (no padding) of the logical storage key
 *
 * GET /api/v1/files/info/{encodedKey}
 *   Returns content-type and original filename without streaming the file.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileDownloadController {

    private final FileStorageService fileStorageService;

    /**
     * Stream a stored file to the caller.
     * The file is decrypted and decompressed on the fly before sending.
     */
    @GetMapping("/download/{encodedKey}")
    public ResponseEntity<Resource> download(@PathVariable String encodedKey) throws IOException {
        String key = decodeKey(encodedKey);

        if (!fileStorageService.fileExists(key)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource    = fileStorageService.loadAsResource(key);
        String  contentType  = fileStorageService.getContentType(key);
        String  filename     = fileStorageService.getOriginalFilename(key);

        log.debug("Serving file download: key={}, contentType={}", key, contentType);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Inline preview (browser renders it instead of downloading).
     * Useful for images and PDFs embedded in the frontend.
     */
    @GetMapping("/view/{encodedKey}")
    public ResponseEntity<Resource> view(@PathVariable String encodedKey) throws IOException {
        String key = decodeKey(encodedKey);

        if (!fileStorageService.fileExists(key)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource   = fileStorageService.loadAsResource(key);
        String  contentType = fileStorageService.getContentType(key);
        String  filename    = fileStorageService.getOriginalFilename(key);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Lightweight info endpoint — no file streaming, just metadata.
     */
    @GetMapping("/info/{encodedKey}")
    public ResponseEntity<FileInfoResponse> info(@PathVariable String encodedKey) {
        String key = decodeKey(encodedKey);

        if (!fileStorageService.fileExists(key)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new FileInfoResponse(
                key,
                fileStorageService.getOriginalFilename(key),
                fileStorageService.getContentType(key),
                fileStorageService.getPublicUrl(key)
        ));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private String decodeKey(String encodedKey) {
        try {
            return new String(Base64.getUrlDecoder().decode(encodedKey));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid file key encoding");
        }
    }

    /** Simple DTO — no Lombok/Jackson needed for a 4-field record. */
    public record FileInfoResponse(
            String key,
            String originalFilename,
            String contentType,
            String downloadUrl
    ) {}
}