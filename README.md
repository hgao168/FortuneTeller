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
