"""Unit tests for the JSON parsing helpers in `app.analyzer.core`.

Run with:
    pytest backend/tests
"""
from app.analyzer.core import parse_json_payload, strip_json_fences


class TestStripJsonFences:
    def test_passthrough_when_no_fence(self):
        assert strip_json_fences('{"a": 1}') == '{"a": 1}'

    def test_strips_plain_triple_backticks(self):
        assert strip_json_fences('```\n{"a": 1}\n```').strip() == '{"a": 1}'

    def test_strips_json_tagged_fence(self):
        assert strip_json_fences('```json\n{"a": 1}\n```').strip() == '{"a": 1}'

    def test_handles_surrounding_whitespace(self):
        assert strip_json_fences('  ```json\n{"a": 1}\n```  ').strip() == '{"a": 1}'


class TestParseJsonPayload:
    def test_returns_dict_for_valid_json(self):
        assert parse_json_payload('{"summary": "ok"}') == {"summary": "ok"}

    def test_returns_dict_when_fenced(self):
        assert parse_json_payload('```json\n{"summary": "ok"}\n```') == {"summary": "ok"}

    def test_returns_empty_dict_for_invalid_json(self):
        assert parse_json_payload("not json at all") == {}

    def test_returns_empty_dict_for_json_list(self):
        # Top-level array is valid JSON but not the dict shape callers expect.
        assert parse_json_payload("[1, 2, 3]") == {}

    def test_returns_empty_dict_for_empty_string(self):
        assert parse_json_payload("") == {}

    def test_nested_dict_preserved(self):
        payload = '{"a": {"b": [1, 2]}}'
        assert parse_json_payload(payload) == {"a": {"b": [1, 2]}}
