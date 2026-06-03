# WS Bridge Server Sequence

## Основной сценарий: успешный запрос `GET /get-notes/{device_id}`

```mermaid
sequenceDiagram
    autonumber
    participant C as HTTP Client
    participant S as WS Bridge Server
    participant D as Device (WebSocket)

    C->>S: GET /get-notes/{device_id}
    S->>S: Проверка connections[device_id]

    alt Device connected
        S->>S: request_id = UUID
        S->>S: pending[request_id] = Future
        S->>D: {"action":"get_notes","request_id":"..."}
        D-->>S: {"request_id":"...","result":...}
        S->>S: future.set_result(result)
        S-->>C: 200 {"request_id":"...","result":...}
        S->>S: pending.pop(request_id)
    else Device not connected
        S-->>C: 200 {"error":"device not connected"}
    end
```

## Альтернативы и ошибки

```mermaid
sequenceDiagram
    autonumber
    participant C as HTTP Client
    participant S as WS Bridge Server
    participant D as Device (WebSocket)

    C->>S: GET /get-notes/{device_id}
    S->>D: send get_notes(request_id)

    alt Timeout (10s)
        S->>S: wait_for(Future, 10s) -> TimeoutError
        S-->>C: 200 {"request_id":"...","error":"device response timeout"}
        S->>S: pending.pop(request_id)
    else Device disconnected during wait
        D--xS: WebSocket disconnect
        S->>S: remove_connection(device_id)
        S->>S: all pending futures -> RuntimeError("device disconnected")
        S-->>C: 200 {"request_id":"...","error":"device disconnected"}
    else Malformed/unknown device payload
        D-->>S: invalid JSON OR unknown request_id
        S->>S: log + ignore
        Note over S: Ожидание продолжается до timeout/валидного ответа
    end
```

## Примечания для команды
- Корреляция запрос-ответ выполняется строго по `request_id`.
- `GET /get-events/{device_id}` временно сохранен как legacy alias.
- Для потокобезопасности используются два lock:
  - `connections_lock` для глобального реестра устройств.
  - `pending_lock` для in-flight запросов конкретного устройства.
- Очистка `pending` в `finally` обязательна для предотвращения утечек ожиданий.
- Одинаковый `device_id` допускает только одну активную WS-сессию: старая закрывается при новом подключении.
