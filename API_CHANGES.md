# API Changes — Job Card Type System

All responses are wrapped in `MyApiResponse<T>`:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2026-06-27T10:30:00"
}
```

---

## NEW Endpoints

### 1. Service Slots — `POST /api/mechanic-settings/service-slots`
**Auth:** MECHANIC | **Purpose:** Add a time-windowed service slot (TYPE_4)

**Request:**
```json
{
  "slotNumber": 1,
  "startTime": "11:00",
  "endTime": "13:00",
  "restrictedCategory": "REPAIR",
  "maxVehicleQty": 5,
  "autoAllocationQty": 3,
  "applicableDays": "MON,WED,FRI",
  "isEnabled": true
}
```

**Response:** `201 Created`
```json
{
  "id": 10,
  "slotNumber": 1,
  "startTime": "11:00",
  "endTime": "13:00",
  "restrictedCategory": "REPAIR",
  "maxVehicleQty": 5,
  "autoAllocationQty": 3,
  "manualReviewQty": 2,
  "applicableDays": "MON,WED,FRI",
  "isEnabled": true
}
```

---

### 2. Service Slots — `PUT /api/mechanic-settings/service-slots/{slotId}`
**Auth:** MECHANIC | **Purpose:** Update a service slot

**Request:** Same as POST  
**Response:** `200 OK` — Updated `MechanicServiceSlotResponse`

---

### 3. Service Slots — `DELETE /api/mechanic-settings/service-slots/{slotId}`
**Auth:** MECHANIC | **Purpose:** Delete a service slot

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Service slot deleted successfully",
  "data": null
}
```

---

### 4. Service Slots — `GET /api/mechanic-settings/{mechanicId}/service-slots`
**Auth:** Authenticated | **Purpose:** List enabled service slots for a mechanic (booking flow)

**Response:** `200 OK`
```json
[
  {
    "id": 10,
    "slotNumber": 1,
    "startTime": "11:00",
    "endTime": "13:00",
    "restrictedCategory": "REPAIR",
    "maxVehicleQty": 5,
    "autoAllocationQty": 3,
    "manualReviewQty": 2,
    "applicableDays": "MON,WED,FRI",
    "isEnabled": true
  }
]
```

---

### 5. Technicians — `POST /api/mechanic-settings/technicians`
**Auth:** MECHANIC | **Purpose:** Add a technician with reserved hours

**Request:**
```json
{
  "technicianName": "John",
  "reservedHours": 9.00,
  "isActive": true
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "technicianName": "John",
  "reservedHours": 9.00,
  "isActive": true
}
```

---

### 6. Technicians — `PUT /api/mechanic-settings/technicians/{techId}`
**Auth:** MECHANIC | **Purpose:** Update technician hours or toggle active status

**Request:** Same as POST  
**Response:** `200 OK` — Updated `MechanicTechnicianCapacityResponse`

---

### 7. Technicians — `DELETE /api/mechanic-settings/technicians/{techId}`
**Auth:** MECHANIC | **Purpose:** Remove a technician

**Response:** `200 OK` — `data: null`

---

### 8. Technicians — `GET /api/mechanic-settings/technicians`
**Auth:** MECHANIC | **Purpose:** List own technicians

**Response:** `200 OK`
```json
[
  { "id": 1, "technicianName": "John", "reservedHours": 9.00, "isActive": true },
  { "id": 2, "technicianName": "David", "reservedHours": 6.00, "isActive": true },
  { "id": 3, "technicianName": "Ravi", "reservedHours": 8.00, "isActive": false }
]
```

---

### 9. Daily Override — `POST /api/mechanic-settings/daily-override`
**Auth:** MECHANIC | **Purpose:** Set emergency capacity override for a date

**Request:**
```json
{
  "overrideDate": "2026-07-15",
  "overrideMaxVehicles": 25,
  "overrideCapacityHours": 42.00,
  "reason": "Festival rush — extending capacity"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "overrideDate": "2026-07-15",
  "overrideMaxVehicles": 25,
  "overrideCapacityHours": 42.00,
  "reason": "Festival rush — extending capacity",
  "isActive": true,
  "createdAt": "2026-06-27T10:00:00"
}
```

