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


def _lang_prompt(language: str | None) -> str:
    normalized = (language or "").lower()
    if normalized.startswith("zh"):
        return "Use Simplified Chinese for all output text."
    return "Use English for all output text."


def _build_prompt(scope: ReadingScope, language: str | None) -> str:
    return (
        "You are a friendly palmistry guide. Analyze the provided palm image. "
        + _scope_prompt(scope)
        + " "
        + _lang_prompt(language)
        + " Respond ONLY as compact JSON with this exact schema: "
        + '{"summary": str, "sections": [{"title": str, "text": str}], '
        + '"advice": [str]}. '
        + "Include 4 sections titled Career, Relationships, Health, Wealth. "
        + "Keep each section under 80 words. Provide 3 short advice bullets. "
        + "Never claim medical, financial, or legal certainty."
    )


def _fallback(scope: ReadingScope, language: str | None) -> PalmReadingResponse:
    is_zh = (language or "").lower().startswith("zh")
    horizon_map = {
        "today": "today",
        "month": "this month",
        "year": "the year ahead",
        "long_term": "the long arc of life",
    }
    horizon = horizon_map.get(scope, "the year ahead")

    if is_zh:
        sections: List[PalmReadingSection] = [
            PalmReadingSection(
                title="事业",
                text="近期事业重心在于稳中求进，持续学习会带来更明确的机会与方向。",
            ),
            PalmReadingSection(
                title="感情",
                text="坦诚表达会让关系更有安全感，旧识中可能出现值得重新连接的人。",
            ),
            PalmReadingSection(
                title="健康",
                text="节奏感是关键，规律作息与补水会比短期高强度更有效。",
            ),
            PalmReadingSection(
                title="财富",
                text="财运偏向长期积累，避免冲动决策，重视稳健与计划性。",
            ),
        ]
        return PalmReadingResponse(
            scope=scope,
            summary="整体运势偏稳，属于厚积薄发阶段，持续行动会逐步显现回报。",
            sections=sections,
            advice=[
                "选一个可以坚持 90 天的小习惯。",
                "把一场拖延已久的沟通尽快完成。",
                "每周固定记录一次收支与计划。",
            ],
            disclaimer="本解读仅供娱乐参考，不构成医疗、财务、心理或法律建议。",
        )

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


def _parse(content: str, scope: ReadingScope, language: str | None) -> PalmReadingResponse:
    data = parse_json_payload(content)
    if not data:
        return _fallback(scope, language)
    fallback = _fallback(scope, language)
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
        disclaimer=fallback.disclaimer,
    )


async def analyze_palm_image(
    image_bytes: bytes,
    scope: ReadingScope,
    language: str | None = None,
) -> PalmReadingResponse:
    validate_image_bytes(image_bytes)

    if not os.getenv("OPENAI_API_KEY"):
        return _fallback(scope, language)

    try:
        b64 = base64.b64encode(image_bytes).decode("ascii")
        content = await call_openai_vision(_build_prompt(scope, language), [b64])
        return _parse(content, scope, language)
    except Exception:  # noqa: BLE001 - never fail the request on provider errors
        return _fallback(scope, language)
