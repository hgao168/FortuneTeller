from typing import List, Literal, Optional
from pydantic import BaseModel, Field


ReadingScope = Literal["today", "month", "year", "long_term"]
MatchType = Literal["romantic", "friend"]


class PalmReadingSection(BaseModel):
    title: str
    text: str


class PalmReadingResponse(BaseModel):
    scope: ReadingScope
    summary: str
    sections: List[PalmReadingSection] = Field(default_factory=list)
    advice: List[str] = Field(default_factory=list)
    disclaimer: str = "Entertainment only. Not medical, financial, or legal advice."


class PalmMatchResponse(BaseModel):
    match_type: MatchType
    score: int = Field(ge=0, le=100)
    summary: str
    strengths: List[str] = Field(default_factory=list)
    tensions: List[str] = Field(default_factory=list)
    advice: List[str] = Field(default_factory=list)
    disclaimer: str = "Entertainment only. Not medical, financial, or legal advice."


class PalmValidationError(BaseModel):
    detail: str
    reason: Optional[str] = None