---

### 10. Daily Override — `PUT /api/mechanic-settings/daily-override/{overrideId}`
**Auth:** MECHANIC | **Purpose:** Update an override

**Request:** Same as POST  
**Response:** `200 OK` — Updated `DailyQuotaOverrideResponse`

---

### 11. Daily Override — `DELETE /api/mechanic-settings/daily-override/{overrideId}`
**Auth:** MECHANIC | **Purpose:** Remove an override

**Response:** `200 OK` — `data: null`

---

### 12. Daily Override — `GET /api/mechanic-settings/daily-overrides`
**Auth:** MECHANIC | **Purpose:** List own overrides (ordered by date desc)

**Response:** `200 OK` — `List<DailyQuotaOverrideResponse>`

---

### 13. Walk-In Booking — `POST /api/bookings/walk-in`
**Auth:** MECHANIC | **Purpose:** Mechanic creates a booking for walk-in customer (TYPE_3/4 only)

**Request:**
```json
{
  "customerName": "Ravi Kumar",
  "customerMobile": "9876543210",
  "vehicleDescription": "Honda Activa 2022 - TN09AB1234",
  "serviceSettingId": 15,
  "scheduledDateTime": "2026-07-15T09:00:00",
  "description": "Oil change and brake check",
  "customerNotes": "Walk-in customer"
}
```

**Response:** `201 Created`
```json
{
  "id": 150,
  "bookingNumber": "BKG1719468000001ABCD",
  "jobCardNumber": "010126071503",
  "vehicleUserId": null,
  "mechanicId": 5,
  "mechanicName": "Suresh Mech",
  "vehicleId": null,
  "scheduledDateTime": "2026-07-15T09:00:00",
  "status": "CONFIRMED",
  "bookingType": "STANDARD",
  "bookingSource": "WALK_IN",
  "allocationResult": "AUTO_CONFIRMED",
  "walkInCustomerName": "Ravi Kumar",
  "walkInCustomerMobile": "9876543210",
  "walkInVehicleDescription": "Honda Activa 2022 - TN09AB1234",
  "description": "Oil change and brake check"
}
```

---

### 14. Availability Check — `GET /api/bookings/availability`
**Auth:** Authenticated | **Purpose:** Per-category availability for UI grey-out

**Query params:** `mechanicId=5&date=2026-07-15`

**Response:** `200 OK`
```json
{
  "mechanicId": 5,
  "date": "2026-07-15",
  "jobCardType": "TYPE_2",
  "effectiveCapacity": 36.00,
  "usedCapacity": 28.00,
  "isFullyBooked": false,
  "availableByCategory": {
    "GENERAL": 3,
    "EXPRESS": 1,
    "REPAIR": 0,
    "COMPLEX": 2
  },
  "slots": [
    {
      "slotId": 10,
      "startTime": "11:00",
      "endTime": "13:00",
      "restrictedCategory": "REPAIR",
      "maxVehicleQty": 5,
      "bookedCount": 5,
      "isAvailable": false,
      "applicableDays": "MON,WED,FRI"
    }
  ]
}
```

---

### 15. Mechanic Dashboard — `GET /api/mechanic-dashboard/today`
**Auth:** MECHANIC | **Purpose:** Real-time booking counters for mechanic home screen

**Response:** `200 OK`
```json
{
  "date": "2026-07-15",
  "jobCardType": "TYPE_3",
  "effectiveCapacityHours": 36.00,
  "totalBookedCount": 19,
  "totalBookedHours": 28.50,
  "remainingCapacityHours": 7.50,
  "autoConfirmedCount": 8,
  "pendingReviewCount": 4,
  "riderAppCount": 12,
  "walkInCount": 7,
  "quotaOverrideActive": false,
  "technicianSummary": {
    "totalTechnicians": 4,
    "activeTechnicians": 4,
    "totalReservedHours": 36.00
  },
  "slotUtilization": [
    {
      "slotId": 10,
      "restrictedCategory": "REPAIR",
      "startTime": "11:00",
      "endTime": "13:00",
      "maxVehicleQty": 5,
      "bookedCount": 3,
      "autoConfirmedCount": 2,
      "pendingReviewCount": 1,
      "applicableDays": "MON,WED,FRI"
    }
  ]
}
```

