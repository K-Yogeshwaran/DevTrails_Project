# DevTrails Backend API Documentation

## Overview
This is a comprehensive API documentation for the DevTrails backend service. The backend is a Spring Boot Java application that provides insurance services for gig workers, including policy management, claim processing, and real-time disruption tracking.

**Server Configuration:**
- **Base URL:** `http://localhost:8080`
- **Database:** PostgreSQL (localhost:5432/gigshield_db)
- **Authentication:** JWT-based with 24-hour expiry
- **CORS:** Enabled for all origins

## Authentication

### JWT Token System
The API uses JWT (JSON Web Tokens) for authentication. Tokens are valid for 24 hours (`86400000` ms).

**Authorization Header:**
```
Authorization: Bearer <jwt_token>
```

### Admin Credentials
Hardcoded admin accounts for hackathon:
- **Username:** `admin` | **Password:** `gigshield@admin2026`
- **Username:** `devtrails` | **Password:** `devtrails@2026`
- **Username:** `yogesh` | **Password:** `yogesh@admin`
- **Username:** `sriram` | **Password:** `sriram@admin`

### Public Endpoints (No Authentication Required)
- `/api/workers/register`
- `/api/workers/login`
- `/api/*/health`
- `/api/trigger-events` (all endpoints)
- `/api/admin/login`
- `/api/admin/**` (all admin endpoints)

---

## API Endpoints

## 1. Workers API (`/api/workers`)

### Register New Worker
**POST** `/api/workers/register`

**Request Body:**
```json
{
  "name": "John Doe",
  "phone": "9876543210",
  "email": "john@example.com",
  "zoneId": "zone-001",
  "persona": "delivery",
  "dailyEarnings": 500,
  "activeHours": 8,
  "experienceMonths": 12,
  "daysPerWeek": 6,
  "password": "password123"
}
```

**Validations:**
- `phone`: 10-digit mobile number starting with 6-9
- `dailyEarnings`: ₹100 - ₹10,000
- `activeHours`: 1-16 hours
- `password`: Minimum 6 characters
- `persona`: Required field
- `zoneId`: Required field

**Response:**
```json
{
  "workerId": "worker-uuid-string",
  "name": "John Doe",
  "phone": "9876543210",
  "zoneId": "zone-001",
  "persona": "delivery",
  "dailyEarnings": 500,
  "token": "jwt-token-string",
  "message": "Worker registered successfully"
}
```

### Worker Login
**POST** `/api/workers/login`

**Request Body:**
```json
{
  "phone": "9876543210",
  "password": "password123"
}
```

**Response:**
```json
{
  "workerId": "worker-uuid-string",
  "name": "John Doe",
  "phone": "9876543210",
  "zoneId": "zone-001",
  "persona": "delivery",
  "dailyEarnings": 500,
  "token": "jwt-token-string",
  "message": "Login successful"
}
```

### Get Worker Profile
**GET** `/api/workers/{workerId}`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "workerId": "worker-uuid-string",
  "name": "John Doe",
  "phone": "9876543210",
  "email": "john@example.com",
  "zoneId": "zone-001",
  "persona": "delivery",
  "dailyEarnings": 500,
  "activeHours": 8,
  "experienceMonths": 12,
  "daysPerWeek": 6,
  "isActive": true,
  "createdAt": "2024-01-15T10:30:00"
}
```

### Update Worker Profile
**PUT** `/api/workers/{workerId}`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "dailyEarnings": 600,
  "activeHours": 9,
  "zoneId": "zone-002",
  "daysPerWeek": 5
}
```

**Response:** Same as Get Worker Profile

### Get Workers by Zone
**GET** `/api/workers/zone/{zoneId}`

**Response:** Array of worker profiles (same structure as above)

### Get All Active Workers
**GET** `/api/workers`

**Response:** Array of active worker profiles

### Deactivate Worker
**DELETE** `/api/workers/{workerId}`

**Response:**
```json
{
  "message": "Worker deactivated successfully",
  "workerId": "worker-uuid-string"
}
```

---

## 2. Policies API (`/api/policies`)

