# FortuneTeller Backend

FastAPI service mirroring RunForm's analyzer pattern.

## Run locally

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:OPENAI_API_KEY = "sk-..."  # optional; falls back to deterministic reading if unset
uvicorn app.main:app --reload --port 8000
```

Test:

```powershell
curl -F "image=@palm.jpg" -F "scope=year" http://localhost:8000/analyze-palm
```

## Env vars

- `OPENAI_API_KEY` (optional) — enable real multimodal analysis
- `FORTUNE_MODEL` (optional) — defaults to `gpt-4o-mini`
- `ENVIRONMENT` (optional)
