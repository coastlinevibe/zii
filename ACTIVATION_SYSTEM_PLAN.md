# Zii Chat Activation System - Complete Implementation Plan

## Overview
Monetization system using activation codes with offline entry and online validation.

## Pricing Tiers
- **R5** = 5 days (entry tier)
- **R15** = 30 days (monthly)
- **R50** = 90 days (quarterly)
- **R150** = 365 days (yearly)

**NO FREE TIER** - All access requires activation code

## Distribution Channels
1. **Website** - R15+ codes (FastPay payment gateway)
2. **WhatsApp Support** - All tiers (EFT, cash, manual processing)
3. **Partner Shops** - Buy codes at discount, sell at full price (upfront payment to us)

## Core Features
- ✅ Offline code entry
- ✅ 1-hour grace period for validation
- ✅ Single-use enforcement (timestamp-based)
- ✅ 12-hour grace period after expiry
- ✅ Expiry notifications (48h, 36h, 24h, 12h, 6h, 1h)
- ✅ Stackable codes (extends from current expiry)

## Technical Architecture

### 1. Code Format
```
XXXX-YYYY-ZZZZ-CCCC
- XXXX: Random prefix (4 chars)
- YYYY: Encoded duration + batch (4 chars)
- ZZZZ: Unique code ID (4 chars)
- CCCC: Checksum (4 chars)
```

### 2. Activation States
```kotlin
enum class ActivationState {
    NONE,       // No code entered
    PENDING,    // Code entered, awaiting validation (1 hour)
    VALIDATED,  // Code validated online, full access
    EXPIRED,    // Subscription expired (12h grace)
    REVOKED     // Code was already used
}
```

### 3. Storage (Encrypted SharedPreferences)
```kotlin
data class ActivationData(
    val code: String,
    val state: ActivationState,
    val entryTimestamp: Long,
    val validationDeadline: Long,
    val permanentToken: String?,
    val expiryDate: Long?,
    val durationDays: Int
)
```


## User Flow

### Scenario 1: Offline Activation
```
1. User purchases code from shop
2. User enters code in app (OFFLINE)
3. App validates format/checksum locally
4. App grants TEMPORARY 1-hour access
5. App shows: "Connect to WiFi within 1 hour"
6. User goes to shop/friend's WiFi
7. App auto-detects internet
8. App validates with server
9. Server checks timestamp + usage
10. If first: Grant permanent access
11. If duplicate: Revoke and show error
```

### Scenario 2: Online Activation
```
1. User enters code (ONLINE)
2. App validates immediately with server
3. Instant activation or rejection
```

### Scenario 3: Race Condition (2 devices, same code)
```
Device A enters at: 10:00:00.123
Device B enters at: 10:00:00.456

Device A validates at 10:30:
✅ First to validate → Gets access

Device B validates at 10:45:
❌ Code already used → Rejected
```

## Server Implementation (Vercel)

### Setup
```
Domain: api.ziichat.com
Endpoint: POST /api/activate
Database: Vercel KV (Redis-like)
Cost: FREE (up to 100k requests/month)
```

### API Endpoint
```javascript
// /api/activate.js
export default async function handler(req, res) {
    const { code, entryTimestamp, deviceId } = req.body;
    
    // Validate code format
    if (!isValidCode(code)) {
        return res.status(400).json({
            success: false,
            error: "Invalid code format"
        });
    }
    
    // Check if code exists and is unused
    const existing = await kv.get(`code:${code}`);
    
    if (existing && existing.used) {
        return res.status(409).json({
            success: false,
            error: "Code already activated"
        });
    }
    
    // Mark code as used
    await kv.set(`code:${code}`, {
        entryTimestamp,
        validatedAt: Date.now(),
        deviceId,
        used: true
    });
    
    // Decode duration from code
    const durationDays = decodeDuration(code);
    const expiryDate = Date.now() + (durationDays * 24 * 60 * 60 * 1000);
    
    // Generate permanent token
    const token = generateToken(code, deviceId);
    
    return res.status(200).json({
        success: true,
        token,
        expiryDate,
        durationDays
    });
}
```


### Database Schema (Vercel KV)
```javascript
// Key: code:XXXX-YYYY-ZZZZ-CCCC
// Value:
{
    entryTimestamp: 1234567890123,
    validatedAt: 1234567890456,
    deviceId: "abc123...",
    used: true,
    durationDays: 30
}
```

## Android Implementation