### Create New Policy
**POST** `/api/policies`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "workerId": "worker-uuid-string",
  "tier": "standard",
  "season": "summer"
}
```

**Validations:**
- `tier`: Must be `basic`, `standard`, or `premium`
- `season`: Must be `summer`, `monsoon`, `winter`, or `spring`

**Response:**
```json
{
  "policyNumber": "policy-uuid-string",
  "workerId": "worker-uuid-string",
  "tier": "standard",
  "weeklyPremium": 45.50,
  "coverageCap": 1000.00,
  "coverageUsed": 0.00,
  "coverageRemaining": 1000.00,
  "season": "summer",
  "status": "active",
  "weekStart": "2024-01-15",
  "weekEnd": "2024-01-21",
  "createdAt": "2024-01-15T10:30:00",
  "message": "Policy created successfully"
}
```

### Get Current Policy
**GET** `/api/policies/{workerId}/current`

**Headers:** `Authorization: Bearer <token>`

**Response:** Same as Create Policy Response

### Get Policy History
**GET** `/api/policies/{workerId}/history`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
[
  {
    "policyNumber": "policy-uuid-string",
    "tier": "standard",
    "weeklyPremium": 45.50,
    "coverageCap": 1000.00,
    "coverageUsed": 200.00,
    "status": "expired",
    "weekStart": "2024-01-08",
    "weekEnd": "2024-01-14"
  }
]
```

### Check Coverage
**GET** `/api/policies/{workerId}/coverage-check?date=2024-01-15`

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `date` (optional): Date to check coverage (defaults to today)

**Response:**
```json
{
  "isCovered": true,
  "policyNumber": "policy-uuid-string",
  "coverageRemaining": 800.00,
  "reason": ""
}
```

### Expire Old Policies
**POST** `/api/policies/expire-old`

**Response:**
```json
{
  "message": "Old policies expired successfully"
}
```

---

## 3. Claims API (`/api/claims`)

### Get Worker Claims
**GET** `/api/claims/worker/{workerId}`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
[
  {
    "claimId": "claim-uuid-string",
    "triggerType": "rainfall",
    "payoutAmount": 150.00,
    "status": "approved",
    "triggeredAt": "2024-01-15T14:30:00"
  }
]
```

### Get Claim Details
**GET** `/api/claims/detail/{claimId}`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "claimId": "claim-uuid-string",
  "workerId": "worker-uuid-string",
  "policyNumber": "policy-uuid-string",
  "triggerType": "rainfall",
  "triggerValue": 45.2,
  "zoneId": "zone-001",
  "disruptedHours": 4.5,
  "payoutAmount": 150.00,
  "fraudScore": 0.15,
  "status": "approved",
  "triggeredAt": "2024-01-15T14:30:00",
  "processedAt": "2024-01-15T15:45:00",
  "message": "Claim processed successfully"
}
```

### Get Claim Processing Logs
**GET** `/api/claims/logs/{claimId}`

**Headers:** `Authorization: Bearer <token>`

**Response:** Array of processing log entries with timestamps and status updates

### Get Claim by Event ID
**GET** `/api/claims/event/{eventId}`

**Headers:** `Authorization: Bearer <token>`

**Response:** 
- If claim exists: Full claim details (same as Get Claim Details)
- If claim doesn't exist: `{"status": "queued", "eventId": "event-id"}`

### Get Claims Analytics
**GET** `/api/claims/analytics?workerId={workerId}`

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `workerId` (optional): Get analytics for specific worker or all workers

**Response:**
```json
{
  "totalClaims": 25,
  "approvedClaims": 20,
  "rejectedClaims": 3,
  "flaggedClaims": 2,
  "totalPaidOut": 3500.00,
  "approvalRate": 80.0,
  "fraudRate": 8.0
}
```

---

## 4. Trigger Events API (`/api/trigger-events`)

### Create Trigger Event
**POST** `/api/trigger-events`

**Request Body:**
```json
{
  "eventId": "event-uuid-string",
  "triggerType": "rainfall",
  "zoneId": "zone-001",
  "zoneName": "Downtown Area",
  "triggerValue": 45.2
}
```

