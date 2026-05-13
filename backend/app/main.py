import os
from typing import Optional

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware

from .analyzer import analyze_palm_image
from .schemas import PalmReadingResponse, ReadingScope


ENVIRONMENT = os.getenv("ENVIRONMENT", "production")
ALLOWED_SCOPES = {"year", "long_term"}
MAX_IMAGE_BYTES = 8 * 1024 * 1024  # 8 MB cap


app = FastAPI(title="FortuneTeller API", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
async def health() -> dict:
    return {"status": "ok", "env": ENVIRONMENT}


@app.post("/analyze-palm", response_model=PalmReadingResponse)
async def analyze_palm(
    image: UploadFile = File(...),
    scope: str = Form(...),
    user_id: Optional[str] = Form(None),
) -> PalmReadingResponse:
    if scope not in ALLOWED_SCOPES:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid scope '{scope}'. Must be one of: {sorted(ALLOWED_SCOPES)}",
        )

    if image.content_type and not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="Uploaded file must be an image")

    payload = await image.read()
    if len(payload) > MAX_IMAGE_BYTES:
        raise HTTPException(status_code=413, detail="Image too large (max 8 MB)")

    try:
        return await analyze_palm_image(payload, scope)  # type: ignore[arg-type]
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail="Analysis failed. Please try again.") from exc