---

## NEW: Create Settings Endpoint

### 16. Create Settings — `POST /api/mechanic-settings`
**Auth:** MECHANIC | **Purpose:** First-time setup of mechanic booking settings. Fails if settings already exist (use PUT to update).

**Request:**
```json
{
  "jobCardType": "TYPE_1",
  "classification": "AUTO",
  "maxVehiclesPerDay": 20,
  "reserveCapacity": false,
  "jobCardSerialPrefix": "0101",
  "serviceReportingTime": "08:30",
  "advanceEnabled": false
}
```

**Response:** `201 Created` — Full `MechanicSettingsResponse` (see #17 for response shape)

---

## UPDATED Endpoints

### 17. Update Settings — `PUT /api/mechanic-settings`
**Auth:** MECHANIC | **What changed:** Now update-only (fails if settings don't exist — use POST first). Used to change job card type, capacity config, etc.

**Request — example upgrading TYPE_1 → TYPE_4:**
```json
{
  "jobCardType": "TYPE_4",
  "classification": "AUTO",
  "maxVehiclesPerDay": 20,
  "reserveCapacity": true,
  "fullDayCapacityHours": 36.00,
  "jobCardSerialPrefix": "0101",
  "serviceReportingTime": "08:30",
  "expressReportingTime": "11:00",
  "advanceEnabled": true,
  "advanceAmount": 100.00,
  "totalDailyCapacityHours": null,
  "autoAllocationEnabled": true,
  "autoAllocationCapacityHours": 12.00,
  "serviceSettings": [
    {
      "category": "GENERAL",
      "serviceName": "Oil Change",
      "durationMinutes": 60,
      "maxSlotsPerDay": 5,
      "isExpressEligible": false,
      "isActive": true,
      "displayOrder": 0
    }
  ]
}
```

**Response (new fields highlighted):**
```json
{
  "id": 1,
  "mechanicId": 5,
  "mechanicName": "Suresh Mech",
  "jobCardType": "TYPE_3",
  "classification": "AUTO",
  "maxVehiclesPerDay": 20,
  "reserveCapacity": true,
  "fullDayCapacityHours": 36.00,
  "jobCardSerialPrefix": "0101",
  "serviceReportingTime": "08:30",
  "expressReportingTime": "11:00",
  "advanceEnabled": true,
  "advanceAmount": 100.00,
  "totalDailyCapacityHours": null,
  "autoAllocationEnabled": true,
  "autoAllocationCapacityHours": 12.00,
  "effectiveCapacityHours": 36.00,
  "reservedCapacityHours": 24.00,
  "serviceSettings": [ ... ],
  "technicianCapacities": [
    { "id": 1, "technicianName": "John", "reservedHours": 9.00, "isActive": true },
    { "id": 2, "technicianName": "David", "reservedHours": 9.00, "isActive": true }
  ],
  "createdAt": "2026-06-20T08:00:00",
  "updatedAt": "2026-06-27T10:30:00"
}
```

**New fields added to request:** `jobCardType`, `classification`, `totalDailyCapacityHours`, `autoAllocationEnabled`, `autoAllocationCapacityHours`

**New fields added to response:** above + `effectiveCapacityHours` (computed), `reservedCapacityHours` (computed), `technicianCapacities` (list)

---

### 17. Get Settings — `GET /api/mechanic-settings` / `GET /api/mechanic-settings/{mechanicId}`
**What changed:** Response now includes all new fields from #16

---

### 18. Add Service — `POST /api/mechanic-settings/services`
**What changed:** Added `category` field to request and response

**Request (new field):**
```json
{
  "category": "REPAIR",
  "serviceName": "Brake Cable Replacement",
  "durationMinutes": 45,
  "maxSlotsPerDay": 3,
  "isExpressEligible": false,
  "isActive": true,
  "displayOrder": 2
}
```

**Response (new field):**
```json
{
  "id": 12,
  "category": "REPAIR",
  "serviceName": "Brake Cable Replacement",
  "durationMinutes": 45,
  "maxSlotsPerDay": 3,
  "isExpressEligible": false,
  "isActive": true,
  "displayOrder": 2,
  "createdAt": "2026-06-27T10:00:00"
}
```

---

### 19. Update Service — `PUT /api/mechanic-settings/services/{serviceId}`
**What changed:** Added `category` field to request and response (same as #18)

---

### 20. Create Booking — `POST /api/bookings`
**What changed:** Response now includes allocation fields. Booking behaviour varies by mechanic's `jobCardType`:
- TYPE_1: Auto-confirmed, job card generated immediately
- TYPE_2: Auto-confirmed after per-service + hour cap validation
- TYPE_3: Auto-confirmed within auto-allocation hours, else PENDING for mechanic review
- TYPE_4: Routed to slot (if category matches) or general pool, auto/manual split

**Request:** Unchanged

**Response (new fields):**
```json
{
  "id": 123,
  "bookingNumber": "BKG1719468000001ABCD",
  "jobCardNumber": "010126071503",
  "status": "CONFIRMED",
  "bookingSource": "RIDER_APP",
  "allocationResult": "AUTO_CONFIRMED",
  "serviceSlotId": null,
  "walkInCustomerName": null,
  "walkInCustomerMobile": null,
  "walkInVehicleDescription": null,
  "...": "...other existing fields unchanged..."
}
```

---

### 21. Get Booking — `GET /api/bookings/{id}`
**What changed:** Response now includes `bookingSource`, `allocationResult`, `serviceSlotId`, walk-in fields (same as #20)

---

## Summary Table

| # | Method | Path | Status | Auth |
|---|--------|------|--------|------|
| 1 | POST | `/api/mechanic-settings/service-slots` | **NEW** | MECHANIC |
| 2 | PUT | `/api/mechanic-settings/service-slots/{id}` | **NEW** | MECHANIC |
| 3 | DELETE | `/api/mechanic-settings/service-slots/{id}` | **NEW** | MECHANIC |
| 4 | GET | `/api/mechanic-settings/{mechanicId}/service-slots` | **NEW** | Authenticated |
| 5 | POST | `/api/mechanic-settings/technicians` | **NEW** | MECHANIC |
| 6 | PUT | `/api/mechanic-settings/technicians/{id}` | **NEW** | MECHANIC |
| 7 | DELETE | `/api/mechanic-settings/technicians/{id}` | **NEW** | MECHANIC |
| 8 | GET | `/api/mechanic-settings/technicians` | **NEW** | MECHANIC |
| 9 | POST | `/api/mechanic-settings/daily-override` | **NEW** | MECHANIC |
| 10 | PUT | `/api/mechanic-settings/daily-override/{id}` | **NEW** | MECHANIC |
| 11 | DELETE | `/api/mechanic-settings/daily-override/{id}` | **NEW** | MECHANIC |
| 12 | GET | `/api/mechanic-settings/daily-overrides` | **NEW** | MECHANIC |
| 13 | POST | `/api/bookings/walk-in` | **NEW** | MECHANIC |
| 14 | GET | `/api/bookings/availability` | **NEW** | Authenticated |
| 15 | GET | `/api/mechanic-dashboard/today` | **NEW** | MECHANIC |
| 16 | POST | `/api/mechanic-settings` | **NEW** | MECHANIC |
| 17 | PUT | `/api/mechanic-settings` | **UPDATED** | MECHANIC |
| 18 | GET | `/api/mechanic-settings` / `{mechanicId}` | **UPDATED** | MECHANIC / Authenticated |
| 19 | POST | `/api/mechanic-settings/services` | **UPDATED** | MECHANIC |
| 20 | PUT | `/api/mechanic-settings/services/{id}` | **UPDATED** | MECHANIC |
| 21 | POST | `/api/bookings` | **UPDATED** | Authenticated |
| 22 | GET | `/api/bookings/{id}` | **UPDATED** | Authenticated |
