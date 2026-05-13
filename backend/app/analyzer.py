"""Palm image analyzer.

Mirrors the RunForm `analyzer.py` pattern: a single async entrypoint that
takes binary input and returns a strongly typed response. Uses the OpenAI
multimodal API when `OPENAI_API_KEY` is configured, otherwise returns a
deterministic fallback so the app remains functional offline.
"""
from __future__ import annotations

import base64
import io
import json
import os
from typing import List

from PIL import Image

from .schemas import PalmReadingResponse, PalmReadingSection, ReadingScope


_DISCLAIMER = (
    "This reading is for entertainment purposes only. It is not medical, "
    "financial, psychological, or legal advice."
)


def _validate_image_bytes(image_bytes: bytes) -> None:
    if not image_bytes:
        raise ValueError("Empty image payload")
    try:
        with Image.open(io.BytesIO(image_bytes)) as img:
            img.verify()
    except Exception as exc:  # noqa: BLE001 - validation boundary
        raise ValueError(f"Invalid image: {exc}") from exc


def _scope_prompt(scope: ReadingScope) -> str:
    if scope == "year":
        return (
            "Provide a focused palm reading for the upcoming 12 months. "
            "Be specific about near-term themes."
        )
    return (
        "Provide a long-term palm reading covering life themes over many years. "
        "Focus on enduring patterns rather than short-term events."
    )


def _build_user_prompt(scope: ReadingScope) -> str:
    return (
        "You are a friendly palmistry guide. Analyze the provided palm image. "
        + _scope_prompt(scope)
        + " Respond ONLY as compact JSON with this exact schema: "
        + '{"summary": str, "sections": [{"title": str, "text": str}], '
        + '"advice": [str]}. '
        + "Include 4 sections titled Career, Relationships, Health, Wealth. "
        + "Keep each section under 80 words. Provide 3 short advice bullets. "
        + "Never claim medical, financial, or legal certainty."
    )


def _fallback_response(scope: ReadingScope) -> PalmReadingResponse:
    horizon = "the year ahead" if scope == "year" else "the long arc of life"
    sections: List[PalmReadingSection] = [
        PalmReadingSection(
            title="Career",
            text=(
                f"For {horizon}, your palm suggests steady growth driven by "
                "curiosity. Opportunities favor those who keep learning."
            ),
        ),
        PalmReadingSection(
            title="Relationships",
            text=(
                "Connections deepen when you communicate openly. Watch for a "
                "renewed bond with someone you already know."
            ),
        ),
        PalmReadingSection(
            title="Health",
            text=(
                "Balance is the theme: small daily routines outperform "
                "intense bursts. Sleep and hydration are your edge."
            ),
        ),
        PalmReadingSection(
            title="Wealth",
            text=(
                "Patience compounds. Avoid impulsive bets; favor consistent "
                "savings and one well-researched move."
            ),
        ),
    ]
    return PalmReadingResponse(
        scope=scope,
        summary=(
            "A grounded period with quiet momentum. Your palm hints at "
            "deliberate progress over flashy wins."
        ),
        sections=sections,
        advice=[
            "Pick one habit to keep for 90 days.",
            "Say yes to a conversation you have been postponing.",
            "Track one small financial routine weekly.",
        ],
        disclaimer=_DISCLAIMER,
    )


def _parse_model_json(content: str, scope: ReadingScope) -> PalmReadingResponse:
    """Best-effort JSON parse with safe fallback."""
    try:
        # Some models wrap JSON in code fences.
        cleaned = content.strip()
        if cleaned.startswith("```"):
            cleaned = cleaned.strip("`")
            if cleaned.lower().startswith("json"):
                cleaned = cleaned[4:].lstrip()
        data = json.loads(cleaned)
        sections = [
            PalmReadingSection(title=s.get("title", ""), text=s.get("text", ""))
            for s in data.get("sections", [])
            if isinstance(s, dict)
        ]
        advice = [str(a) for a in data.get("advice", []) if isinstance(a, str)]
        return PalmReadingResponse(
            scope=scope,
            summary=str(data.get("summary", "")).strip() or "Reading completed.",
            sections=sections or _fallback_response(scope).sections,
            advice=advice or _fallback_response(scope).advice,
            disclaimer=_DISCLAIMER,
        )
    except Exception:  # noqa: BLE001 - parser must not crash request
        return _fallback_response(scope)


async def analyze_palm_image(image_bytes: bytes, scope: ReadingScope) -> PalmReadingResponse:
    _validate_image_bytes(image_bytes)

    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        return _fallback_response(scope)

    try:
        from openai import AsyncOpenAI

        client = AsyncOpenAI(api_key=api_key)
        b64 = base64.b64encode(image_bytes).decode("ascii")
        prompt = _build_user_prompt(scope)

        result = await client.chat.completions.create(
            model=os.getenv("FORTUNE_MODEL", "gpt-4o-mini"),
            messages=[
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/jpeg;base64,{b64}",
                            },
                        },
                    ],
                }
            ],
            temperature=0.8,
        )
        content = result.choices[0].message.content or ""
        return _parse_model_json(content, scope)
    except Exception:  # noqa: BLE001 - never fail the request on provider errors
        return _fallback_response(scope)
