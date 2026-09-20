# Backend Changes for Frontend — Issues #108–#122

**Branch:** `workshop_settings` (pushed)
**Scope:** Implements the remaining gaps from `BACKEND_SPECIFICATIONS_AND_REQUIREMENTS.md`, `RIDER_BOOKING_COLLABORATION_AND_BACKEND_SPECIFICATION.md`, and `WORKSHOP_API_AUDIT_AND_BACKEND_SPECIFICATION.md`. Everything below is new or changed; anything from those specs not mentioned here was already live before this change (Service Desk, Settlements, availability seeding, etc. — see the "Already live, no change" note at the end).

All endpoints follow existing platform conventions: `{ "success": boolean, "message": string, "data": T, "timestamp": string, "errorCode": string | null }`.

---

## 1. Auth — check-phone, captcha, request-otp (#108, #110)

### `GET /api/auth/check-phone?phone=9876543210`
Public. Pre-flight lookup before asking for a PIN.

```json
// exists = true
{ "success": true, "message": "Registered user found",
  "data": { "exists": true, "role": "VEHICLE_USER", "isVerified": true, "message": null } }

// exists = false
{ "success": true, "message": "Not yet registered? Please sign up.",
  "data": { "exists": false, "role": null, "isVerified": null, "message": "Not yet registered? Please sign up." } }
```

### `GET /api/auth/captcha`
Public. Returns a 5-digit numeric challenge, valid 5 minutes, single-use.

```json
{ "success": true, "message": "Captcha generated",
  "data": { "captchaId": "c4b3a128-...", "captchaCode": "48921", "expiresAt": "2026-09-15T12:35:00" } }
```

### `POST /api/auth/request-otp`
Public. Same OTP delivery as the existing `/send-otp`, but requires a solved captcha first.

```json
// Request
{ "phone": "9876543210", "captchaId": "c4b3a128-...", "captchaResponse": "48921", "purpose": "FORGOT_PIN" }

// Error on bad/expired/reused captcha
{ "success": false, "message": "Incorrect 5-digit number entered. Please try again.",
  "errorCode": "INVALID_CAPTCHA" }
```
`purpose` is accepted but not yet branched on server-side — pass whatever you have (`FORGOT_PIN`, `REGISTRATION`, etc.), it's informational only for now.

**Note:** the existing `POST /api/auth/send-otp` (no captcha) still works unchanged — `request-otp` is an additive, captcha-gated alternative, not a replacement.

---

## 2. Rider profile — display name & notification preferences (#111, #112)

`GET /api/users/me` and `PUT /api/users/me` gained three fields.

```json
{
  "displayName": "Ramesh K",
  "notificationsEnabled": true,
  "whatsappNotificationsEnabled": true,
  "...": "...(existing fields unchanged)"
}
```

- **`displayName`** in the `GET` response is always the *effective* display name: falls back to `firstName` when the stored value is null/blank. You don't need separate fallback logic on the client.
- **Subordination rule:** if you `PUT` `notificationsEnabled: false`, the backend force-sets `whatsappNotificationsEnabled: false` too, regardless of what you send for it. WhatsApp can only be `true` while `notificationsEnabled` is `true`.
- Omit any of the three fields in a `PUT` to leave them unchanged (same partial-update semantics as the rest of `UserUpdateRequest`).

**Note:** `email` and `city` are no longer required on `POST /api/auth/register/vehicle-user` (previously validation-blocked). If `email` is omitted, the backend auto-generates a placeholder (`{mobile}@no-email.alvexo.app`) since the column is still unique/non-null internally — this is invisible to you, just don't assume the `email` field on a freshly-registered vehicle user is one the rider actually typed.

---

## 3. Vehicle-make eligibility & brand matching (#115, #117)

### Vehicle-makes catalog — **use the existing endpoint, no new one was built**
`GET /api/vehicles/manufacturers?fuelType=PETROL` already returns the real catalogue of make names for a fuel type — this is what the spec's `GET /api/master/vehicle-makes` was asking for. Point the "Supported Vehicles" picker at it instead of a new path.

### `GET/POST /api/workshop/service-eligibility` (mechanic, self-scoped)
```json
// GET response / POST request body
{
  "entries": [
    { "fuelType": "PETROL", "serviceCategory": "GENERAL", "vehicleMakes": ["Hero", "Honda", "TVS"], "otherMakesAllowed": true },
    { "fuelType": "PETROL", "serviceCategory": "EXPRESS", "vehicleMakes": ["Hero", "Honda"], "otherMakesAllowed": false }
  ]
}
```
`POST` upserts by `(fuelType, serviceCategory)` — send only the entries you want to change/add, existing ones for other combinations are untouched.

**`serviceCategory` uses the platform's existing enum: `GENERAL | EXPRESS | REPAIR`** (not the spec doc's `GENERAL_REPAIRS` string — that name doesn't exist in this codebase's `ServiceCategory` enum, so `GENERAL` and `REPAIR` are separate configurable categories instead of one combined one).

### `GET /api/mechanics/{mechanicId}/service-eligibility` (public read)
Same shape as above, read-only, for previewing a specific workshop's coverage.

### `GET /api/mechanics/search` — new optional params
Added `vehicleMake` and `fuelType` as **additional, optional** query params — they layer on top of the existing "exactly one of `area`/`mobile`/`pinCode`" rule, they don't count against it.

```
GET /api/mechanics/search?area=Anna%20Nagar&vehicleMake=Honda&fuelType=PETROL
```

