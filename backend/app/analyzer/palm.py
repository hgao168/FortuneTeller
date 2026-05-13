"""Single-image palm reading flow."""
from __future__ import annotations

import base64
import os
from typing import List

from ..schemas import PalmReadingResponse, PalmReadingSection, ReadingScope
from .core import (
    DISCLAIMER,
    call_openai_vision,
    parse_json_payload,
    validate_image_bytes,
)


def _scope_prompt(scope: ReadingScope) -> str:
    if scope == "today":
        return (
            "Provide a palm reading focused on today only — the next 24 hours. "
            "Be specific about immediate energy, micro-opportunities, and things to watch out for right now."
        )
    if scope == "month":
        return (
            "Provide a palm reading focused on the current month — the next 30 days. "
            "Highlight themes, turning points, and key dates to pay attention to."
        )
    if scope == "year":
        return (
            "Provide a focused palm reading for the upcoming 12 months. "
            "Be specific about near-term themes."
        )
    return (
        "Provide a long-term palm reading covering life themes over many years. "
        "Focus on enduring patterns rather than short-term events."
    )


def _build_prompt(scope: ReadingScope) -> str:
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


def _fallback(scope: ReadingScope) -> PalmReadingResponse:
    horizon_map = {
        "today": "today",
        "month": "this month",
        "year": "the year ahead",
        "long_term": "the long arc of life",
    }
    horizon = horizon_map.get(scope, "the year ahead")
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
        disclaimer=DISCLAIMER,
    )


def _parse(content: str, scope: ReadingScope) -> PalmReadingResponse:
    data = parse_json_payload(content)
    if not data:
        return _fallback(scope)
    fallback = _fallback(scope)
    sections = [
        PalmReadingSection(title=s.get("title", ""), text=s.get("text", ""))
        for s in data.get("sections", [])
        if isinstance(s, dict)
    ]
    advice = [str(a) for a in data.get("advice", []) if isinstance(a, str)]
    return PalmReadingResponse(
        scope=scope,
        summary=str(data.get("summary", "")).strip() or "Reading completed.",
        sections=sections or fallback.sections,
        advice=advice or fallback.advice,
        disclaimer=DISCLAIMER,
    )


async def analyze_palm_image(image_bytes: bytes, scope: ReadingScope) -> PalmReadingResponse:
    validate_image_bytes(image_bytes)

    if not os.getenv("OPENAI_API_KEY"):
        return _fallback(scope)

    try:
        b64 = base64.b64encode(image_bytes).decode("ascii")
        content = await call_openai_vision(_build_prompt(scope), [b64])
        return _parse(content, scope)
    except Exception:  # noqa: BLE001 - never fail the request on provider errors
        return _fallback(scope)