**Response:**
```json
{
  "eventId": "event-uuid-string",
  "triggerType": "rainfall",
  "zoneId": "zone-001",
  "zoneName": "Downtown Area",
  "triggerValue": 45.2,
  "status": "active",
  "startedAt": "2024-01-15T14:30:00",
  "endedAt": null,
  "disruptedHours": null,
  "affectedWorkerIds": "worker1,worker2,worker3",
  "elapsedSeconds": 3600,
  "message": "Trigger event created"
}
```

### Resolve Trigger Event
**POST** `/api/trigger-events/resolve`

**Request Body:**
```json
{
  "eventId": "event-uuid-string",
  "activeWorkerIds": ["worker1", "worker2", "worker3"]
}
```

**Response:** Same as Create Trigger Event Response with `endedAt` and `disruptedHours` populated

### Get Active Events by Zone
**GET** `/api/trigger-events/active/{zoneId}`

**Response:** Array of active trigger events for the specified zone

### Get All Active Events
**GET** `/api/trigger-events/active`

**Response:** Array of all active trigger events across all zones

---

## 5. Wallet API (`/api/wallet`)

### Get Wallet Balance
**GET** `/api/wallet/{workerId}`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "workerId": "worker-uuid-string",
  "balance": 850.75,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T16:45:00"
}
```

### Get Wallet Transactions
**GET** `/api/wallet/{workerId}/transactions`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
[
  {
    "transactionId": "txn-uuid-string",
    "workerId": "worker-uuid-string",
    "type": "claim_payout",
    "amount": 150.00,
    "description": "Rainfall disruption claim",
    "createdAt": "2024-01-15T15:45:00"
  }
]
```

---

## 6. Admin API (`/api/admin`)

### Admin Login
**POST** `/api/admin/login`

**Request Body:**
```json
{
  "username": "admin",
  "password": "gigshield@admin2026"
}
```

**Response:**
```json
{
  "token": "admin-jwt-token",
  "username": "admin",
  "role": "admin",
  "message": "Welcome, admin"
}
```

### Get Dashboard Statistics
**GET** `/api/admin/stats`

**Response:**
```json
{
  "totalWorkers": 150,
  "activeWorkers": 120,
  "totalClaims": 450,
  "approvedClaims": 380,
  "flaggedClaims": 15,
  "claimsToday": 8,
  "activePolicies": 95,
  "activeTriggers": 3,
  "totalPaidOut": 12500.00,
  "approvalRate": 84.4
}
```

### Get Flagged Claims
**GET** `/api/admin/claims/flagged`

**Response:**
```json
[
  {
    "claimId": "claim-uuid-string",
    "workerId": "worker-uuid-string",
    "workerName": "John Doe",
    "workerPhone": "9876543210",
    "triggerType": "rainfall",
    "zoneId": "zone-001",
    "payoutAmount": 200.00,
    "fraudScore": 0.85,
    "triggeredAt": "2024-01-15T14:30:00",
    "processedAt": "2024-01-15T15:45:00"
  }
]
```

### Approve Flagged Claim
**POST** `/api/admin/claims/{claimId}/approve`

**Response:**
```json
{
  "message": "Claim claim-uuid-string approved successfully",
  "claimId": "claim-uuid-string"
}
```

### Reject Claim
**POST** `/api/admin/claims/{claimId}/reject`

**Response:**
```json
{
  "message": "Claim claim-uuid-string rejected",
  "claimId": "claim-uuid-string"
}
```

### Get All Claims
**GET** `/api/admin/claims/all`

**Response:** Array of all claims (same structure as flagged claims)

### Get Worker Statistics
**GET** `/api/admin/workers`

**Response:**
```json
{
  "total": 150,
  "active": 120,
  "inactive": 30,
  "byPersona": {
    "delivery": 60,
    "construction": 40,
    "agriculture": 30,
    "other": 20
  },
  "byZone": {
    "zone-001": 45,
    "zone-002": 38,
    "zone-003": 37
  }
}
```

