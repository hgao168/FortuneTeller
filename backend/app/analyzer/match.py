"""Two-image compatibility (match) flow with BaZi birth context."""
from __future__ import annotations

import base64
import os

from ..schemas import MatchType, PalmMatchResponse
from .core import (
    DISCLAIMER,
    call_openai_vision,
    parse_json_payload,
    validate_image_bytes,
)


def _lang_prompt(language: str | None) -> str:
    normalized = (language or "").lower()
    if normalized.startswith("zh"):
        return "Use Simplified Chinese for all output text."
    return "Use English for all output text."


def _build_prompt(
    match_type: MatchType,
    person_a_birth: str,
    person_b_birth: str,
    language: str | None,
) -> str:
    relation = "romantic partners" if match_type == "romantic" else "friends"
    return (
        "You are a friendly palmistry guide. Use BOTH palm images and birth data (BaZi style) to compare compatibility as "
        + relation
        + ". Person A birth datetime: "
        + person_a_birth
        + ". Person B birth datetime: "
        + person_b_birth
        + ". "
        + _lang_prompt(language)
        + ". Respond ONLY as compact JSON with this exact schema: "
        + '{"score": int, "summary": str, "strengths": [str], "tensions": [str], "advice": [str]}. '
        + "Set score between 0 and 100. Give 3 strengths, 3 tensions, and 3 actionable advice bullets. "
        + "Never claim medical, financial, or legal certainty."
    )


def _fallback(match_type: MatchType, language: str | None) -> PalmMatchResponse:
    is_zh = (language or "").lower().startswith("zh")
    if is_zh and match_type == "romantic":
        return PalmMatchResponse(
            match_type=match_type,
            score=78,
            summary="你们的恋爱匹配度温暖且有潜力，彼此节奏具备互补性。",
            strengths=[
                "情绪沟通较自然，容易形成信任感。",
                "能互相推动成长，不会造成过重压力。",
                "日常节奏有机会逐步同步。",
            ],
            tensions=[
                "一方可能更需要情绪确认。",
                "压力下决策速度可能不一致。",
                "个人空间边界需要提前沟通。",
            ],
            advice=[
                "每周建立一次固定的二人仪式感时间。",
                "谈敏感话题前先做简短情绪确认。",
                "本月共同设定一个边界与一个目标。",
            ],
            disclaimer="本解读仅供娱乐参考，不构成医疗、财务、心理或法律建议。",
        )
    if is_zh:
        return PalmMatchResponse(
            match_type=match_type,
            score=84,
            summary="这组友情配对稳定且支持性强，长期来看有持续升温空间。",
            strengths=[
                "务实支持让信任建立更快。",
                "行动力与思考力形成互补。",
                "出现误会后通常能较快修复。",
            ],
            tensions=[
                "社交能量差异可能带来小摩擦。",
                "一方容易付出过多却不表达需求。",
                "若缺少计划，见面频率可能下滑。",
            ],
            advice=[
                "约定固定的联络或见面节奏。",
                "需要帮助时直接表达，不要只暗示。",
                "一起庆祝小进步，增强关系正反馈。",
            ],
            disclaimer="本解读仅供娱乐参考，不构成医疗、财务、心理或法律建议。",
        )

    if match_type == "romantic":
        return PalmMatchResponse(
            match_type=match_type,
            score=78,
            summary="You have a warm and promising romantic dynamic with complementary temperaments.",
            strengths=[
                "Emotional communication feels natural and honest.",
                "You motivate each other's growth without heavy pressure.",
                "Daily habits can align into a stable rhythm.",
            ],
            tensions=[
                "One person may seek reassurance more often.",
                "Decision speed can differ under stress.",
                "Expectations around personal space may need calibration.",
            ],
            advice=[
                "Set one shared weekly ritual that you both enjoy.",
                "Use short check-ins before discussing sensitive topics.",
                "Agree on one boundary and one shared goal this month.",
            ],
            disclaimer=DISCLAIMER,
        )
    return PalmMatchResponse(
        match_type=match_type,
        score=84,
        summary="This friendship pairing is steady, supportive, and likely to strengthen over time.",
        strengths=[
            "Trust builds quickly through practical support.",
            "Your personalities balance action and reflection well.",
            "You recover from misunderstandings without long grudges.",
        ],
        tensions=[
            "Different social energy levels can cause occasional friction.",
            "One friend may over-give and under-ask.",
            "Scheduling expectations may drift without planning.",
        ],
        advice=[
            "Plan one recurring catch-up cadence.",
            "Be explicit when you need help instead of hinting.",
            "Celebrate small wins together to reinforce momentum.",
        ],
        disclaimer=DISCLAIMER,
    )


def _parse(content: str, match_type: MatchType, language: str | None) -> PalmMatchResponse:
    data = parse_json_payload(content)
    if not data:
        return _fallback(match_type, language)
    fallback = _fallback(match_type, language)
    score = max(0, min(100, int(data.get("score", 0) or 0)))
    strengths = [str(v) for v in data.get("strengths", []) if isinstance(v, str)]
    tensions = [str(v) for v in data.get("tensions", []) if isinstance(v, str)]
    advice = [str(v) for v in data.get("advice", []) if isinstance(v, str)]
    return PalmMatchResponse(
        match_type=match_type,
        score=score,
        summary=str(data.get("summary", "")).strip() or "Compatibility reading completed.",
        strengths=strengths or fallback.strengths,
        tensions=tensions or fallback.tensions,
        advice=advice or fallback.advice,
        disclaimer=fallback.disclaimer,
    )


async def analyze_palm_match(
    image_a_bytes: bytes,
    image_b_bytes: bytes,
    match_type: MatchType,
    person_a_birth: str,
    person_b_birth: str,
    language: str | None = None,
) -> PalmMatchResponse:
    validate_image_bytes(image_a_bytes)
    validate_image_bytes(image_b_bytes)

    if not os.getenv("OPENAI_API_KEY"):
        return _fallback(match_type, language)

    try:
        b64_a = base64.b64encode(image_a_bytes).decode("ascii")
        b64_b = base64.b64encode(image_b_bytes).decode("ascii")
        prompt = _build_prompt(match_type, person_a_birth, person_b_birth, language)
        content = await call_openai_vision(prompt, [b64_a, b64_b])
        return _parse(content, match_type, language)
    except Exception:  # noqa: BLE001
        return _fallback(match_type, language)
