# Workshop Profile API — Implementation Reference

Backend implementation of the **Workshop Profile** feature (the 15-item "My Profile" menu
plus the profile aggregate) described in `WORKSHOP_PROFILE_SCREEN.md`. This document
reflects the actual Spring Boot implementation, not the original frontend-mock spec —
see [§8 Deviations](#8-deviations-from-the-original-frontend-spec) for where the two differ.

A "workshop" is a `User` with `role = MECHANIC`. Every endpoint below is scoped to the
authenticated workshop via the JWT — there is no `workshopId` in the path for self-service
endpoints.

---

## 1. Where it lives in the code

| Concern | File(s) |
|---|---|
| Core profile logic (aggregate, owner, info, location, images, referral, onboarding, verification, operational trust) | `service/WorkshopProfileService.java` |
| Operations (pickup/drop, payment, notifications) | `service/WorkshopOperationsService.java` |
| Support & legal | `service/WorkshopSupportLegalService.java` |
| Self-service endpoints (sections 0–13) | `controller/WorkshopProfileController.java` |
| Support & legal endpoints (sections 14–15) | `controller/WorkshopSupportLegalController.java` |
| Account: change-password / delete-account | `controller/UserController.java`, `service/AuthService.java` |
| Admin: onboarding decision | `controller/AdminController.java` |
| File upload/download | `service/FileStorageService.java`, `controller/FileController.java`, `model/StoredFile.java` |
| Business-rule error codes | `exception/BusinessRuleException.java`, wired in `exception/GlobalExceptionHandler.java` |
| Entities | `model/Workshop*.java`, `model/StoredFile.java`, `model/LegalDocument.java`, `model/ReferralPartnerCode.java` |
| Schema | `db/changelog/changes/023-*.yaml` through `036-*.yaml` |

---

## 2. Response envelope & error codes

Every endpoint returns the app-wide envelope (`MyApiResponse<T>`), **not** the
`{success, data, message, errors}` shape from the original doc:

```jsonc
{
  "success": true,
  "message": "Owner information saved",
  "data": { /* payload */ },
  "timestamp": "2026-07-07T19:56:17.464",
  "errorCode": null
}
```

On failure, `success: false` and `errorCode` (added in this feature) carries a
machine-readable code the client can switch on:

```jsonc
{ "success": false, "message": "This referral code has expired", "errorCode": "REFERRAL_EXPIRED", "data": null }
```

| Code | HTTP | Thrown by |
|---|---|---|
| `REFERRAL_INVALID` / `REFERRAL_EXPIRED` / `REFERRAL_INACTIVE` | 400 | `POST /referral/validate`, `PUT /referral` |
| `ONBOARDING_INCOMPLETE` | 400 | `POST /onboarding/submit` — lists which sections are missing |
| `FEE_UNPAID` | 400 | `POST /onboarding/submit` |
| `VERIFICATION_INSUFFICIENT_METHODS` | 400 | `POST /verification/submit` — fewer than 2 methods |
| `CHANNELS_REQUIRED` | 400 | `PUT /notifications/preferences` — empty `channels` |
| `IMAGE_TOO_LARGE` / `IMAGE_BAD_FORMAT` | 400 | any file upload (images, ID proof, support attachment) |
| `OTP_INVALID` / `OTP_EXPIRED` | 400 | `POST /owner/whatsapp/verify-otp` |
| `VALIDATION_ERROR` | 400 | bean-validation failures (`data` holds a field→message map) and a few cross-field checks (state, payment action params) |

Plain `BadRequestException` (role checks, "not configured yet") and
`ResourceNotFoundException` (404) still return `errorCode: null`, same as the rest of the app.

---

## 3. Section-by-section endpoints

Base path for sections 0–13: **`/api/workshop-profile`** (role: `MECHANIC` unless noted).
Sections 14–15 share the same base path from a second controller.

### 0. Profile aggregate
- `GET /api/workshop-profile` → header, stats, `status`, and per-section `complete` flags
  (`owner`, `workshop`, `location`, `images`, `referral`, `onboarding`, `verification`).
  Backs both the hero header and Menu 1 "Current Status".

### 2. Owner Information
- `GET/PUT /owner` — `ownerName, primaryPhone, secondaryPhone, whatsappNumber, whatsappVerified, email, idProofUrl`.
  Changing `whatsappNumber` resets `whatsappVerified` to `false`.
- `POST /owner/id-proof` (multipart, field `file`) — uploads via `FileStorageService`; requires owner info to already exist.
- `POST /owner/whatsapp/send-otp` / `POST /owner/whatsapp/verify-otp` — reuses the existing
  `OtpService.generateAndSaveOtpForMobile` / `validateOtpByMobile` (the same mechanic OTP already
  used for account mobile-number changes), delivered by email via a new
  `NotificationService.sendWhatsappVerificationOtp`.

### 3. Workshop Information
- `GET/PUT /info` — single field `workshopName`, stored directly on `User.workshopName`
  (no new table — see [§8](#8-deviations-from-the-original-frontend-spec)).

### 4. Workshop Location
- `GET/PUT /location` — `addressLine1/2, city, state, pincode, latitude, longitude`, stored on
  the existing `User` address/geo columns. `pincode` validated against `^[1-9][0-9]{5}$`;
  `state` validated against a fixed `Constants.INDIAN_STATES` set.

### 5. Workshop Images
- `GET /images`, `POST /images` (multipart, fields `file` + `type` = `shopfront|interior|signage`),
  `DELETE /images/{imageId}`. Max 5 MB, JPEG/PNG/WebP only. A workshop's `images` section is
  "complete" once all 3 mandatory types exist.

### 6. Referral Code
- `POST /referral/validate` — checks against a seeded `referral_partner_codes` table
  (`SALES100`, `WORKSHOP50`, `MECH20`, `PARTNER10` valid; `EXPIRED1` expired; `INACTIVE1` inactive;
  anything else → `REFERRAL_INVALID`).
- `PUT /referral` — validates then persists to `workshop_referrals` (one per workshop).

### 7. Submit Onboarding
- `GET /onboarding` — checklist (reuses the same section-completion map as the aggregate),
  `feeAmount` (fixed ₹1000, `Constants.WORKSHOP_ONBOARDING_FEE`), `feePaid`, `referenceId`, `decision`.
- `POST /onboarding/pay` — `{ amount }` must equal the fee exactly; generates a `paymentRef`.
- `POST /onboarding/submit` — fails `ONBOARDING_INCOMPLETE` if any section besides
  `onboarding` itself is incomplete, or `FEE_UNPAID` if the fee hasn't been paid. On success,
  generates `ONB-XXXXXXXX` and moves the workshop's overall `status` to `PENDING`.
- **Admin decision** (not in the original doc — added so the workflow is actually closeable):
  `POST /api/admin/workshop-profile/{mechanicId}/onboarding/decision` (`ADMINISTRATOR` only),
  body `{ decision: APPROVED|REJECTED|MORE_INFO, reason? }`. Transitions the workshop's
  `WorkshopStatus`: `APPROVED → VERIFIED`, `REJECTED → REJECTED`, `MORE_INFO → IN_PROGRESS`.

### 8. Platform Verified
- `GET /verification` — `eligible` (true only while `status = NEW_TO_APP`), the 3 fixed
  methods (`BUSINESS_LICENSE`, `GST_CERTIFICATE`, `BANK_STATEMENT`) with `done` flags, `verified`.
- `POST /verification/submit` — `{ methods: [...] }`, requires ≥ 2
  (`Constants.PLATFORM_VERIFICATION_MIN_METHODS`), rejected outside the `NEW_TO_APP` window.

### 9. Operational Trust
- `GET /operational-trust` — read-only, derived from `WorkshopProfile`:
  `suspended → "suspended"`, else `status == VERIFIED → "verified"`, else `"under_evaluation"`.

### 10. Pickup & Drop Status
- `GET /pickup-drop-status` — read-only. 5 fixed stages
  (`REQUESTED, DRIVER_ASSIGNED, PICKED_UP, IN_SERVICE, RETURNED`); stages before the
  workshop's `currentStage` show `DONE`, the current one `ACTIVE`, later ones `PENDING`.
  `facilityEnabled` is a stored flag (no write endpoint here — intended to be flipped by the
  Settings ▸ Configuration "Pickup/Drop Facility" switch, which isn't part of this branch).

### 11. Payment Setup
- `GET /payment` — `eligibility` (`NOT_ELIGIBLE → ELIGIBLE → PENDING_APPROVAL → APPROVED`),
  3 derived steps (`eligibility_check`, `account_link`, `approval`), `partner`, `accountRef`.
- `POST /payment/action` — `{ action: "request_approval" | "link_account", partner?, accountRef? }`.
  `partner` is a free-form string the caller supplies — nothing here hard-codes a gateway.

### 12. Notifications
- `GET/PUT /notifications/preferences` — booking toggles (`newBookingEnabled`,
  `bookingCancelledEnabled`, `bookingRescheduledEnabled`), `channels` (`PUSH|SMS|EMAIL`, ≥1
  required or `CHANNELS_REQUIRED`), `sound` (`DAY|NIGHT`). Payment notifications are always
  returned as `{enabled: true, locked: true}` and are never persisted as a toggle.

### 13. Account
- `POST /api/users/change-password` and `DELETE /api/users/me` (new, on `UserController` —
  works for all 4 roles, matches the existing `change-pin` pattern: re-verifies the current
  password, invalidates all refresh tokens).
- `POST /api/workshop-profile/account/deactivate` — `{ reason? }`, sets `User.active = false`
  and records the reason on `WorkshopProfile`.
- Biometric login is **not** implemented server-side — confirmed local-only
  (device keystore) per the source doc; no endpoint needed.

### 14. Feedback & Support
- `GET/POST /support/requests` (multipart: `category`, `subject`, `description`, optional `image` ≤2MB).
  `description` word count enforced in the service (`Constants.SUPPORT_DESCRIPTION_MAX_WORDS = 200`)
  since bean validation can't express a word-count limit. Generates `SR-XXXXXXXX`.

### 15. Legal
- `GET /legal/documents` — global (not per-workshop), seeded via Liquibase; open to any
  authenticated user (`isAuthenticated()`, not `MECHANIC`-only).
- `GET/POST /legal/consents` — per-workshop consent history, validated against the seeded
  document catalogue.

---

## 4. Entities

One row per workshop unless noted:

| Entity | Table | Notes |
|---|---|---|
| `WorkshopProfile` | `workshop_profiles` | `status` (`WorkshopStatus`), `suspended`, `suspensionReason` |
| `WorkshopOwnerInfo` | `workshop_owner_infos` | FK to `StoredFile` for `idProofFile` |
| `WorkshopImage` | `workshop_images` | many per workshop; FK to `StoredFile` |
| `ReferralPartnerCode` | `referral_partner_codes` | global catalogue, seeded |
| `WorkshopReferral` | `workshop_referrals` | the code a workshop applied |
| `WorkshopOnboarding` | `workshop_onboardings` | fee/submission/decision |
| `WorkshopVerification` | `workshop_verifications` (+`workshop_verification_methods`) | `@ElementCollection<VerificationMethod>` |
| `WorkshopPickupDropStatus` | `workshop_pickup_drop_statuses` | `currentStage` drives derived per-stage state |
| `WorkshopPaymentSetup` | `workshop_payment_setups` | partner-agnostic |
| `WorkshopNotificationPreferences` | `workshop_notification_preferences` (+`workshop_notification_channels`) | `@ElementCollection<NotificationChannel>` |
| `WorkshopSupportRequest` | `workshop_support_requests` | many per workshop |
| `LegalDocument` | `legal_documents` | global, seeded |
| `WorkshopLegalConsent` | `workshop_legal_consents` | many per workshop |
| `StoredFile` | `stored_files` | generic upload metadata, shared by owner ID proof, images, support attachments |

**Not new tables** — reused directly on the existing `users` table: `workshopName` (info),
`addressLine1/2, city, state, postalCode, latitude, longitude` (location).

---

## 5. File storage

`FileStorageService` writes uploads to local disk under `app.upload.dir` (default `uploads/`,
configurable in `application.properties`), naming each file with a random UUID and recording
metadata in `stored_files`. URLs returned in API responses are always `/api/files/{id}`;
`FileController` streams the bytes back (requires authentication). Swap the disk calls for an
S3/GCS SDK if this ever needs to run across more than one app instance.

---

## 6. Validation summary

- Bean validation (`jakarta.validation`) on every request DTO — field-level messages come back
  under `errorCode: "VALIDATION_ERROR"` with a field→message map in `data`.
- Cross-field / business rules that can't be expressed as annotations live in the service layer
  and throw `BusinessRuleException` with one of the codes in [§2](#2-response-envelope--error-codes).
- Role guard: every workshop-profile method checks `mechanic.getRole() == MECHANIC` and throws
  a plain `BadRequestException` otherwise (consistent with `MechanicSettingsService`).

---

## 7. Database migrations

Liquibase changesets `023`–`036` (`src/main/resources/db/changelog/changes/`), registered in
`db.changelog-master.yaml` under a `# ── Workshop Profile feature ──` block. All are additive
(new tables only — no `ALTER` of existing tables). `027a` and `035a` seed the referral-code
catalogue and legal documents respectively. Verified by booting the app against a real
Postgres instance — all 14 changesets applied cleanly with no errors.

---

## 8. Deviations from the original frontend spec

| Doc said | Implementation does | Why |
|---|---|---|
| `{success, data, message, errors}` envelope, `/api/v1/workshop/...` paths | This app's existing `{success, message, data, timestamp, errorCode}` envelope, `/api/workshop-profile/...` | Consistency with every other controller in this codebase |
| `idProofUrl` included in the owner-info `PUT` body | Dedicated `POST /owner/id-proof` multipart endpoint | Avoids trusting client-supplied URLs; matches how images/support attachments are handled |
| Workshop Info / Location as separate resources | Backed by existing `User` columns, not new tables | Those fields already exist on `User` for mechanic accounts — no need to duplicate them |
| No admin decision endpoint shown | Added `POST /api/admin/workshop-profile/{mechanicId}/onboarding/decision` | Without it, a submitted onboarding could never actually be approved/rejected in production |
| Biometric section | No server endpoint | Confirmed local-only in the source doc |

---

## 9. Suggested next steps (not built here)

- Admin/ops endpoints to advance `WorkshopPickupDropStatus.currentStage` and suspend/unsuspend
  a workshop (`WorkshopProfile.suspended`) — both are currently read-only from this API and
  would need to be driven by whatever logistics/support tooling owns those workflows.
- Wiring `WorkshopPickupDropStatus.facilityEnabled` to the Settings ▸ Configuration
  "Pickup/Drop Facility" master switch once that Settings feature exists on this branch.
