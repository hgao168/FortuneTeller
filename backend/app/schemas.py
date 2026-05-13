from typing import List, Literal, Optional
from pydantic import BaseModel, Field


ReadingScope = Literal["year", "long_term"]


class PalmReadingSection(BaseModel):
    title: str
    text: str


class PalmReadingResponse(BaseModel):
    scope: ReadingScope
    summary: str
    sections: List[PalmReadingSection] = Field(default_factory=list)
    advice: List[str] = Field(default_factory=list)
    disclaimer: str = "Entertainment only. Not medical, financial, or legal advice."


class PalmValidationError(BaseModel):
    detail: str
    reason: Optional[str] = None