### 1. Activation Manager
```kotlin
class ActivationManager(context: Context) {
    
    fun enterCode(code: String): Result {
        // Validate format
        if (!validateCodeFormat(code)) {
            return Result.Error("Invalid code format")
        }
        
        // Store pending activation
        val entryTimestamp = System.currentTimeMillis()
        storage.save(
            code = code,
            state = PENDING,
            entryTimestamp = entryTimestamp,
            validationDeadline = entryTimestamp + HOUR_IN_MILLIS
        )
        
        // Try immediate validation if online
        if (isOnline()) {
            return validateOnline(code, entryTimestamp)
        }
        
        return Result.Pending("Connect to WiFi within 1 hour")
    }
    
    fun validateOnline(code: String, entryTimestamp: Long): Result {
        val response = api.activate(code, entryTimestamp, deviceId)
        
        return when (response.status) {
            200 -> {
                storage.save(
                    state = VALIDATED,
                    permanentToken = response.token,
                    expiryDate = response.expiryDate
                )
                scheduleExpiryNotifications(response.expiryDate)
                Result.Success
            }
            409 -> {
                storage.save(state = REVOKED)
                Result.Error("Code already used")
            }
            else -> Result.Error("Validation failed")
        }
    }
    
    fun checkActivationStatus(): ActivationStatus {
        val data = storage.load()
        val now = System.currentTimeMillis()
        
        return when (data.state) {
            NONE -> ActivationStatus.NotActivated
            PENDING -> {
                if (now > data.validationDeadline) {
                    ActivationStatus.ValidationExpired
                } else {
                    ActivationStatus.PendingValidation(
                        timeRemaining = data.validationDeadline - now
                    )
                }
            }
            VALIDATED -> {
                if (now > data.expiryDate!!) {
                    if (now < data.expiryDate + GRACE_PERIOD) {
                        ActivationStatus.GracePeriod(
                            timeRemaining = data.expiryDate + GRACE_PERIOD - now
                        )
                    } else {
                        ActivationStatus.Expired
                    }
                } else {
                    ActivationStatus.Active(
                        expiryDate = data.expiryDate
                    )
                }
            }
            REVOKED -> ActivationStatus.Revoked
            EXPIRED -> ActivationStatus.Expired
        }
    }
}
```


### 2. Background Validation Worker
```kotlin
class ValidationWorker : Worker() {
    override fun doWork(): Result {
        val manager = ActivationManager(context)
        val data = storage.load()
        
        if (data.state == PENDING && isOnline()) {
            manager.validateOnline(data.code, data.entryTimestamp)
        }
        
        return Result.success()
    }
}

// Schedule periodic validation attempts
WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "activation_validation",
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<ValidationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
    )
```

### 3. Notification Scheduler
```kotlin
fun scheduleExpiryNotifications(expiryDate: Long) {
    val notifications = listOf(
        expiryDate - 48.hours to "2 days until expiry",
        expiryDate - 36.hours to "36 hours until expiry",
        expiryDate - 24.hours to "1 day until expiry",
        expiryDate - 12.hours to "12 hours until expiry",
        expiryDate - 6.hours to "6 hours until expiry",
        expiryDate - 1.hours to "1 hour until expiry",
        expiryDate to "Subscription expired"
    )
    
    notifications.forEach { (time, message) ->
        if (time > System.currentTimeMillis()) {
            scheduleNotification(time, message)
        }
    }
}
```

### 4. UI Screens

#### Activation Screen
```kotlin
@Composable
fun ActivationScreen(viewModel: ActivationViewModel) {
    var code by remember { mutableStateOf("") }
    val status by viewModel.status.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Enter Activation Code", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase() },
            label = { Text("XXXX-XXXX-XXXX-XXXX") },
            visualTransformation = CodeVisualTransformation()
        )
        
        Spacer(Modifier.height(16.dp))
        
        Button(onClick = { viewModel.activateCode(code) }) {
            Text("Activate")
        }
        
        when (status) {
            is Pending -> PendingValidationCard(status.timeRemaining)
            is Active -> ActiveCard(status.expiryDate)
            is GracePeriod -> GracePeriodCard(status.timeRemaining)
            is Error -> ErrorCard(status.message)
        }
    }
}
```


## Code Generation Tool

### Generator Script (Node.js)
```javascript
const crypto = require('crypto');

function generateCode(durationDays, batchId) {
    // Random prefix (4 chars)
    const prefix = crypto.randomBytes(2).toString('hex').toUpperCase();
    
    // Encode duration + batch (4 chars)
    const encoded = encodeDuration(durationDays, batchId);
    
    // Unique ID (4 chars)
    const uniqueId = crypto.randomBytes(2).toString('hex').toUpperCase();
    
    // Calculate checksum (4 chars)
    const checksum = calculateChecksum(prefix + encoded + uniqueId);
    
    return `${prefix}-${encoded}-${uniqueId}-${checksum}`;
}

function encodeDuration(days, batchId) {
    // Encode days in first 2 chars, batch in last 2
    const daysHex = days.toString(16).padStart(2, '0').toUpperCase();
    const batchHex = batchId.toString(16).padStart(2, '0').toUpperCase();
    return daysHex + batchHex;
}

function calculateChecksum(data) {
    return crypto.createHash('sha256')
        .update(data + SECRET_KEY)
        .digest('hex')
        .substring(0, 4)
        .toUpperCase();
}

// Generate batch
function generateBatch(count, durationDays, batchId) {
    const codes = [];
    for (let i = 0; i < count; i++) {
        codes.push(generateCode(durationDays, batchId));
    }
    return codes;
}

// Example usage
const codes = generateBatch(100, 30, 1); // 100 codes for 30 days, batch 1
console.log(codes);
```