Each result in the response now includes:
```json
{
  "...": "...(existing fields)",
  "supportedBrands": ["Hero", "Honda", "TVS"],
  "isBrandSupported": true
}
```
- `supportedBrands` — flattened, de-duplicated list of every make the workshop has configured across all categories. Empty array if not configured yet.
- `isBrandSupported` — `null` when you didn't pass `vehicleMake`; `true`/`false` when you did.
- **Important:** if a workshop hasn't configured eligibility at all yet, `isBrandSupported` defaults to `true` (permissive) rather than hiding them — don't disable a card just because `supportedBrands` is empty; check `isBrandSupported` explicitly.

### Alternate Date Proposal flow
Two new endpoints on top of the existing booking ones:

**`POST /api/bookings/{id}/propose-date`** — mechanic only
```json
// Request
{ "proposedDateTime": "2026-09-18T09:00:00" }
```
This does **not** move `scheduledDateTime` yet — it sets `proposedDateTime`/`proposalStatus: "PENDING"` on the booking and notifies the rider.

**`POST /api/bookings/{id}/proposal-response`** — rider only
```json
// Request
{ "accept": true }
```
- `accept: true` → `scheduledDateTime` is updated to the proposed date, `proposalStatus: "ACCEPTED"`, mechanic notified.
- `accept: false` → booking is cancelled (`status: "CANCELLED"`), `proposalStatus: "DECLINED"`, mechanic notified. (Advance refund itself is not automated — same as every other cancellation path today.)

`BookingResponse` now includes `proposedDateTime` and `proposalStatus` (`NONE | PENDING | ACCEPTED | DECLINED`) on every booking read, so you can render the pending-proposal banner from the normal booking GET calls.

This is distinct from the existing `PUT /api/bookings/{id}/reschedule`, which still works exactly as before (immediate move, no accept/decline step) — use that for a straightforward reschedule, use propose/respond only for the "workshop is over capacity, needs rider's OK" flow.

---

## 4. Workshop settings extras (#122 + audit)

### `PUT /api/mechanic-settings` — two new optional fields
```json
{
  "...": "...(existing required fields unchanged)",
  "serviceAllocations": [
    { "serviceType": "Bike General", "auto": false, "mechanic": true },
    { "serviceType": "Bike Express", "auto": true, "mechanic": false }
  ],
  "allowGeneralUseExpressHours": true
}
```
Both are returned on `GET /api/mechanic-settings` and `GET /api/mechanic-settings/{mechanicId}` too. Omit `serviceAllocations` in a `PUT` to leave the existing array unchanged; `allowGeneralUseExpressHours` behaves the same way.

### `POST/GET /api/mechanic-settings/restore-points`, `POST /api/mechanic-settings/restore-points/{id}/restore`
```json
// POST /restore-points request
{ "name": "Pre-Festive Capacity Settings" }

// response (same shape for list items)
{ "id": 142, "name": "Pre-Festive Capacity Settings", "savedAt": "2026-09-15T10:15:00" }
```
**Behavior difference from the spec doc:** the snapshot is taken from the workshop's **currently-saved** settings + configuration on the server — the endpoint does not accept a `settings`/`configuration` payload to snapshot arbitrary unsaved values. Save your changes via the normal `PUT /api/mechanic-settings` and `PUT /api/mechanic-settings/configuration` first, *then* call this to bookmark that state. `POST .../{id}/restore` re-applies both the capacity settings and configuration settings that were active at snapshot time.

### Reminder change limiter + audit trail
No changes needed — this was already fully implemented (`reminderChangesThisYear`/`reminderChangesYear` counter capped at 2/year, plus the existing generic audit log surfaced at `GET /api/mechanic-settings/audit`). Nothing new to integrate here.

---

## 5. Workshop Current Status card (WS-STATUS-001)

### `GET /api/workshop-profile/status`
```json
{
  "success": true,
  "data": {
    "platformStatus": "ADMIN_VERIFICATION_PENDING",
    "trustLevel": "SELF_LISTED",
    "explanation": "Your onboarding has been submitted and is being reviewed.",
    "bookingsAllowed": false,
    "bookingWarning": "Customer bookings are not allowed while Admin verification is pending.",
    "updatedAt": "2026-09-15T14:30:00"
  }
}
```
`platformStatus` values actually produced by this endpoint: `INITIATE_ONBOARDING`, `DOCUMENTS_INSUFFICIENT`, `ADMIN_VERIFICATION_PENDING`, `REJECTED`, `SELF_LISTED`, `PAUSED_ADMIN_ACTION`, `DEACTIVATED`.

**Narrower than the original spec:** `ON_HOLD`, `PAUSED_WORKSHOP_REQUEST`, and `DELETION_REQUEST_PENDING` are not distinct states in the backend today — there's no workshop-initiated pause or deletion-request tracking yet, only admin suspension (`PAUSED_ADMIN_ACTION`) and account deactivation (`DEACTIVATED`). `trustLevel` is currently always `"SELF_LISTED"` — there's no multi-tier trust model behind it yet, so don't build UI that branches on it changing.

---

## Already live, no change (in case any doc suggested otherwise)
Confirmed already deployed and unaffected by this branch — Service Desk (today/week/pickup/drop/earnings), Settlements (list/receipt/statement/queries/faq), mechanic weekly availability + seeding, mechanic configuration settings + mechanic master, and `GENERAL_SERVICE`/`EXPRESS_SERVICE` booking service types.

## Migrations
Six new Liquibase changesets, `042`–`047` (a same-numbered `041` migration for user soft-delete landed on this branch from another PR first, so ours starts at `042`): captcha challenges table, user profile columns, workshop service eligibility table, booking proposal columns, mechanic settings service-allocation columns, and the settings restore-points table.
