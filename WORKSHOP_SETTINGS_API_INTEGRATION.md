# Workshop Settings & Home — Frontend Integration Guide

Implements the contract described in `WORKSHOP_SETTINGS_HOME_SPEC.md`. All endpoints are
under the servlet context path (`/vehiclserviceapi` in this environment) and require a
`MECHANIC`-role JWT unless noted otherwise. All responses use the standard envelope:

```json
{
  "success": true,
  "message": "Settings saved successfully",
  "data": { /* ... */ },
  "timestamp": "2026-07-10T09:15:00"
}
```

On error, `success` is `false`, `data` is usually `null` (except validation errors — see
[Error handling](#error-handling)), and `message` describes the problem. HTTP status codes
are meaningful (`400`, `404`, `409`, `500`) — don't rely on `success` alone.

---

## 1. Level → API field mapping

The mechanic-facing **Workshop Capacity Level (1–4)** is not sent directly. Derive
`jobCardType` + `reserveCapacity` + `reserveForSlots` from the selected Level tab and send
those; the backend derives and returns `classification` (`TYPE_1`.."TYPE_4") itself — treat
it as read-only/informational, it will be ignored if sent in a request.

| Mechanic sees | `jobCardType` | `reserveCapacity` | `reserveForSlots` | `classification` (response) |
|---|---|---|---|---|
| **Level 1** — Auto Basic | `AUTO` | `false` | `false` | `TYPE_1` |
| **Level 2** — Auto + Hours | `AUTO` | `true` | `false` | `TYPE_2` |
| **Level 3** — Mechanic Day | `MECHANIC` | `false` | `false` | `TYPE_3` |
| **Level 4** — Mechanic Slot | `MECHANIC` | `false` | `true` | `TYPE_4` |

**Important:** the backend normalizes on save — if `jobCardType = MECHANIC`, whatever you
send for `reserveCapacity` is forced to `false`; if `jobCardType = AUTO`, `reserveForSlots`
must be sent as `false` or the request is rejected with 400. Only send the flag pair that
matches the chosen Level.

Use `classification` from the `GET` response to decide which dashboard layout to render —
don't re-derive it client-side.

---

## 2. Settings API (`/api/mechanic-settings`)

### `PUT /api/mechanic-settings` — save (create or update)

Auth: `MECHANIC`. First save for a mechanic creates the row; subsequent saves fully replace
it. If `serviceSettings` is present in the body it **replaces all** existing service rows;
omit the key entirely to leave services untouched.

**Request body — all fields required unless noted:**

```jsonc
{
  "jobCardType": "AUTO",              // "AUTO" | "MECHANIC"
  "maxVehiclesPerDay": 20,             // int, >= 1 — validated for every Level
  "reserveCapacity": false,            // bool — meaningful only for AUTO
  "reserveForSlots": false,            // bool — meaningful only for MECHANIC
  "fullDayCapacityHours": 8.0,         // decimal, >= 0.5 — validated for every Level
  "jobCardSerialPrefix": "0101",       // string, 2-10 chars
  "serviceReportingTime": "08:30",     // "HH:mm", required
  "expressReportingTime": "07:30",     // "HH:mm", REQUIRED only if reserveCapacity=true, else omit/null
  "advanceEnabled": false,
  "advanceAmount": 200.00,             // REQUIRED only if advanceEnabled=true, else omit/null
  "autoAllocationEnabled": false,
  "autoAllocationCapacityHours": 2.0,  // decimal, >= 0.5 — validated for every Level
  "autoIssue": true,                   // bool, required — false = job cards must be issued manually (§2.1)
  "serviceSettings": [                 // OPTIONAL — omit key to leave existing services unchanged
    {
      "serviceName": "Oil Change",
      "category": "GENERAL",           // "GENERAL" | "EXPRESS" | "REPAIR" — required
      "durationMinutes": 30,           // int, >= 1
      "maxSlotsPerDay": 10,            // int, >= 1, optional (null = no per-service cap)
      "isExpressEligible": false,
      "isActive": true,
      "displayOrder": 0
    }
  ]
}
```

**Response `data`:**

```jsonc
{
  "id": 1,
  "mechanicId": 42,
  "mechanicName": "Suresh Mech",
  "jobCardType": "AUTO",
  "classification": "TYPE_1",          // derived — read-only
  "maxVehiclesPerDay": 20,
  "reserveCapacity": false,
  "reserveForSlots": false,
  "fullDayCapacityHours": 8.0,
  "jobCardSerialPrefix": "0101",
  "serviceReportingTime": "08:30:00",
  "expressReportingTime": null,
  "advanceEnabled": false,
  "advanceAmount": null,
  "autoAllocationEnabled": false,
  "autoAllocationCapacityHours": 2.0,
  "autoIssue": true,
  "serviceSettings": [ /* MechanicServiceSettingResponse[] — see below */ ],
  "createdAt": "2026-07-10T09:00:00",
  "updatedAt": "2026-07-10T09:00:00"
}
```

### `GET /api/mechanic-settings` — my settings
Auth: `MECHANIC`. Returns the same shape as above. **404** if not configured yet — show the
Settings screen in "first-time setup" state rather than treating it as an error toast.

### `GET /api/mechanic-settings/{mechanicId}` — settings by mechanic ID
Auth: any authenticated user. Used by the booking flow to read another mechanic's settings.

### 2.1 Manual job-card issuance (`autoIssue = false`)

When `autoIssue` is `false`, confirming a booking (`PUT /api/bookings/{id}/status?status=CONFIRMED`)
does **not** generate a `jobCardNumber`. The mechanic must issue it explicitly:

`PUT /api/bookings/{id}/issue-job-card` — Auth: `MECHANIC`, assigned mechanic only. **400** if
the booking isn't `CONFIRMED` yet, or if a job card was already issued. Returns the updated
`BookingResponse` (see §9).

---

## 3. Service catalogue API (`/api/mechanic-settings/services`)

Individual CRUD, independent of the bulk `serviceSettings` array in the settings save.

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/api/mechanic-settings/services` | MECHANIC | Body: `MechanicServiceSettingRequest` (see fields above). Returns `201`. |
| `PUT` | `/api/mechanic-settings/services/{serviceId}` | MECHANIC | Full replace of one service row. |
| `DELETE` | `/api/mechanic-settings/services/{serviceId}` | MECHANIC | Hard delete. |
| `GET` | `/api/mechanic-settings/{mechanicId}/services` | any authenticated | Active services only, ordered by `displayOrder` — this is the Home dashboard's **F column** source. |

**`MechanicServiceSettingResponse`:**
```jsonc
{
  "id": 5,
  "serviceName": "Oil Change",
  "category": "GENERAL",
  "durationMinutes": 30,
  "maxSlotsPerDay": 10,
  "isExpressEligible": false,
  "isActive": true,
  "displayOrder": 0,
  "createdAt": "2026-07-10T09:00:00"
}
```

---

## 4. Service Slots API — Slot 1 / Slot 2 (Level 4 only)

New in this integration. Backend stores slot config regardless of the mechanic's current
Level, but it's only rendered on Level 4 (Mechanic Slot).

### `POST` or `PUT /api/mechanic-settings/service-slots` — upsert
Auth: `MECHANIC`. **Requires mechanic settings to already exist** (`PUT /mechanic-settings`
must have been called at least once) — otherwise **400**. Create-or-update by `slotNumber`;
send only the slots you want to change, up to 2 items.

```jsonc
// Request
{
  "slots": [
    { "slotNumber": 1, "slotTime": "09:00", "repairQty": 5, "enabled": true },
    { "slotNumber": 2, "slotTime": "13:00", "repairQty": 5, "enabled": true }
  ]
}
```

Rules:
- `slotNumber` must be `1` or `2`; duplicate slot numbers in one request → **400**.
- `slotTime` is required whenever `enabled` is `true` (or omitted, since it defaults to `true`).
- `repairQty` (the slot's "Veh. Qty") must be ≥ 1.

**Response `data`** (array, sorted by `slotNumber`):
```jsonc
[
  { "id": 10, "slotNumber": 1, "slotTime": "09:00:00", "repairQty": 5, "enabled": true,
    "createdAt": "2026-07-10T09:00:00", "updatedAt": "2026-07-10T09:00:00" },
  { "id": 11, "slotNumber": 2, "slotTime": "13:00:00", "repairQty": 5, "enabled": true,
    "createdAt": "2026-07-10T09:00:00", "updatedAt": "2026-07-10T09:00:00" }
]
```

### `GET /api/mechanic-settings/{mechanicId}/service-slots`
Auth: any authenticated user. Same array shape as above; empty array if no slots configured
(**not** a 404) — treat that as "Slot 1/2 not yet configured".

---

## 5. Home Dashboard API — `GET /api/mechanic-dashboard/today`

New in this integration. Auth: `MECHANIC`. Returns everything needed to render **today's**
Home dashboard in one call — no need to separately call the counters/services/slots
endpoints just for the dashboard screen. **404** if settings aren't configured yet (same
"first-time setup" handling as `GET /mechanic-settings`).

```jsonc
{
  "date": "2026-07-10",
  "jobCardType": "MECHANIC",
  "classification": "TYPE_4",
  "maxVehiclesPerDay": 20,
  "totalBookedCount": 12,          // CONFIRMED — excludes cancelled/rejected
  "autoConfirmedCount": 9,         // JC ISSUED — bookings with a job card number already
  "pendingCount": 3,               // Job Card Waiting List
  "walkInCount": 2,                // bookings with channel = WALK_IN
  "riderCount": 0,                 // bookings with channel = RIDER_APP
  "capacityUtilizationPercent": 62.50,
  "serviceBreakdown": [
    {
      "serviceId": 5,
      "serviceName": "Oil Change",
      "category": "GENERAL",
      "maxSlotsPerDay": 10,
      "bookedCount": 6,            // F
      "issuedCount": 4,            // I
      "pendingCount": 2            // P
    }
  ],
  "slots": [                       // empty array unless classification = TYPE_4
    { "slotNumber": 1, "slotTime": "09:00:00", "enabled": true, "plannedCount": 5, "issuedCount": 3 },
    { "slotNumber": 2, "slotTime": "13:00:00", "enabled": true, "plannedCount": 5, "issuedCount": 2 }
  ]
}
```

**Rendering guide by `classification`** (all fields are always present — just pick which
tiles to show):

| classification | Tiles to render |
|---|---|
| `TYPE_1` (Auto Basic) | `maxVehiclesPerDay`, `totalBookedCount`, `autoConfirmedCount`, `capacityUtilizationPercent` |
| `TYPE_2` (Auto + Hours) | `serviceBreakdown` (F/I columns + sum for TOTAL VEHICLES), `capacityUtilizationPercent` |
| `TYPE_3` (Mechanic Day) | `serviceBreakdown` (F/I/P columns), `pendingCount` (waiting list) |
| `TYPE_4` (Mechanic Slot) | everything in `TYPE_3` **+** `slots` (Slot 1/2 cards: Planned = `plannedCount`, JC Issued = `issuedCount`) |

`slots[].issuedCount` is computed by exact match against `scheduledDateTime`'s time-of-day —
if your booking flow lets customers pick an arbitrary time instead of the exact slot time for
Level 4, that count will under-report; flag this to backend if that's the case.

### 5.1 Any date — `GET /api/mechanic-dashboard?date=YYYY-MM-DD`

Same response shape as `/today`, for the 7-day strip. `date` is required.

### 5.2 Waiting-list drill-down — `GET /api/mechanic-dashboard/pending?date=YYYY-MM-DD`

Auth: `MECHANIC`. `date` optional (defaults to today). Returns `BookingDetailResponse[]` — the
actual vehicles behind `pendingCount`, for the tap-through UI on Types 3/4:

```jsonc
[{
  "bookingId": 501,
  "bookingNumber": "BKG...", "jobCardNumber": null,
  "vehicleRegistrationNumber": "KA01AB1234",   // null if the vehicle has no registration on file
  "vehicleInfo": "Honda Activa",
  "customerName": "Ravi Kumar", "customerPhone": "9876543210",
  "serviceSettingName": "Oil Change",
  "scheduledDateTime": "2026-07-10T10:00:00",
  "status": "PENDING", "channel": "ONLINE"
}]
```

Approve → `PUT /api/bookings/{bookingId}/status?status=CONFIRMED` (existing endpoint).
Reschedule → `PUT /api/bookings/{bookingId}/reschedule?scheduledDateTime=2026-07-11T10:00:00`
(Auth: `MECHANIC`, assigned mechanic only; re-checks slot conflicts, does **not** re-run
capacity checks since the booking already holds a slot for that day).

### 5.3 Issued-JC drill-down — `GET /api/mechanic-dashboard/services/{serviceId}/issued?date=`

Same `BookingDetailResponse[]` shape, filtered to bookings with a `jobCardNumber` for one
service row — the vehicles behind a service's `issuedCount` (I column).

### 5.4 Service Due / Second Reminder — `GET /api/mechanic-dashboard/reminders`

Auth: `MECHANIC`. Vehicles whose most recent **completed** booking with this mechanic has
passed `serviceDueIntervalDays` (from Configuration Settings, §10 — defaults to 90/15 if not
configured yet):

```jsonc
[{
  "vehicleId": 12, "vehicleRegistrationNumber": "KA01AB1234", "vehicleInfo": "Honda Activa",
  "customerId": 8, "customerName": "Ravi Kumar", "customerPhone": "9876543210",
  "lastServiceDate": "2026-04-01", "daysSinceService": 100,
  "reminderStage": "DUE"          // "DUE" | "SECOND_REMINDER"
}]
```

Only vehicles that are actually due appear (no "NONE" rows). This endpoint computes the list
live — it does not send notifications; wiring an actual push/SMS reminder is still open.

---

## 6. Error handling

| Status | Meaning | `data` shape |
|---|---|---|
| `400` | Bean validation failure (missing/invalid field) | `{ "fieldName": "error message", ... }` — map keyed by field name |
| `400` | Business rule violation (e.g. `reserveForSlots` true with `jobCardType=AUTO`, slots saved before settings exist) | `null` — read `message` |
| `404` | Settings/mechanic/resource not found | `null` |
| `409` | Conflict (e.g. duplicate slot number persisted concurrently) | `null` |
| `500` | Unexpected server error | `null` — generic message only, no stack trace |

Example validation error:
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "fullDayCapacityHours": "fullDayCapacityHours must be at least 0.5",
    "jobCardType": "jobCardType is required"
  },
  "timestamp": "2026-07-10T09:15:00"
}
```

---

## 7. Booking channel & walk-in

`BookingResponse.channel` is now `"ONLINE" | "WALK_IN" | "RIDER_APP"`. Existing/online bookings
are always `ONLINE`; nothing changes for that flow.

`POST /api/bookings/walk-in` — Auth: `MECHANIC`. Mechanic records a walk-in customer at the
workshop. Requires an **already-registered** `vehicleUserId` + `vehicleId` (there's no
guest/unregistered-customer flow — if the customer isn't in the system yet, register them
first). `scheduledDateTime` defaults to now if omitted. Confirmed **immediately** (no PENDING
step, since the customer is physically present) and still subject to the mechanic's normal
capacity/advance rules — a full workshop rejects a walk-in just like it would an online booking.

```jsonc
// Request
{
  "vehicleUserId": 8, "vehicleId": 12,
  "serviceType": "OIL_CHANGE", "description": "Routine service",
  "serviceSettingId": 5,           // required in hour-slot mode, same as regular bookings
  "advancePaid": 0
}
```
Returns `201` + `BookingResponse` with `status: "CONFIRMED"`, `channel: "WALK_IN"`.

`RIDER_APP` has no creation endpoint yet — reserved for a future partner-app integration; the
field exists so the dashboard's `riderCount` won't need a schema change later.

---

## 8. Configuration Settings (`/api/mechanic-settings/configuration`)

Previously local-only (spec §4 "Needed") — now backed by its own row, separate from the main
settings save.

### `PUT /api/mechanic-settings/configuration` — save (create or update)
Auth: `MECHANIC`. All fields required.

```jsonc
{
  "jobCardNumberStartingSequence": "0001",
  "jobCardNumberFormat": "NUMERIC",              // "NUMERIC" | "ALPHANUMERIC"
  "jobCardNumberResetFrequency": "WEEKLY",       // "DAILY" | "WEEKLY" | "MONTHLY" | "NEVER"
  "jobCardNumberPrefix": "AB",                   // ≤ 4 chars, optional
  "jobCardNumberSuffix": null,                   // ≤ 4 chars, optional
  "rescheduleLimit": 2,                          // >= 0
  "rescheduleCutoffTime": "10:00",
  "pickupDropFacilityEnabled": false,
  "autoConfirmOtherState": true,
  "repairsRequireAdvance": false,
  "serviceDueIntervalDays": 90,                  // >= 1
  "secondReminderIntervalDays": 15               // >= 1
}
```

> **Note:** this is metadata only — actual job card numbers are still generated from
> `mechanic_settings.jobCardSerialPrefix` (§2). `jobCardNumberPrefix/Suffix/Format/Reset`
> here are not yet wired into the generator; treat them as configuration-at-rest until backend
> confirms the generator has been switched over.

**Reminder-interval change cap:** `serviceDueIntervalDays`/`secondReminderIntervalDays` can
only be changed **2 times per calendar year** (resets each January 1). A 3rd change in the
same year → **400**. The response includes `reminderChangesThisYear` and
`reminderChangesRemainingThisYear` so the UI can warn before the limit is hit.

### `GET /api/mechanic-settings/configuration` — my configuration
Auth: `MECHANIC`. **404** if not set up yet (defaults shown above apply until first save).

### Mechanic Master CRUD

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/api/mechanic-settings/configuration/mechanics` | MECHANIC | `{ "name": "...", "phone": "...", "active": true, "displayOrder": 0 }` → `201` |
| `PUT` | `/api/mechanic-settings/configuration/mechanics/{entryId}` | MECHANIC | Full replace |
| `DELETE` | `/api/mechanic-settings/configuration/mechanics/{entryId}` | MECHANIC | Hard delete |
| `GET` | `/api/mechanic-settings/configuration/{mechanicId}/mechanics` | any authenticated | List, ordered by `displayOrder` |

These are directory entries only (name/phone/active) — not separate login accounts.

---

## 9. Promotional Offers (`/api/mechanic-settings/promotional-offers`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/api/mechanic-settings/promotional-offers` | MECHANIC | See body below → `201` |
| `PUT` | `/api/mechanic-settings/promotional-offers/{offerId}` | MECHANIC | Full replace |
| `DELETE` | `/api/mechanic-settings/promotional-offers/{offerId}` | MECHANIC | Hard delete |
| `GET` | `/api/mechanic-settings/promotional-offers/{mechanicId}` | any authenticated | All offers (mechanic's own management view) |
| `GET` | `/api/mechanic-settings/promotional-offers/{mechanicId}/active` | any authenticated | Only `active=true` and today within `[startDate, endDate]` — this is the customer-facing/Home dashboard feed |

```jsonc
{
  "title": "Monsoon Service Special",
  "description": "20% off on general service",
  "discountType": "PERCENTAGE",      // "PERCENTAGE" | "FLAT_AMOUNT"
  "discountValue": 20.0,             // PERCENTAGE must be <= 100
  "startDate": "2026-07-01",
  "endDate": "2026-07-31",
  "active": true
}
```

---

## 10. Holiday / Pause (`/api/mechanic-settings/holidays`)

Blocks new bookings (online, walk-in, and reschedules land on capacity checks the same way)
for a given date. **No automatic redistribution** of existing bookings on that date — this is
a deliberate scope limit; handle already-booked vehicles on a newly-marked date manually until
a redistribution flow is designed.

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/api/mechanic-settings/holidays` | MECHANIC | `{ "date": "2026-08-15", "type": "HOLIDAY", "reason": "..." }` — `type` is `"HOLIDAY"` (fully closed) or `"PAUSE"` (open but not accepting bookings). Cannot be a past date. → `201`, or `400` if that date is already marked. |
| `DELETE` | `/api/mechanic-settings/holidays/{holidayId}` | MECHANIC | Removes the marker (bookings become possible again) |
| `GET` | `/api/mechanic-settings/holidays/{mechanicId}?from=&to=` | any authenticated | `from`/`to` required — powers the 7-day strip / calendar view |

A booking attempt (online, walk-in) on a marked date fails with **400**: `"Mechanic is on
holiday on 2026-08-15 (reason)"`.

---

## 11. One-Day Capacity & Advance Change (`/api/mechanic-settings/daily-overrides`)

Per-date override of `maxVehiclesPerDay` / `fullDayCapacityHours` / `advanceEnabled` /
`advanceAmount`. Any field left `null` falls back to the mechanic's standing settings (§2) for
that date. At least one field must be set.

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` or `PUT` | `/api/mechanic-settings/daily-overrides` | MECHANIC | Upsert by `date` (see body below) |
| `DELETE` | `/api/mechanic-settings/daily-overrides/{overrideId}` | MECHANIC | Removes the override for that date |
| `GET` | `/api/mechanic-settings/daily-overrides/{mechanicId}?from=&to=` | any authenticated | `from`/`to` required |

```jsonc
{
  "date": "2026-08-20",
  "maxVehiclesPerDayOverride": 30,        // omit/null = no change to this field
  "fullDayCapacityHoursOverride": null,
  "advanceEnabledOverride": null,
  "advanceAmountOverride": null
}
```

Both the booking capacity checks (`POST /bookings`, `POST /bookings/walk-in`) and the
dashboard's `capacityUtilizationPercent` (§5) automatically pick up the override for that date
— no separate wiring needed on the client beyond calling this API to set it.

---

## 12. Settings Audit (`/api/mechanic-settings/audit`)

Field-level change history for both the main settings (§2) and Configuration Settings (§8).
A row is written **only** for fields that actually changed on a given save (first-time
creation is not audited — nothing to compare against).

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/mechanic-settings/audit` | Last 50 changes, most recent first |
| `GET` | `/api/mechanic-settings/audit/range?from=&to=` | ISO date-time range, e.g. `from=2026-07-01T00:00:00&to=2026-07-31T23:59:59` |

```jsonc
[{
  "id": 1, "entityType": "MECHANIC_SETTINGS", "fieldName": "maxVehiclesPerDay",
  "oldValue": "20", "newValue": "25",
  "changedByUserId": 42, "changedByName": "Suresh Mech",
  "changedAt": "2026-07-10T09:15:00"
}]
```

`entityType` is `"MECHANIC_SETTINGS"` or `"CONFIGURATION_SETTINGS"`. `oldValue`/`newValue` are
plain `.toString()` of the field (e.g. enum name, decimal, boolean) — format for display
per-field on the client rather than assuming a fixed type.

---

## 13. Not implemented yet

- **Redistribution logic** for existing bookings when a date is newly marked Holiday/Pause (§10).
- **Notification delivery** for Service Due / Second Reminders (§5.4) and Promotional Offers
  (§9) — both are queryable lists today, not push/SMS sends.
- **Job-card-number generator** does not yet consume `jobCardNumberFormat/Prefix/Suffix/ResetFrequency`
  from Configuration Settings (§8) — it still only uses `mechanic_settings.jobCardSerialPrefix`.
- `RIDER_APP` booking channel has no creation endpoint (partner-app integration pending).