### Batch Management
```javascript
// Store generated codes in Vercel KV
async function storeBatch(codes, durationDays, batchId) {
    for (const code of codes) {
        await kv.set(`code:${code}`, {
            durationDays,
            batchId,
            generated: Date.now(),
            used: false
        });
    }
}
```

## Security Considerations

### 1. Code Validation
- ✅ Checksum prevents fake codes
- ✅ Server-side validation prevents tampering
- ✅ Timestamp prevents replay attacks

### 2. Storage Security
- ✅ Encrypted SharedPreferences for local data
- ✅ HTTPS for all API calls
- ✅ Token-based authentication after activation

### 3. Anti-Abuse
- ✅ Rate limiting on API (10 attempts per device per hour)
- ✅ Device fingerprinting
- ✅ Batch tracking for fraud detection

## Deployment Checklist

### Phase 1: Development
- [ ] Implement ActivationManager
- [ ] Create Activation UI screens
- [ ] Build Vercel API endpoints
- [ ] Create code generator tool
- [ ] Test offline/online flows

### Phase 2: Testing
- [ ] Test race conditions (2 devices, same code)
- [ ] Test 1-hour validation window
- [ ] Test 12-hour grace period
- [ ] Test notification scheduling
- [ ] Test code stacking

### Phase 3: Production
- [ ] Deploy Vercel API
- [ ] Configure domain (api.ziichat.com)
- [ ] Integrate FastPay for R15+ payments
- [ ] Set up WhatsApp support workflow
- [ ] Create partner onboarding process
- [ ] Generate initial code batches
- [ ] Set up monitoring/analytics
- [ ] Create support documentation

## Partner Program

### Partner Pricing (Wholesale)
- R5 codes: R4 (20% discount)
- R15 codes: R12 (20% discount)
- R50 codes: R40 (20% discount)
- R150 codes: R120 (20% discount)

### Partner Benefits
- Buy codes in bulk upfront
- Sell at full retail price
- Keep 20% profit margin
- No payment processing hassle
- Physical voucher cards available

### Partner Onboarding
1. Partner applies via website/WhatsApp
2. We verify business legitimacy
3. Partner pays upfront for code batch
4. We generate and deliver codes
5. Partner sells codes to customers
6. We provide marketing materials


## Support & Edge Cases

### Common Issues

**Issue 1: "Code already used"**
- Solution: Contact support with code + purchase proof
- Support can check server logs for timestamp
- Can issue replacement code if legitimate

**Issue 2: "Validation expired"**
- Solution: User must connect to WiFi
- Code remains valid until validated
- No time limit on validation attempts

**Issue 3: "Can't connect to validation server"**
- Solution: App retries automatically every 15 minutes
- User can manually retry from activation screen
- Grace period allows continued use during outages

**Issue 4: "Lost access after phone reset"**
- Solution: Re-enter same code
- If already validated: Contact support
- Support can verify purchase and issue new code

### Analytics to Track

1. **Code Usage**
   - Codes generated vs used
   - Average time to validation
   - Failed validation attempts

2. **Revenue**
   - Sales by tier (R5, R15, R50, R150)
   - Sales by channel (website, WhatsApp, shops)
   - Renewal rate

3. **User Behavior**
   - Average subscription duration
   - Churn rate
   - Grace period usage

## Future Enhancements

### Phase 2 Features
- [ ] Family plans (multi-device codes)
- [ ] Referral system (earn free days)
- [ ] Auto-renewal via payment gateway
- [ ] Subscription management dashboard
- [ ] Gift codes

### Phase 3 Features
- [ ] In-app purchases (Google Play)
- [ ] Cryptocurrency payments
- [ ] Partner API for shops
- [ ] Bulk code generation portal

## Cost Estimates

### Infrastructure (Monthly)
- Vercel hosting: FREE (up to 100k requests)
- Domain registration: ~R150/year (~R12/month)
- Vercel KV database: FREE (256MB)
- **Total: ~R12/month**

### Scaling (at 10,000 active users)
- Vercel Pro: $20/month (~R380)
- Vercel KV Pro: $10/month (~R190)
- **Total: ~R570/month**

### Break-even Analysis
- Monthly cost: R570
- Average subscription: R15 (30 days)
- Break-even: 38 subscriptions/month
- **Very achievable!**

## Contact & Support

### For Users
- WhatsApp: [Your number]
- Email: support@ziichat.com
- Website: ziichat.com/support

### For Partners/Shops
- Partner portal: ziichat.com/partners
- Bulk code requests: partners@ziichat.com
- Commission: [Your terms]

---

**Document Version:** 1.0  
**Last Updated:** 2024-11-28  
**Status:** Ready for Implementation
