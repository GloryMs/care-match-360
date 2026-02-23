# Care Match 360 — API Documentation

> For the frontend team. All services are RESTful JSON APIs. Dates use ISO 8601 format (`yyyy-MM-ddTHH:mm:ss`). All IDs are UUIDs.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Authentication & Headers](#2-authentication--headers)
3. [Standard Response Format](#3-standard-response-format)
4. [Error Format](#4-error-format)
5. [Care Identity Service — Port 8001](#5-care-identity-service--port-8001)
    - [Auth Endpoints](#51-auth-endpoints)
    - [User Endpoints](#52-user-endpoints)
    - [Two-Factor Auth Endpoints](#53-two-factor-auth-2fa-endpoints)
6. [Care Profile Service — Port 8002](#6-care-profile-service--port-8002)
    - [Patient Profile Endpoints](#61-patient-profile-endpoints)
    - [Provider Profile Endpoints](#62-provider-profile-endpoints)
7. [Care Match Service — Port 8003](#7-care-match-service--port-8003)
    - [Match Endpoints](#71-match-endpoints)
    - [Offer Endpoints](#72-offer-endpoints)
8. [Care Billing Service — Port 8004](#8-care-billing-service--port-8004)
    - [Subscription Endpoints](#81-subscription-endpoints)
    - [Invoice Endpoints](#82-invoice-endpoints)
    - [Webhook Endpoints](#83-webhook-endpoints)
9. [Care Notification Service — Port 8005](#9-care-notification-service--port-8005)
    - [Notification Endpoints](#91-notification-endpoints)
    - [Analytics Endpoints](#92-analytics-endpoints)
10. [Enumerations Reference](#10-enumerations-reference)
11. [Subscription Plans Reference](#11-subscription-plans-reference)

---

## 1. Architecture Overview

| Service | Base URL | Port | Responsibility |
|---|---|---|---|
| Care Identity | `http://localhost:8001/api/v1` | 8001 | Authentication, users, 2FA |
| Care Profile | `http://localhost:8002/api/v1` | 8002 | Patient & provider profiles, documents |
| Care Match | `http://localhost:8003/api/v1` | 8003 | Match scoring, offers |
| Care Billing | `http://localhost:8004/api/v1` | 8004 | Subscriptions, invoices, payments |
| Care Notification | `http://localhost:8005/api/v1` | 8005 | Notifications, analytics, event logs |

---

## 2. Authentication & Headers

All protected endpoints require a **JWT Bearer token** obtained from the login endpoint.

```
Authorization: Bearer <accessToken>
```

Many endpoints also require the resolved user ID (injected by the API gateway or passed directly during development):

```
X-User-Id: <uuid>
```

**Token Details**

| Property | Value |
|---|---|
| Access token lifetime | 15 minutes |
| Refresh token lifetime | 7 days |
| Token type | `Bearer` |

---

## 3. Standard Response Format

Every successful response is wrapped in `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { },
  "message": "Operation successful",
  "timestamp": "2026-02-22T10:30:00"
}
```

Paginated responses use `PageResponse<T>` as the `data` value:

```json
{
  "success": true,
  "data": {
    "content": [ ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false
  },
  "message": "OK",
  "timestamp": "2026-02-22T10:30:00"
}
```

---

## 4. Error Format

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": {
      "email": "Email should be valid",
      "password": "Password must be between 8 and 100 characters"
    }
  },
  "timestamp": "2026-02-22T10:30:00"
}
```

---

## 5. Care Identity Service — Port 8001

Base path: `/api/v1`

---

### 5.1 Auth Endpoints

#### `POST /auth/register`

Register a new user account.

**Request Body**

| Field | Type | Required | Validation |
|---|---|---|---|
| `email` | `string` | Yes | Valid email format |
| `password` | `string` | Yes | 8–100 characters |
| `role` | `string` (enum) | Yes | See [UserRole](#userrole) |

```json
{
  "email": "user@example.com",
  "password": "SecurePass123",
  "role": "PATIENT"
}
```

**Response** — `ApiResponse<UserResponse>`

```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "role": "PATIENT",
    "isVerified": false,
    "isActive": true,
    "twoFactorEnabled": false,
    "createdAt": "2026-02-22T10:00:00",
    "updatedAt": "2026-02-22T10:00:00"
  },
  "message": "User registered successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `POST /auth/login`

Authenticate and obtain tokens.

**Request Body**

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` | `string` | Yes | |
| `password` | `string` | Yes | |
| `twoFactorCode` | `string` | No | Required only if 2FA is enabled |

```json
{
  "email": "user@example.com",
  "password": "SecurePass123",
  "twoFactorCode": "123456"
}
```

**Response** — `ApiResponse<AuthResponse>`

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "role": "PATIENT",
      "isVerified": true,
      "isActive": true,
      "twoFactorEnabled": false,
      "createdAt": "2026-02-22T10:00:00",
      "updatedAt": "2026-02-22T10:00:00"
    }
  },
  "message": "Login successful",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `POST /auth/refresh-token`

Exchange a refresh token for a new access token.

**Request Body**

| Field | Type | Required |
|---|---|---|
| `refreshToken` | `string` | Yes |

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** — `ApiResponse<AuthResponse>` _(same shape as login response)_

---

#### `POST /auth/logout`

Invalidate the current session.

**Headers**

| Header | Required |
|---|---|
| `X-User-Id` | Yes |

**Response** — `ApiResponse<null>`

```json
{
  "success": true,
  "data": null,
  "message": "Logged out successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `POST /auth/verify-email`

Verify the user's email address using the token sent by email.

**Request Body**

| Field | Type | Required |
|---|---|---|
| `token` | `string` | Yes |

```json
{
  "token": "abc123verificationtoken"
}
```

**Response** — `ApiResponse<null>`

---

#### `POST /auth/resend-verification`

Resend the email verification link.

**Request Body**

| Field | Type | Required |
|---|---|---|
| `email` | `string` | Yes |

```json
{
  "email": "user@example.com"
}
```

**Response** — `ApiResponse<null>`

---

#### `POST /auth/forgot-password`

Request a password reset email.

**Request Body**

| Field | Type | Required | Validation |
|---|---|---|---|
| `email` | `string` | Yes | Valid email format |

```json
{
  "email": "user@example.com"
}
```

**Response** — `ApiResponse<null>`

---

#### `POST /auth/reset-password`

Reset the password using the token from the reset email.

**Request Body**

| Field | Type | Required | Validation |
|---|---|---|---|
| `token` | `string` | Yes | |
| `newPassword` | `string` | Yes | 8–100 characters |

```json
{
  "token": "abc123resettoken",
  "newPassword": "NewSecurePass123"
}
```

**Response** — `ApiResponse<null>`

---

#### `POST /auth/change-password`

Change the password for the currently authenticated user.

**Headers**

| Header | Required |
|---|---|
| `X-User-Id` | Yes |

**Request Body**

| Field | Type | Required | Validation |
|---|---|---|---|
| `currentPassword` | `string` | Yes | |
| `newPassword` | `string` | Yes | 8–100 characters |

```json
{
  "currentPassword": "OldPass123",
  "newPassword": "NewSecurePass456"
}
```

**Response** — `ApiResponse<null>`

---

### 5.2 User Endpoints

#### `GET /users/{userId}`

Get a user by their UUID.

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `userId` | `UUID` | Target user ID |

**Response** — `ApiResponse<UserResponse>`

```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "role": "PATIENT",
    "isVerified": true,
    "isActive": true,
    "twoFactorEnabled": false,
    "createdAt": "2026-02-22T10:00:00",
    "updatedAt": "2026-02-22T10:00:00"
  },
  "message": "OK",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `GET /users/email/{email}`

Look up a user by their email address.

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `email` | `string` | User's email address |

**Response** — `ApiResponse<UserResponse>` _(same shape as above)_

---

#### `GET /users/me`

Get the profile of the currently authenticated user.

**Headers**

| Header | Required |
|---|---|
| `X-User-Id` | Yes |

**Response** — `ApiResponse<UserResponse>` _(same shape as above)_

---

#### `PUT /users/{userId}/deactivate`

Deactivate a user account.

**Path Parameters**

| Parameter | Type |
|---|---|
| `userId` | `UUID` |

**Response** — `ApiResponse<UserResponse>`

---

#### `PUT /users/{userId}/activate`

Reactivate a previously deactivated user account.

**Path Parameters**

| Parameter | Type |
|---|---|
| `userId` | `UUID` |

**Response** — `ApiResponse<UserResponse>`

---

### 5.3 Two-Factor Auth (2FA) Endpoints

All 2FA endpoints require the `X-User-Id` header.

#### `POST /auth/2fa/setup`

Generate a 2FA secret and QR code URL for setup.

**Headers** — `X-User-Id` required

**Response** — `ApiResponse<TwoFactorSetupResponse>`

```json
{
  "success": true,
  "data": {
    "secret": "JBSWY3DPEHPK3PXP",
    "qrCodeUrl": "otpauth://totp/CareMatch360:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=CareMatch360",
    "backupCodes": [
      "12345678",
      "23456789",
      "34567890",
      "45678901",
      "56789012",
      "67890123",
      "78901234",
      "89012345"
    ]
  },
  "message": "2FA setup initialized",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `POST /auth/2fa/enable`

Enable 2FA after scanning the QR code and verifying the first TOTP code.

**Headers** — `X-User-Id` required

**Request Body**

| Field | Type | Required |
|---|---|---|
| `code` | `string` | Yes |

```json
{
  "code": "123456"
}
```

**Response** — `ApiResponse<null>`

---

#### `POST /auth/2fa/disable`

Disable 2FA by verifying the current code.

**Headers** — `X-User-Id` required

**Request Body**

| Field | Type | Required |
|---|---|---|
| `code` | `string` | Yes |

```json
{
  "code": "123456"
}
```

**Response** — `ApiResponse<null>`

---

#### `GET /auth/2fa/status`

Check whether 2FA is enabled for the current user.

**Headers** — `X-User-Id` required

**Response** — `ApiResponse<Boolean>`

```json
{
  "success": true,
  "data": true,
  "message": "OK",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

## 6. Care Profile Service — Port 8002

Base path: `/api/v1`

---

### 6.1 Patient Profile Endpoints

#### `POST /patients`

Create a new patient profile for the authenticated user.

**Headers** — `X-User-Id` required

**Request Body**

| Field | Type | Required | Validation |
|---|---|---|---|
| `age` | `integer` | Yes | 0–150 |
| `gender` | `string` | Yes | |
| `region` | `string` | Yes | |
| `latitude` | `number` | Yes | |
| `longitude` | `number` | Yes | |
| `careLevel` | `integer` | Yes | 1–5 |
| `careType` | `string[]` | Yes | At least one value |
| `lifestyleAttributes` | `object` | No | Free-form JSON |
| `medicalRequirements` | `object` | No | Free-form JSON |
| `dataVisibility` | `object<string, boolean>` | No | Field visibility flags |
| `consentGiven` | `boolean` | Yes | |

```json
{
  "age": 72,
  "gender": "female",
  "region": "Bavaria",
  "latitude": 48.1351,
  "longitude": 11.5820,
  "careLevel": 3,
  "careType": ["RESIDENTIAL", "DEMENTIA_CARE"],
  "lifestyleAttributes": {
    "petsAllowed": true,
    "smokingAllowed": false,
    "dietaryRequirements": "vegetarian"
  },
  "medicalRequirements": {
    "mobilityAid": "wheelchair",
    "medicationSupport": true,
    "conditions": ["dementia", "diabetes"]
  },
  "dataVisibility": {
    "age": true,
    "medicalRequirements": false,
    "region": true
  },
  "consentGiven": true
}
```

**Response** — `ApiResponse<PatientProfileResponse>`

```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "age": 72,
    "gender": "female",
    "region": "Bavaria",
    "latitude": 48.1351,
    "longitude": 11.5820,
    "careLevel": 3,
    "careType": ["RESIDENTIAL", "DEMENTIA_CARE"],
    "lifestyleAttributes": {
      "petsAllowed": true,
      "smokingAllowed": false,
      "dietaryRequirements": "vegetarian"
    },
    "medicalRequirements": {
      "mobilityAid": "wheelchair",
      "medicationSupport": true,
      "conditions": ["dementia", "diabetes"]
    },
    "dataVisibility": {
      "age": true,
      "medicalRequirements": false,
      "region": true
    },
    "consentGiven": true,
    "createdAt": "2026-02-22T10:00:00",
    "updatedAt": "2026-02-22T10:00:00"
  },
  "message": "Patient profile created",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `PUT /patients`

Update the patient profile of the authenticated user. All fields are optional.

**Headers** — `X-User-Id` required

**Request Body** _(all fields optional)_

| Field | Type |
|---|---|
| `age` | `integer` |
| `gender` | `string` |
| `region` | `string` |
| `latitude` | `number` |
| `longitude` | `number` |
| `careLevel` | `integer` (1–5) |
| `careType` | `string[]` |
| `lifestyleAttributes` | `object` |
| `medicalRequirements` | `object` |
| `dataVisibility` | `object<string, boolean>` |
| `consentGiven` | `boolean` |

```json
{
  "careLevel": 4,
  "careType": ["RESIDENTIAL"]
}
```

**Response** — `ApiResponse<PatientProfileResponse>` _(same shape as create)_

---

#### `GET /patients/me`

Retrieve the patient profile of the authenticated user.

**Headers** — `X-User-Id` required

**Response** — `ApiResponse<PatientProfileResponse>` _(same shape as create)_

---

#### `GET /patients/{profileId}`

Retrieve a patient profile by its ID.

**Path Parameters**

| Parameter | Type |
|---|---|
| `profileId` | `UUID` |

**Response** — `ApiResponse<PatientProfileResponse>` _(same shape as create)_

---

#### `DELETE /patients`

Delete the patient profile of the authenticated user.

**Headers** — `X-User-Id` required

**Response** — `ApiResponse<null>`

---

#### `POST /patients/documents`

Upload a document to the patient profile. Uses `multipart/form-data`.

**Headers** — `X-User-Id` required
**Content-Type** — `multipart/form-data`

**Form Fields**

| Field | Type | Required |
|---|---|---|
| `file` | `file` | Yes |
| `documentType` | `string` | Yes |

**Response** — `ApiResponse<DocumentResponse>`

```json
{
  "success": true,
  "data": {
    "id": "doc-uuid-001",
    "profileId": "550e8400-e29b-41d4-a716-446655440001",
    "profileType": "PATIENT",
    "documentType": "MEDICAL_REPORT",
    "fileName": "medical-report-2026.pdf",
    "fileUrl": "https://s3.amazonaws.com/carematch/documents/...",
    "presignedUrl": "https://s3.amazonaws.com/carematch/documents/...?X-Amz-Signature=...",
    "fileSize": 204800,
    "mimeType": "application/pdf",
    "uploadedAt": "2026-02-22T10:00:00"
  },
  "message": "Document uploaded",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `GET /patients/documents`

Get all documents for the authenticated patient profile.

**Headers** — `X-User-Id` required

**Response** — `ApiResponse<List<DocumentResponse>>`

---

#### `DELETE /patients/documents/{documentId}`

Delete a document from the patient profile.

**Path Parameters**

| Parameter | Type |
|---|---|
| `documentId` | `UUID` |

**Response** — `ApiResponse<null>`

---

### 6.2 Provider Profile Endpoints

#### `POST /providers`

Create a new provider profile for the authenticated user.

**Headers** — `X-User-Id` required

**Request Body**

| Field | Type | Required | Validation |
|---|---|---|---|
| `facilityName` | `string` | Yes | |
| `providerType` | `string` (enum) | Yes | See [ProviderType](#providertype) |
| `latitude` | `number` | Yes | |
| `longitude` | `number` | Yes | |
| `address` | `string` | Yes | |
| `capacity` | `integer` | No | |
| `availableRooms` | `integer` | No | |
| `roomTypes` | `object<string, integer>` | No | Room type → count map |
| `serviceRadius` | `integer` | No | Kilometres (ambulatory providers) |
| `maxDailyPatients` | `integer` | No | Ambulatory providers |
| `specializations` | `string[]` | Yes | At least one value |
| `staffCount` | `integer` | No | |
| `staffToPatientRatio` | `number` | No | |
| `availability` | `object` | No | Free-form schedule JSON |
| `qualityIndicators` | `object` | No | Free-form quality metrics JSON |

```json
{
  "facilityName": "Sonnenschein Pflegeheim",
  "providerType": "RESIDENTIAL",
  "latitude": 48.1351,
  "longitude": 11.5820,
  "address": "Musterstraße 1, 80331 Munich",
  "capacity": 80,
  "availableRooms": 5,
  "roomTypes": {
    "SINGLE": 3,
    "DOUBLE": 2
  },
  "specializations": ["DEMENTIA_CARE", "PALLIATIVE_CARE"],
  "staffCount": 40,
  "staffToPatientRatio": 0.5,
  "availability": {
    "monday": "08:00-20:00",
    "tuesday": "08:00-20:00"
  },
  "qualityIndicators": {
    "averageRating": 4.5,
    "certifications": ["ISO_9001", "TÜV"]
  }
}
```

**Response** — `ApiResponse<ProviderProfileResponse>`

```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "facilityName": "Sonnenschein Pflegeheim",
    "providerType": "RESIDENTIAL",
    "latitude": 48.1351,
    "longitude": 11.5820,
    "address": "Musterstraße 1, 80331 Munich",
    "capacity": 80,
    "availableRooms": 5,
    "roomTypes": { "SINGLE": 3, "DOUBLE": 2 },
    "serviceRadius": null,
    "maxDailyPatients": null,
    "specializations": ["DEMENTIA_CARE", "PALLIATIVE_CARE"],
    "staffCount": 40,
    "staffToPatientRatio": 0.5,
    "availability": { "monday": "08:00-20:00" },
    "qualityIndicators": { "averageRating": 4.5 },
    "introVideoUrl": null,
    "images": [],
    "isVisible": true,
    "createdAt": "2026-02-22T10:00:00",
    "updatedAt": "2026-02-22T10:00:00"
  },
  "message": "Provider profile created",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `PUT /providers`

Update the provider profile of the authenticated user. All fields are optional.

**Headers** — `X-User-Id` required

**Request Body** _(all fields optional)_

| Field | Type |
|---|---|
| `facilityName` | `string` |
| `providerType` | `string` (enum) |
| `latitude` | `number` |
| `longitude` | `number` |
| `address` | `string` |
| `capacity` | `integer` |
| `availableRooms` | `integer` |
| `roomTypes` | `object<string, integer>` |
| `serviceRadius` | `integer` |
| `maxDailyPatients` | `integer` |
| `specializations` | `string[]` |
| `staffCount` | `integer` |
| `staffToPatientRatio` | `number` |
| `availability` | `object` |
| `qualityIndicators` | `object` |
| `isVisible` | `boolean` |

**Response** — `ApiResponse<ProviderProfileResponse>` _(same shape as create)_

---

#### `GET /providers/me`

Get the provider profile of the authenticated user.

**Headers** — `X-User-Id` required

**Response** — `ApiResponse<ProviderProfileResponse>` _(same shape as create)_

---

#### `GET /providers/{profileId}`

Get a provider profile by ID.

**Path Parameters**

| Parameter | Type |
|---|---|
| `profileId` | `UUID` |

**Response** — `ApiResponse<ProviderProfileResponse>` _(same shape as create)_

---

#### `POST /providers/search`

Search for providers matching specified criteria.

**Headers** — `X-User-Id` required

**Request Body**

| Field | Type | Required | Default | Notes |
|---|---|---|---|---|
| `providerType` | `string` (enum) | No | | Filter by provider type |
| `region` | `string` | No | | Filter by region name |
| `latitude` | `number` | No | | Center point for radius search |
| `longitude` | `number` | No | | Center point for radius search |
| `radiusKm` | `integer` | No | | Max distance in kilometres |
| `careLevel` | `integer` | No | | Required care level (1–5) |
| `specializations` | `string[]` | No | | Filter by specializations |
| `availabilityDate` | `string` (date) | No | | Format: `yyyy-MM-dd` |
| `roomType` | `string` | No | | Filter by room type |
| `minCapacity` | `integer` | No | | Minimum total capacity |
| `page` | `integer` | No | `0` | |
| `size` | `integer` | No | `20` | |
| `sortBy` | `string` | No | | Field to sort by |
| `sortDirection` | `string` | No | `"asc"` | `"asc"` or `"desc"` |

```json
{
  "providerType": "RESIDENTIAL",
  "latitude": 48.1351,
  "longitude": 11.5820,
  "radiusKm": 25,
  "careLevel": 3,
  "specializations": ["DEMENTIA_CARE"],
  "page": 0,
  "size": 10,
  "sortBy": "distance",
  "sortDirection": "asc"
}
```

**Response** — `ApiResponse<ProviderSearchResponse>`

```json
{
  "success": true,
  "data": {
    "providers": [ ],
    "totalResults": 42,
    "page": 0,
    "size": 10,
    "totalPages": 5
  },
  "message": "OK",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `DELETE /providers`

Delete the provider profile of the authenticated user.

**Headers** — `X-User-Id` required

**Response** — `ApiResponse<null>`

---

#### `POST /providers/documents`

Upload a document to the provider profile.

**Headers** — `X-User-Id` required
**Content-Type** — `multipart/form-data`

**Form Fields**

| Field | Type | Required |
|---|---|---|
| `file` | `file` | Yes |
| `documentType` | `string` | Yes |

**Response** — `ApiResponse<DocumentResponse>` _(same shape as patient document)_

---

#### `GET /providers/documents`

Get all documents for the authenticated provider profile.

**Headers** — `X-User-Id` required

**Response** — `ApiResponse<List<DocumentResponse>>`

---

#### `DELETE /providers/documents/{documentId}`

Delete a document from the provider profile.

**Path Parameters**

| Parameter | Type |
|---|---|
| `documentId` | `UUID` |

**Response** — `ApiResponse<null>`

---

## 7. Care Match Service — Port 8003

Base path: `/api/v1`

---

### 7.1 Match Endpoints

#### `POST /matches/calculate`

Trigger the match score calculation between a specific patient and provider.

**Query Parameters**

| Parameter | Type | Required |
|---|---|---|
| `patientId` | `UUID` | Yes |
| `providerId` | `UUID` | Yes |

**Response** — `ApiResponse<MatchScoreResponse>`

```json
{
  "success": true,
  "data": {
    "id": "match-uuid-001",
    "patientId": "550e8400-e29b-41d4-a716-446655440001",
    "providerId": "550e8400-e29b-41d4-a716-446655440002",
    "score": 87.50,
    "explanation": {
      "summary": "Strong match based on care level and specialization alignment",
      "strengths": ["careLevel", "specializations"],
      "weaknesses": ["distance"]
    },
    "scoreBreakdown": {
      "careLevel": 28.5,
      "distance": 14.0,
      "specialization": 19.0,
      "lifestyle": 18.0,
      "social": 8.0
    },
    "calculatedAt": "2026-02-22T10:00:00",
    "providerName": "Sonnenschein Pflegeheim",
    "providerType": "RESIDENTIAL",
    "providerAddress": "Musterstraße 1, 80331 Munich"
  },
  "message": "Match calculated",
  "timestamp": "2026-02-22T10:00:00"
}
```

> **Scoring weights:** Care Level 30% · Distance 20% · Specialization 20% · Lifestyle 20% · Social 10%
> **Matching threshold:** 70 (scores below this are not recommended)

---

#### `GET /matches/patient/{patientId}`

Get all match scores for a patient, paginated.

**Path Parameters**

| Parameter | Type |
|---|---|
| `patientId` | `UUID` |

**Query Parameters**

| Parameter | Type | Default |
|---|---|---|
| `page` | `integer` | `0` |
| `size` | `integer` | `20` |

**Response** — `ApiResponse<PageResponse<MatchScoreResponse>>`

---

#### `GET /matches/provider/{providerId}`

Get all match scores for a provider, paginated.

**Path Parameters**

| Parameter | Type |
|---|---|
| `providerId` | `UUID` |

**Query Parameters**

| Parameter | Type | Default |
|---|---|---|
| `page` | `integer` | `0` |
| `size` | `integer` | `20` |

**Response** — `ApiResponse<PageResponse<MatchScoreResponse>>`

---

#### `GET /matches/patient/{patientId}/top`

Get the top-N highest-scoring providers for a patient.

**Path Parameters**

| Parameter | Type |
|---|---|
| `patientId` | `UUID` |

**Query Parameters**

| Parameter | Type | Default |
|---|---|---|
| `limit` | `integer` | `10` |

**Response** — `ApiResponse<List<MatchScoreResponse>>`

---

#### `GET /matches/patient/{patientId}/provider/{providerId}`

Get the specific match score between a patient and a provider.

**Path Parameters**

| Parameter | Type |
|---|---|
| `patientId` | `UUID` |
| `providerId` | `UUID` |

**Response** — `ApiResponse<MatchScoreResponse>`

---

#### `POST /matches/recalculate/patient/{patientId}`

Trigger recalculation of all match scores for a patient against all providers.

**Path Parameters**

| Parameter | Type |
|---|---|
| `patientId` | `UUID` |

**Response** — `ApiResponse<null>`

---

#### `POST /matches/recalculate/provider/{providerId}`

Trigger recalculation of all match scores for a provider against all patients.

**Path Parameters**

| Parameter | Type |
|---|---|
| `providerId` | `UUID` |

**Response** — `ApiResponse<null>`

---

### 7.2 Offer Endpoints

#### `POST /offers`

Create a new care offer (starts in `DRAFT` status).

**Headers** — `X-User-Id` required

**Request Body**

| Field | Type | Required | Notes |
|---|---|---|---|
| `patientId` | `UUID` | Yes | The target patient |
| `message` | `string` | Yes | Offer message text |
| `availabilityDetails` | `object` | No | Free-form availability information |

```json
{
  "patientId": "550e8400-e29b-41d4-a716-446655440001",
  "message": "We would love to welcome you to our facility. We have a private room available.",
  "availabilityDetails": {
    "availableFrom": "2026-03-01",
    "roomType": "SINGLE",
    "visitingHours": "10:00-18:00"
  }
}
```

**Response** — `ApiResponse<OfferResponse>`

```json
{
  "success": true,
  "data": {
    "id": "offer-uuid-001",
    "patientId": "550e8400-e29b-41d4-a716-446655440001",
    "providerId": "550e8400-e29b-41d4-a716-446655440002",
    "matchId": "match-uuid-001",
    "status": "DRAFT",
    "message": "We would love to welcome you to our facility.",
    "availabilityDetails": {
      "availableFrom": "2026-03-01",
      "roomType": "SINGLE"
    },
    "createdAt": "2026-02-22T10:00:00",
    "updatedAt": "2026-02-22T10:00:00",
    "expiresAt": "2026-03-22T10:00:00",
    "providerName": "Sonnenschein Pflegeheim",
    "patientName": "Maria Müller",
    "matchScore": 87.50
  },
  "message": "Offer created",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `PUT /offers/{offerId}/send`

Send a draft offer to the patient (changes status from `DRAFT` to `SENT`).

**Headers** — `X-User-Id` required

**Path Parameters**

| Parameter | Type |
|---|---|
| `offerId` | `UUID` |

**Response** — `ApiResponse<OfferResponse>`

---

#### `PUT /offers/{offerId}/accept`

Accept an offer (patient action — changes status to `ACCEPTED`).

**Headers** — `X-User-Id` required

**Path Parameters**

| Parameter | Type |
|---|---|
| `offerId` | `UUID` |

**Response** — `ApiResponse<OfferResponse>`

---

#### `PUT /offers/{offerId}/reject`

Reject an offer (patient action — changes status to `REJECTED`).

**Headers** — `X-User-Id` required

**Path Parameters**

| Parameter | Type |
|---|---|
| `offerId` | `UUID` |

**Response** — `ApiResponse<OfferResponse>`

---

#### `GET /offers/{offerId}`

Get an offer by its ID.

**Path Parameters**

| Parameter | Type |
|---|---|
| `offerId` | `UUID` |

**Response** — `ApiResponse<OfferResponse>`

---

#### `GET /offers/patient/{patientId}`

Get all offers received by a patient, paginated.

**Path Parameters**

| Parameter | Type |
|---|---|
| `patientId` | `UUID` |

**Query Parameters**

| Parameter | Type | Default |
|---|---|---|
| `page` | `integer` | `0` |
| `size` | `integer` | `20` |

**Response** — `ApiResponse<PageResponse<OfferResponse>>`

---

#### `GET /offers/provider/{providerId}`

Get all offers sent by a provider, paginated.

**Path Parameters**

| Parameter | Type |
|---|---|
| `providerId` | `UUID` |

**Query Parameters**

| Parameter | Type | Default |
|---|---|---|
| `page` | `integer` | `0` |
| `size` | `integer` | `20` |

**Response** — `ApiResponse<PageResponse<OfferResponse>>`

---

#### `GET /offers/{offerId}/history`

Get the full status change history of an offer.

**Path Parameters**

| Parameter | Type |
|---|---|
| `offerId` | `UUID` |

**Response** — `ApiResponse<List<OfferHistoryResponse>>`

```json
{
  "success": true,
  "data": [
    {
      "id": "history-uuid-001",
      "offerId": "offer-uuid-001",
      "oldStatus": null,
      "newStatus": "DRAFT",
      "changedBy": "550e8400-e29b-41d4-a716-446655440002",
      "changedAt": "2026-02-22T10:00:00",
      "notes": "Offer created"
    },
    {
      "id": "history-uuid-002",
      "offerId": "offer-uuid-001",
      "oldStatus": "DRAFT",
      "newStatus": "SENT",
      "changedBy": "550e8400-e29b-41d4-a716-446655440002",
      "changedAt": "2026-02-22T11:00:00",
      "notes": null
    }
  ],
  "message": "OK",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

## 8. Care Billing Service — Port 8004

Base path: `/api/v1`

---

### 8.1 Subscription Endpoints

#### `POST /subscriptions`

Create a new subscription for a provider (starts a 14-day trial period).

**Request Body**

| Field | Type | Required | Notes |
|---|---|---|---|
| `providerId` | `UUID` | Yes | |
| `tier` | `string` (enum) | Yes | See [SubscriptionTier](#subscriptiontier) |
| `paymentMethodId` | `string` | No | Stripe payment method ID |

```json
{
  "providerId": "550e8400-e29b-41d4-a716-446655440002",
  "tier": "PRO",
  "paymentMethodId": "pm_1234567890"
}
```

**Response** — `ApiResponse<SubscriptionResponse>`

```json
{
  "success": true,
  "data": {
    "id": "sub-uuid-001",
    "providerId": "550e8400-e29b-41d4-a716-446655440002",
    "tier": "PRO",
    "status": "TRIALING",
    "currentPeriodStart": "2026-02-22T10:00:00",
    "currentPeriodEnd": "2026-03-22T10:00:00",
    "trialEnd": "2026-03-08T10:00:00",
    "cancelledAt": null,
    "createdAt": "2026-02-22T10:00:00",
    "updatedAt": "2026-02-22T10:00:00",
    "planInfo": {
      "name": "Pro",
      "price": 99.99,
      "currency": "EUR",
      "interval": "month",
      "features": [
        "Enhanced profile visibility",
        "Unlimited offers",
        "Priority email support",
        "Analytics dashboard"
      ]
    }
  },
  "message": "Subscription created",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `PUT /subscriptions/{subscriptionId}`

Upgrade or downgrade an existing subscription.

**Path Parameters**

| Parameter | Type |
|---|---|
| `subscriptionId` | `UUID` |

**Request Body**

| Field | Type | Required |
|---|---|---|
| `newTier` | `string` (enum) | Yes |

```json
{
  "newTier": "PREMIUM"
}
```

**Response** — `ApiResponse<SubscriptionResponse>` _(same shape as create)_

---

#### `DELETE /subscriptions/{subscriptionId}`

Cancel a subscription. The subscription remains active until the end of the current billing period.

**Path Parameters**

| Parameter | Type |
|---|---|
| `subscriptionId` | `UUID` |

**Response** — `ApiResponse<SubscriptionResponse>` _(status changes to `CANCELLED`)_

---

#### `GET /subscriptions/{subscriptionId}`

Get a subscription by its ID.

**Path Parameters**

| Parameter | Type |
|---|---|
| `subscriptionId` | `UUID` |

**Response** — `ApiResponse<SubscriptionResponse>`

---

#### `GET /subscriptions/provider/{providerId}`

Get the active subscription for a provider.

**Path Parameters**

| Parameter | Type |
|---|---|
| `providerId` | `UUID` |

**Response** — `ApiResponse<SubscriptionResponse>`

---

### 8.2 Invoice Endpoints

#### `GET /invoices/{invoiceId}`

Get an invoice by its ID.

**Path Parameters**

| Parameter | Type |
|---|---|
| `invoiceId` | `UUID` |

**Response** — `ApiResponse<InvoiceResponse>`

```json
{
  "success": true,
  "data": {
    "id": "inv-uuid-001",
    "subscriptionId": "sub-uuid-001",
    "invoiceNumber": "INV-2026-0001",
    "amount": 99.99,
    "currency": "EUR",
    "status": "PAID",
    "pdfUrl": "https://s3.amazonaws.com/carematch/invoices/INV-2026-0001.pdf",
    "issuedAt": "2026-02-22T10:00:00",
    "dueAt": "2026-03-08T10:00:00",
    "paidAt": "2026-02-22T10:05:00",
    "createdAt": "2026-02-22T10:00:00"
  },
  "message": "OK",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `GET /invoices/number/{invoiceNumber}`

Get an invoice by its human-readable invoice number.

**Path Parameters**

| Parameter | Type | Example |
|---|---|---|
| `invoiceNumber` | `string` | `INV-2026-0001` |

**Response** — `ApiResponse<InvoiceResponse>` _(same shape as above)_

---

#### `GET /invoices/subscription/{subscriptionId}`

Get all invoices for a subscription, paginated.

**Path Parameters**

| Parameter | Type |
|---|---|
| `subscriptionId` | `UUID` |

**Query Parameters**

| Parameter | Type | Default |
|---|---|---|
| `page` | `integer` | `0` |
| `size` | `integer` | `20` |

**Response** — `ApiResponse<PageResponse<InvoiceResponse>>`

---

#### `GET /invoices/{invoiceId}/pdf`

Download the PDF version of an invoice.

**Path Parameters**

| Parameter | Type |
|---|---|
| `invoiceId` | `UUID` |

**Response** — Binary PDF file stream
**Content-Type:** `application/pdf`

---

### 8.3 Webhook Endpoints

#### `POST /webhooks/stripe`

Receives and processes Stripe webhook events. Used internally by Stripe — the frontend does not call this directly.

**Headers**

| Header | Required |
|---|---|
| `Stripe-Signature` | Yes |

**Response** — `200 OK` (no body)

---

## 9. Care Notification Service — Port 8005

Base path: `/api/v1`

---

### 9.1 Notification Endpoints

#### `POST /notifications`

Send a notification to a user.

**Request Body**

| Field | Type | Required | Notes |
|---|---|---|---|
| `userId` | `UUID` | Yes | |
| `type` | `string` (enum) | Yes | See [NotificationType](#notificationtype) |
| `channel` | `string` | Yes | e.g. `"email"`, `"in_app"` |
| `subject` | `string` | No | Used for email notifications |
| `body` | `string` | Yes | Notification message body |
| `templateData` | `object` | No | Variables for template rendering |

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "IN_APP",
  "channel": "in_app",
  "subject": "New offer received",
  "body": "A care provider has sent you a new offer.",
  "templateData": {
    "providerName": "Sonnenschein Pflegeheim",
    "offerId": "offer-uuid-001"
  }
}
```

**Response** — `ApiResponse<NotificationResponse>`

```json
{
  "success": true,
  "data": {
    "id": "notif-uuid-001",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "IN_APP",
    "channel": "in_app",
    "subject": "New offer received",
    "body": "A care provider has sent you a new offer.",
    "status": "SENT",
    "sentAt": "2026-02-22T10:00:00",
    "readAt": null,
    "createdAt": "2026-02-22T10:00:00",
    "isRead": false
  },
  "message": "Notification sent",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `GET /notifications/user/{userId}`

Get all notifications for a user, paginated.

**Path Parameters**

| Parameter | Type |
|---|---|
| `userId` | `UUID` |

**Query Parameters**

| Parameter | Type | Default |
|---|---|---|
| `page` | `integer` | `0` |
| `size` | `integer` | `20` |

**Response** — `ApiResponse<PageResponse<NotificationResponse>>`

---

#### `GET /notifications/user/{userId}/unread`

Get all unread notifications for a user.

**Path Parameters**

| Parameter | Type |
|---|---|
| `userId` | `UUID` |

**Response** — `ApiResponse<List<NotificationResponse>>`

---

#### `GET /notifications/user/{userId}/unread/count`

Get the count of unread notifications for a user.

**Path Parameters**

| Parameter | Type |
|---|---|
| `userId` | `UUID` |

**Response** — `ApiResponse<Long>`

```json
{
  "success": true,
  "data": 5,
  "message": "OK",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `PUT /notifications/{notificationId}/read`

Mark a specific notification as read.

**Path Parameters**

| Parameter | Type |
|---|---|
| `notificationId` | `UUID` |

**Response** — `ApiResponse<NotificationResponse>`

---

#### `PUT /notifications/user/{userId}/read-all`

Mark all notifications for a user as read.

**Path Parameters**

| Parameter | Type |
|---|---|
| `userId` | `UUID` |

**Response** — `ApiResponse<null>`

---

### 9.2 Analytics Endpoints

#### `GET /analytics/events/user/{userId}`

Get all event logs for a specific user, paginated.

**Path Parameters**

| Parameter | Type |
|---|---|
| `userId` | `UUID` |

**Query Parameters**

| Parameter | Type | Default |
|---|---|---|
| `page` | `integer` | `0` |
| `size` | `integer` | `20` |

**Response** — `ApiResponse<PageResponse<EventLogResponse>>`

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "event-uuid-001",
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "eventType": "offer.sent",
        "eventData": {
          "offerId": "offer-uuid-001",
          "patientId": "550e8400-e29b-41d4-a716-446655440001"
        },
        "timestamp": "2026-02-22T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "message": "OK",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `GET /analytics/events/type/{eventType}`

Get all event logs of a specific type.

**Path Parameters**

| Parameter | Type | Example |
|---|---|---|
| `eventType` | `string` | `offer.sent` |

**Response** — `ApiResponse<List<EventLogResponse>>`

---

#### `GET /analytics/events/time-range`

Get event logs within a time range.

**Query Parameters**

| Parameter | Type | Required | Format |
|---|---|---|---|
| `start` | `string` | Yes | ISO datetime: `2026-02-01T00:00:00` |
| `end` | `string` | Yes | ISO datetime: `2026-02-28T23:59:59` |

**Response** — `ApiResponse<List<EventLogResponse>>`

---

#### `GET /analytics/events/counts`

Get a count of events grouped by event type within a time range.

**Query Parameters**

| Parameter | Type | Required | Format |
|---|---|---|---|
| `start` | `string` | Yes | ISO datetime |
| `end` | `string` | Yes | ISO datetime |

**Response** — `ApiResponse<Map<String, Long>>`

```json
{
  "success": true,
  "data": {
    "offer.sent": 42,
    "offer.accepted": 18,
    "profile.created": 130,
    "match.calculated": 310
  },
  "message": "OK",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `GET /analytics/metrics/{metricName}`

Get all usage metrics for a given metric name.

**Path Parameters**

| Parameter | Type | Example |
|---|---|---|
| `metricName` | `string` | `active_users_daily` |

**Response** — `ApiResponse<List<UsageMetricResponse>>`

```json
{
  "success": true,
  "data": [
    {
      "id": "metric-uuid-001",
      "metricName": "active_users_daily",
      "metricValue": 256.00,
      "aggregationPeriod": "DAILY",
      "periodStart": "2026-02-22T00:00:00",
      "periodEnd": "2026-02-22T23:59:59",
      "recordedAt": "2026-02-22T23:59:59"
    }
  ],
  "message": "OK",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

#### `GET /analytics/metrics/{metricName}/recent`

Get recent usage metrics for a given name, within the last N days.

**Path Parameters**

| Parameter | Type |
|---|---|
| `metricName` | `string` |

**Query Parameters**

| Parameter | Type | Default |
|---|---|---|
| `days` | `integer` | `30` |

**Response** — `ApiResponse<List<UsageMetricResponse>>`

---

#### `GET /analytics/report`

Generate a full analytics report.

**Response** — `ApiResponse<AnalyticsReportResponse>`

```json
{
  "success": true,
  "data": {
    "eventCounts": {
      "profile.created": 500,
      "match.calculated": 1200,
      "offer.sent": 300,
      "offer.accepted": 120
    },
    "usageStatistics": {
      "avgMatchScore": 78.3,
      "topRegions": ["Bavaria", "Baden-Württemberg"]
    },
    "totalUsers": 2500,
    "activeUsers": 1800,
    "totalMatches": 12000,
    "totalOffers": 1500,
    "totalSubscriptions": 320
  },
  "message": "OK",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

## 10. Enumerations Reference

### UserRole

| Value | Description |
|---|---|
| `PATIENT` | Care-seeking patient or their representative |
| `RELATIVE` | Relative managing care on behalf of a patient |
| `RESIDENTIAL_PROVIDER` | Provider offering residential care (nursing home, etc.) |
| `AMBULATORY_PROVIDER` | Provider offering ambulatory / home-visit care |
| `ADMIN` | Platform administrator |
| `SUPER_ADMIN` | Super administrator with full access |

### ProviderType

| Value | Description |
|---|---|
| `RESIDENTIAL` | Stationary / in-facility care |
| `AMBULATORY` | Mobile / home-visit care |

### OfferStatus

| Value | Description |
|---|---|
| `DRAFT` | Offer created but not yet sent |
| `SENT` | Offer sent to the patient |
| `VIEWED` | Patient has opened the offer |
| `ACCEPTED` | Patient accepted the offer |
| `REJECTED` | Patient rejected the offer |
| `EXPIRED` | Offer exceeded its expiry date without response |

### SubscriptionTier

| Value | Monthly Price | Description |
|---|---|---|
| `BASIC` | EUR 49.99 | Entry-level provider plan |
| `PRO` | EUR 99.99 | Professional provider plan |
| `PREMIUM` | EUR 199.99 | Full-featured provider plan |

### SubscriptionStatus

| Value | Description |
|---|---|
| `ACTIVE` | Subscription is active and paid |
| `TRIALING` | Within the 14-day free trial period |
| `PAUSED` | Subscription is paused |
| `CANCELLED` | Subscription has been cancelled |
| `PAST_DUE` | Payment failed; within the 3-day grace period |

### InvoiceStatus

| Value | Description |
|---|---|
| `PENDING` | Awaiting payment |
| `PAID` | Payment successful |
| `FAILED` | Payment failed |
| `VOID` | Invoice voided |

### NotificationType

| Value | Description |
|---|---|
| `EMAIL` | Email notification |
| `IN_APP` | In-application notification |
| `PUSH` | Push notification |

### AggregationPeriod

| Value | Description |
|---|---|
| `HOURLY` | Per-hour metric |
| `DAILY` | Per-day metric |
| `WEEKLY` | Per-week metric |
| `MONTHLY` | Per-month metric |

---

## 11. Subscription Plans Reference

| Plan | Tier | Price | Offers/Month | Support | Features |
|---|---|---|---|---|---|
| Basic | `BASIC` | EUR 49.99/mo | Up to 10 | Email | Basic profile visibility |
| Pro | `PRO` | EUR 99.99/mo | Unlimited | Priority email | Enhanced visibility, analytics dashboard |
| Premium | `PREMIUM` | EUR 199.99/mo | Unlimited | 24/7 phone | Maximum visibility, advanced analytics, dedicated account manager |

> **Trial period:** 14 days free for all new subscriptions.
> **Grace period:** 3 days after a failed payment before suspension.