### Get Policy Statistics
**GET** `/api/admin/policies`

**Response:**
```json
{
  "active": 95,
  "expired": 45,
  "exhausted": 10,
  "byTier": {
    "basic": 40,
    "standard": 35,
    "premium": 20
  },
  "totalPremium": 4250.00
}
```

---

## Data Models & Validation

### Common Fields
- **UUIDs**: Most IDs are UUID strings
- **Timestamps**: ISO 8601 format (YYYY-MM-DDTHH:mm:ss)
- **Amounts**: BigDecimal with 2 decimal places
- **Phone**: 10-digit Indian mobile numbers (6-9 prefix)

### Status Values
- **Claim Status**: `pending`, `approved`, `rejected`, `flagged`
- **Policy Status**: `active`, `expired`, `exhausted`
- **Trigger Status**: `active`, `resolved`
- **Worker Status**: `true` (active), `false` (inactive)

### Trigger Types
- `rainfall` - Rain-based disruptions
- `temperature` - Temperature-based disruptions
- `air_quality` - Air quality disruptions
- `other` - Other disruption types

### Policy Tiers
- `basic` - Basic coverage, lower premium
- `standard` - Standard coverage, moderate premium
- `premium` - Premium coverage, higher premium

### Worker Personas
- `delivery` - Food/gig delivery workers
- `construction` - Construction workers
- `agriculture` - Agricultural workers
- `other` - Other gig worker types

---

## Error Handling

### HTTP Status Codes
- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Validation errors or invalid input
- `401 Unauthorized` - Authentication required or invalid credentials
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

### Error Response Format
```json
{
  "error": "Error message description",
  "timestamp": "2024-01-15T10:30:00",
  "path": "/api/endpoint"
}
```

### Common Validation Errors
- Missing required fields
- Invalid phone number format
- Invalid email format
- Out-of-range values
- Invalid enum values

---

## External Services Integration

### Trigger Engine (Flask)
- **URL:** `http://localhost:5001`
- **Purpose:** Processes real-time disruption events
- **Integration:** Creates trigger events via `/api/trigger-events`

### ML Premium API
- **URL:** `http://localhost:5003`
- **Purpose:** Calculates dynamic premiums based on risk factors
- **Integration:** Used during policy creation

---

## Mobile Development Notes

### Authentication Flow
1. Register/login via Workers API
2. Store JWT token securely on device
3. Include token in Authorization header for all protected endpoints
4. Handle token expiry (24 hours) - re-login required

### Real-time Updates
- Poll `/api/trigger-events/active/{zoneId}` for active disruptions
- Use `/api/claims/event/{eventId}` to check claim processing status
- Update wallet balance after claim approvals

### Offline Considerations
- Store essential data locally (worker profile, current policy)
- Queue actions when offline (profile updates)
- Sync when connection restored

### Security Best Practices
- Never store passwords in plain text
- Use HTTPS in production
- Implement proper token storage (Keychain/Keystore)
- Validate all user inputs before sending to API

### Performance Optimization
- Use pagination for large datasets
- Cache frequently accessed data (profile, current policy)
- Implement background sync for transactions
- Optimize image uploads if any

---

## Testing & Development

### Health Check Endpoints
All services have health check endpoints:
- `/api/workers/health`
- `/api/policies/health`
- `/api/claims/health`
- `/api/trigger-events/health`
- `/api/wallet/health`
- `/api/admin/health`

**Response:** `{"status": "ok", "service": "service-name"}`

### Sample Test Data
Use the provided admin credentials to access admin endpoints and view sample data. The system comes with pre-populated test data for development.

### Database Schema
The backend uses PostgreSQL with the following main tables:
- workers
- policies
- claims
- trigger_events
- wallets
- wallet_transactions
- claim_processing_logs

---

## Support & Contact

For any API-related issues or questions during mobile development:
1. Check the health endpoints first
2. Review error messages carefully
3. Verify request formats against this documentation
4. Ensure proper authentication headers are included

**Last Updated:** January 2024
**Version:** 1.0
