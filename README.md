# FortuneTeller

AI palm reading app. Users can:

- Use their live camera to capture their palm and receive a fortune reading
- Upload an existing palm photo for analysis
- Choose reading scope: **This Year** or **Long Term**

The architecture mirrors the RunForm project:

- `backend/` — FastAPI service, single `/analyze-palm` endpoint, OpenAI-powered interpretation
- `ios/FortuneTeller/` — SwiftUI app, live camera + photo upload, results view

## Quick Start

### Backend

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:OPENAI_API_KEY = "sk-..."
uvicorn app.main:app --reload --port 8000
```

### iOS

Open `ios/FortuneTeller/FortuneTeller.xcodeproj` (after generating via XcodeGen or by adding files in Xcode), set `BACKEND_BASE_URL` in Info.plist, then build & run.

## TestFlight From Windows (GitHub Actions)

If you develop on Windows, use the workflow `.github/workflows/ios-testflight.yml` to build and upload on GitHub's macOS runner.

### 1) One-time Apple setup

- Apple Developer membership is active.
- App ID exists for bundle id `ai.movenova.fortuneteller`.
- App Store Connect app exists for the same bundle id.
- Create an App Store provisioning profile named `FortuneTeller-Prod`.
- Prepare/export an Apple Distribution certificate as `.p12` (plus password).
- Create an App Store Connect API key (`.p8`, key id, issuer id).

### 2) GitHub Secrets required

Set these repository secrets exactly:

- `KEYCHAIN_PASSWORD`
- `ios_distribution_p12_base64`
- `IOS_CERTIFICATE_PASSWORD`
- `IOS_PROVISION_PROFILE`
- `APPLE_TEAM_ID`
- `APP_STORE_CONNECT_API_KEY`
- `APP_STORE_CONNECT_KEY_ID`
- `APP_STORE_CONNECT_ISSUER_ID`

### 3) Generate base64 values in PowerShell (Windows)

```powershell
# .p12 -> ios_distribution_p12_base64
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\distribution.p12"))

# .mobileprovision -> IOS_PROVISION_PROFILE
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\FortuneTeller-Prod.mobileprovision"))

# .p8 -> APP_STORE_CONNECT_API_KEY
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\AuthKey_XXXXXX.p8"))
```

### 4) Run upload

- In GitHub: Actions -> `iOS Production → TestFlight (Manual)` -> `Run workflow`.
- The workflow uses `github.run_number` as `CURRENT_PROJECT_VERSION` to keep each uploaded build number unique.

## API

### POST /analyze-palm

Multipart form:

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `image` | file | yes | JPEG/PNG of a single palm |
| `scope` | string | yes | `year` or `long_term` |
| `user_id` | string | no | Optional client-generated id |

Response:

```json
{
  "scope": "year",
  "summary": "string",
  "sections": [
    { "title": "Career", "text": "..." },
    { "title": "Relationships", "text": "..." },
    { "title": "Health", "text": "..." },
    { "title": "Wealth", "text": "..." }
  ],
  "advice": ["...", "..."],
  "disclaimer": "Entertainment only."
}
```

## Disclaimer

This app is for entertainment purposes only and does not provide medical, financial, or legal advice.
