"""Pytest configuration: ensure the `backend` dir is on sys.path so
`from app.analyzer.core import ...` resolves without installing the package.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent  # backend/
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
