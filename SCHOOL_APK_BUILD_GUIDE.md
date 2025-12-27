# School APK Build Guide

## Overview

This guide explains how to build custom APKs for individual schools using the Zii Schools system.

## Architecture

```
zii-website (Admin)          zii-school-app-NEW (APK)
     │                              │
     ├─ Create School               │
     ├─ Add Grades                  │
     ├─ Upload Teachers             │
     ├─ Upload Students             │
     ├─ Upload Parents              │
     ├─ Set Timetable               │
     │                              │
     └─ Export Config ──────────────┼─> school_config.json
                                    │   (embedded in APK)
                                    │
                                    └─> Build APK
                                         └─> zii-school-ABC123-debug.apk
```

## Data Flow

### 1. Admin Website (zii-website)

**Database Tables:**
- `schools` - School information
- `grades` - Grade levels with encryption keys
- `teachers` - Teacher accounts
- `students` - Student accounts
- `parents` - Parent accounts
- `parent_students` - Parent-child relationships
- `timetables` - School schedules

**Export API:**
- `GET /api/admin/schools/[id]/export-config`
- Returns JSON matching APK's `school_config.json` format

### 2. APK Build Process

**Input:**
- School ID from admin website
- Website URL (default: localhost:3000)

**Steps:**
1. Download school config from website API
2. Save to `app/src/main/assets/school_config.json`
3. Update app version (increment versionCode, set versionName)
4. Build APK with Gradle
5. Rename APK with school code

**Output:**
- `zii-school-ABC123-debug.apk` (ready to distribute)

### 3. APK Runtime

**On App Launch:**
1. SchoolConfigManager loads embedded `school_config.json`
2. LoginScreen shows school name and code
3. User selects role and enters credential
4. SchoolConfigManager validates against embedded data
5. SchoolSession saves login state
6. SchoolHomeScreen shows role-specific dashboard

## Build Instructions

### Prerequisites

1. **Admin Website Running:**
   ```bash
   cd zii-website
   npm install
   npm run dev
   # Website at http://localhost:3000
   ```

2. **School Created in Admin:**
   - Navigate to http://localhost:3000/admin/schools
   - Create school
   - Add grades, teachers, students, parents
   - Set timetable

3. **Android Build Tools:**
   - JDK 17+
   - Android SDK
   - Gradle (included in project)

### Build Command

```powershell
cd zii-school-app-NEW
.\build-for-school.ps1 <school_id>
```

**Example:**
```powershell
.\build-for-school.ps1 demo-school-001
```

**With Custom Website URL:**
```powershell
.\build-for-school.ps1 demo-school-001 -WebsiteUrl "https://zii.school"
```

### Build Output

```
========================================
Zii School APK Builder
========================================

[1/4] Downloading school configuration...
  ✓ School: ABC Primary School
  ✓ Code: ABC123
  ✓ Grades: 2
  ✓ Teachers: 3
  ✓ Students: 5
  ✓ Parents: 4
  ✓ Config saved to app\src\main\assets\school_config.json

[2/4] Updating app version...
  ✓ Version code: 19 → 20
  ✓ Version name: 0.4.0-ABC123

[3/4] Building APK...
  This may take a few minutes...
  ✓ Build successful!

[4/4] Finalizing APK...
  ✓ APK created: zii-school-ABC123-debug.apk
  ✓ Size: 45.2 MB

========================================
✓ BUILD COMPLETE!
========================================

School: ABC Primary School
Code: ABC123
APK: zii-school-ABC123-debug.apk
```

## school_config.json Format

```json
{
  "school": {
    "id": "uuid",
    "code": "ABC123",
    "name": "ABC Primary School",
    "contactName": "John Smith",
    "contactPhone": "0821234567",
    "contactEmail": "admin@abc.school",
    "address": "123 School St, Johannesburg",
    "logoUrl": null,
    "primaryColor": "#6B46C1"
  },
  "grades": [
    {
      "id": "uuid",
      "name": "Grade 1",
      "encryptionKey": "base64-encoded-key"
    }
  ],
  "teachers": [
    {
      "id": "uuid",
      "name": "Mrs. Sarah Johnson",
      "phone": "0821111111",
      "email": "sarah@abc.school",
      "gradeId": "uuid",
      "role": "teacher"
    }
  ],
  "students": [
    {
      "id": "uuid",
      "name": "Thabo Mbeki",
      "idNumber": "ABC00001",
      "gradeId": "uuid"
    }
  ],
  "parents": [
    {
      "id": "uuid",
      "name": "Mr. John Mbeki",
      "phone": "0824444444",
      "email": "john.mbeki@email.com",
      "studentIds": ["uuid"]
    }
  ],
  "timetable": {
    "schoolStart": "08:00",
    "schoolEnd": "14:00",
    "pickupStart": "14:00",
    "pickupEnd": "15:00",
    "breaks": [
      {
        "name": "Tea Break",
        "start": "10:00",
        "end": "10:15"
      }
    ]
  }
}
```

