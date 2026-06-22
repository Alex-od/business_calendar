import json

from ws_bridge_server import build_get_events_command, build_get_notes_command, parse_device_message


def test_build_get_notes_command_contains_action_and_request_id() -> None:
    payload = json.loads(build_get_notes_command("req-1"))
    assert payload["action"] == "get_notes"
    assert payload["request_id"] == "req-1"


def test_build_get_events_command_is_legacy_alias_for_notes() -> None:
    payload = json.loads(build_get_events_command("req-1"))
    assert payload["action"] == "get_notes"
    assert payload["request_id"] == "req-1"


def test_parse_device_message_returns_request_id_and_result() -> None:
    request_id, result = parse_device_message('{"request_id":"req-2","result":["A","B"]}')
    assert request_id == "req-2"
    assert result == ["A", "B"]


def test_parse_device_message_rejects_invalid_json() -> None:
    request_id, result = parse_device_message("not-json")
    assert request_id is None
    assert result is None


def test_parse_device_message_rejects_payload_without_request_id() -> None:
    request_id, result = parse_device_message('{"action":"get_notes"}')
    assert request_id is None
    assert result is None
