import os
from typing import Optional

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware

from .analyzer import analyze_palm_image, analyze_palm_match
from .schemas import MatchType, PalmMatchResponse, PalmReadingResponse, ReadingScope


ENVIRONMENT = os.getenv("ENVIRONMENT", "production")
ALLOWED_SCOPES = {"today", "month", "year", "long_term"}
ALLOWED_MATCH_TYPES = {"romantic", "friend"}
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


@app.post("/match-palm", response_model=PalmMatchResponse)
async def match_palm(
    image_a: UploadFile = File(...),
    image_b: UploadFile = File(...),
    match_type: str = Form(...),
    person_a_birth: str = Form(...),
    person_b_birth: str = Form(...),
) -> PalmMatchResponse:
    if match_type not in ALLOWED_MATCH_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid match_type '{match_type}'. Must be one of: {sorted(ALLOWED_MATCH_TYPES)}",
        )

    if not person_a_birth.strip() or not person_b_birth.strip():
        raise HTTPException(status_code=400, detail="Both person birth datetime values are required")

    for image in (image_a, image_b):
        if image.content_type and not image.content_type.startswith("image/"):
            raise HTTPException(status_code=400, detail="Uploaded files must be images")

    payload_a = await image_a.read()
    payload_b = await image_b.read()

    if len(payload_a) > MAX_IMAGE_BYTES or len(payload_b) > MAX_IMAGE_BYTES:
        raise HTTPException(status_code=413, detail="Image too large (max 8 MB each)")

    try:
        return await analyze_palm_match(
            payload_a,
            payload_b,
            match_type,  # type: ignore[arg-type]
            person_a_birth,
            person_b_birth,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail="Match analysis failed. Please try again.") from exc
