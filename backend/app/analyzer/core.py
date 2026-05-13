"""Shared building blocks for the palm analyzer modules.

Contains:
- The legal disclaimer string appended to every response.
- Image-bytes validation.
- Defensive JSON parsing helpers.
- A single multimodal OpenAI call helper used by both palm and match flows.
"""
from __future__ import annotations

import io
import json
import os
from typing import Any, Dict, List, Sequence

from PIL import Image


DISCLAIMER = (
    "This reading is for entertainment purposes only. It is not medical, "
    "financial, psychological, or legal advice."
)
DEFAULT_MODEL = "gpt-4o-mini"
TEMPERATURE = 0.8


def validate_image_bytes(image_bytes: bytes) -> None:
    """Raise ValueError if the bytes are empty or not a decodable image."""
    if not image_bytes:
        raise ValueError("Empty image payload")
    try:
        with Image.open(io.BytesIO(image_bytes)) as img:
            img.verify()
    except Exception as exc:  # noqa: BLE001 - validation boundary
        raise ValueError(f"Invalid image: {exc}") from exc


def strip_json_fences(content: str) -> str:
    """Strip ```json ... ``` fences some models emit around JSON payloads."""
    cleaned = content.strip()
    if cleaned.startswith("```"):
        cleaned = cleaned.strip("`")
        if cleaned.lower().startswith("json"):
            cleaned = cleaned[4:].lstrip()
    return cleaned


def parse_json_payload(content: str) -> Dict[str, Any]:
    """Safe JSON load that always returns a dict (empty dict on any failure)."""
    try:
        data = json.loads(strip_json_fences(content))
        return data if isinstance(data, dict) else {}
    except Exception:  # noqa: BLE001
        return {}


async def call_openai_vision(prompt: str, images_b64: Sequence[str]) -> str:
    """Single entry point for multimodal OpenAI calls.

    Returns the raw string content from the model; callers parse it.
    Caller is responsible for catching exceptions and falling back.
    """
    from openai import AsyncOpenAI

    client = AsyncOpenAI(api_key=os.environ["OPENAI_API_KEY"])
    content: List[Dict[str, Any]] = [{"type": "text", "text": prompt}]
    content.extend(
        {
            "type": "image_url",
            "image_url": {"url": f"data:image/jpeg;base64,{b64}"},
        }
        for b64 in images_b64
    )

    result = await client.chat.completions.create(
        model=os.getenv("FORTUNE_MODEL", DEFAULT_MODEL),
        messages=[{"role": "user", "content": content}],
        temperature=TEMPERATURE,
    )
    return result.choices[0].message.content or ""
