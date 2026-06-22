import asyncio
import json
from dataclasses import dataclass, field
from typing import Any
from uuid import uuid4

from fastapi import FastAPI, WebSocket, WebSocketDisconnect

app = FastAPI()

REQUEST_TIMEOUT_SECONDS = 10.0


@dataclass
class DeviceConnection:
    # Active device socket and all in-flight HTTP requests waiting for device responses.
    websocket: WebSocket
    pending: dict[str, asyncio.Future[Any]] = field(default_factory=dict)
    # Protects pending map from concurrent updates by HTTP and WS handlers.
    pending_lock: asyncio.Lock = field(default_factory=asyncio.Lock)


# device_id -> current live websocket session
connections: dict[str, DeviceConnection] = {}
# Protects global connection registry (connect/disconnect/read access).
connections_lock = asyncio.Lock()


def log(message: str) -> None:
    """Print a unified server log message with ws-bridge prefix."""
    print(f"[ws-bridge] {message}")


def build_get_notes_command(request_id: str) -> str:
    """Build JSON command asking device to return notes for the given request id."""
    return json.dumps({"action": "get_notes", "request_id": request_id})


def build_get_events_command(request_id: str) -> str:
    """Build legacy get_events command for backward-compatible callers."""
    return build_get_notes_command(request_id)


def parse_device_message(raw_message: str) -> tuple[str | None, Any | None]:
    """Parse raw device payload and return (request_id, result) for valid responses."""
    try:
        payload = json.loads(raw_message)
    except json.JSONDecodeError:
        return None, None

    if not isinstance(payload, dict):
        return None, None

    request_id = payload.get("request_id")
    if not isinstance(request_id, str) or not request_id:
        return None, None

    return request_id, payload.get("result")


async def remove_connection(device_id: str, websocket: WebSocket | None = None) -> None:
    """Remove device connection and fail all waiting requests for this device."""
    async with connections_lock:
        current = connections.get(device_id)
        if current is None:
            return
        if websocket is not None and current.websocket is not websocket:
            return
        connection = connections.pop(device_id)

    async with connection.pending_lock:
        # Complete all waiting HTTP futures with an explicit disconnect reason
        # to prevent leaked awaiters and hanging requests.
        for pending_future in connection.pending.values():
            if not pending_future.done():
                pending_future.set_exception(RuntimeError("device disconnected"))
        connection.pending.clear()


@app.websocket("/ws/{device_id}")
async def websocket_endpoint(websocket: WebSocket, device_id: str) -> None:
    """Maintain device websocket session and route device responses to pending futures."""
    await websocket.accept()
    log(f"device connected: {device_id}")

    async with connections_lock:
        previous = connections.get(device_id)
        connections[device_id] = DeviceConnection(websocket=websocket)

    if previous is not None:
        await previous.websocket.close(code=1000, reason="replaced by a newer session")

    try:
        while True:
            raw_message = await websocket.receive_text()
            request_id, result = parse_device_message(raw_message)

            if request_id is None:
                log(f"ignored malformed payload from {device_id}: {raw_message}")
                continue

            async with connections_lock:
                connection = connections.get(device_id)

            if connection is None:
                log(f"dropped response for disconnected device {device_id}")
                continue

            async with connection.pending_lock:
                # Match async device response to the exact HTTP caller by request_id.
                future = connection.pending.get(request_id)

            if future is None:
                log(f"unknown request_id from {device_id}: {request_id}")
                continue

            if not future.done():
                future.set_result(result)
                log(f"response matched: {device_id} request_id={request_id}")
    except WebSocketDisconnect:
        log(f"device disconnected: {device_id}")
    except Exception as error:
        log(f"device error {device_id}: {error}")
    finally:
        await remove_connection(device_id=device_id, websocket=websocket)


@app.get("/get-notes/{device_id}")
async def get_notes(device_id: str) -> dict[str, Any]:
    """Send get_notes command to device and wait for correlated async response."""
    return await request_notes_from_device(device_id)


@app.get("/get-events/{device_id}")
async def get_events(device_id: str) -> dict[str, Any]:
    """Legacy alias for get_notes kept during client migration."""
    return await request_notes_from_device(device_id)


async def request_notes_from_device(device_id: str) -> dict[str, Any]:
    """Send get_notes command to device and wait for correlated async response."""
    async with connections_lock:
        connection = connections.get(device_id)

    if connection is None:
        return {"error": "device not connected"}

    request_id = str(uuid4())
    # This future is resolved by websocket_endpoint when the device sends response.
    response_future = asyncio.get_running_loop().create_future()

    async with connection.pending_lock:
        connection.pending[request_id] = response_future

    try:
        await connection.websocket.send_text(build_get_notes_command(request_id))
        log(f"request sent: device={device_id} request_id={request_id}")
        result = await asyncio.wait_for(response_future, timeout=REQUEST_TIMEOUT_SECONDS)
        return {"request_id": request_id, "result": result}
    except asyncio.TimeoutError:
        return {"request_id": request_id, "error": "device response timeout"}
    except Exception as error:
        return {"request_id": request_id, "error": str(error)}
    finally:
        async with connection.pending_lock:
            # Always cleanup pending map (success, timeout, or transport failure).
            connection.pending.pop(request_id, None)