## Distribution

### Option 1: Direct Install (Testing)

```bash
# Install on connected device
adb install zii-school-ABC123-debug.apk

# Or drag-and-drop to device
```

### Option 2: Upload to Admin Portal

1. Upload APK to school's admin page
2. Generate download link
3. Share link with teachers/parents
4. Users download and install

### Option 3: Google Play (Production)

1. Sign APK with release keystore
2. Upload to Google Play Console
3. Create private track for school
4. Invite users via email

## Testing

### Test Login Credentials

Use the sample data from `school_config.json`:

**Teachers:**
- Phone: `0821111111` (Mrs. Sarah Johnson)
- Phone: `0822222222` (Mr. David Brown)
- Phone: `0823333333` (Ms. Emily White)

**Parents:**
- Phone: `0824444444` (Mr. John Mbeki)
- Phone: `0827777777` (Mrs. Zanele Zulu - 2 children)

**Students:**
- ID: `ABC00001` (Thabo Mbeki)
- ID: `ABC00002` (Lerato Dlamini)
- ID: `ABC00004` (Nomsa Zulu)

### Test Flow

1. **Install APK**
   ```bash
   adb install zii-school-ABC123-debug.apk
   ```

2. **Launch App**
   - Should show login screen
   - School name: "ABC Primary School"
   - School code: "ABC123"

3. **Test Student Login**
   - Select role: "Student"
   - Enter ID: "ABC00001"
   - Click "Login"
   - Should show student dashboard

4. **Test Parent Login**
   - Select role: "Parent"
   - Enter phone: "0824444444"
   - Click "Login"
   - Should show parent dashboard with child info

5. **Test Teacher Login**
   - Select role: "Teacher"
   - Enter phone: "0821111111"
   - Click "Login"
   - Should show teacher dashboard

6. **Test Logout**
   - Click logout button
   - Should return to login screen

7. **Test Session Persistence**
   - Close app
   - Reopen app
   - Should auto-login (skip login screen)

## Troubleshooting

### Build Fails

**Error: "Could not download config"**
- Check website is running: http://localhost:3000
- Check school ID is correct
- Check API endpoint exists: `/api/admin/schools/[id]/export-config`

**Error: "Build failed with exit code 1"**
- Run `.\gradlew.bat clean` first
- Check JDK version: `java -version` (need 17+)
- Check Android SDK is installed

**Error: "APK not found"**
- Check build output for errors
- Look in `app/build/outputs/apk/debug/`

### Login Fails

**Error: "Invalid phone number or student ID"**
- Check credential exists in `school_config.json`
- Check role matches credential type
- Student IDs are case-sensitive

**Error: "This credential belongs to a teacher, not a student"**
- Selected wrong role in dropdown
- Select correct role for credential

### Config Issues

**Error: "Config file not found"**
- Check `app/src/main/assets/school_config.json` exists
- Rebuild APK with `.\build-for-school.ps1`

**Error: "Invalid school configuration format"**
- Check JSON is valid
- Check all required fields present
- Check encryption keys are base64

## Production Checklist

### Before Building

- [ ] All grades created
- [ ] All teachers uploaded
- [ ] All students uploaded
- [ ] All parents uploaded
- [ ] Timetable configured
- [ ] School info complete (name, code, contact)
- [ ] Encryption keys generated for all grades

### After Building

- [ ] Test APK on physical device
- [ ] Test all login types (teacher, parent, student)
- [ ] Test logout and re-login
- [ ] Test session persistence
- [ ] Verify school branding (name, code, colors)
- [ ] Check APK size (should be ~45MB)

### Before Distribution

- [ ] Sign APK with release keystore (production)
- [ ] Test on multiple devices
- [ ] Prepare installation instructions
- [ ] Create support contact info
- [ ] Set up feedback channel

## Next Steps

### Phase 3: Chat Features

Once login is working, implement:

1. **Class Chat Channels**
   - Teacher ↔ Parent messaging
   - Grade-level channels
   - Encryption per grade

2. **Timetable Enforcement**
   - Auto-disable student chat during class
   - Auto-enable during breaks
   - Pickup window activation

3. **Roll Call System**
   - Teacher sends roll call
   - Student responds
   - Parent notification

4. **Emergency Alerts**
   - Staff-only trigger
   - Broadcast to all parents
   - Override DND

### Phase 4: Mesh Integration

Connect to stable BLE mesh:

1. Use grade encryption keys
2. Create logical channels per grade
3. Implement proximity-based access
4. Test with 100+ nodes

---

**Status**: Phase 2 Complete (Login System)
**Next**: Phase 3 (Chat Features)
**Build Script**: `build-for-school.ps1`
**Export API**: `/api/admin/schools/[id]/export-config`
