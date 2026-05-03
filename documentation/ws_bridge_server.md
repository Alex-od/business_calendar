# WS Bridge Server Documentation

## Назначение
`ws_bridge_server.py` реализует мост между:
- HTTP-клиентом (запрашивает события через REST)
- Android-устройством (подключено по WebSocket и отдает данные)

Сервер связывает HTTP-запрос и ответ устройства через `request_id`.

## Расположение
- Сервер: `C:\StudioProjects\my\RadiantDays\mcp-server\ws_bridge_server.py`
- Тесты: `C:\StudioProjects\my\RadiantDays\mcp-server\tests\test_ws_bridge_server.py`

## Основные компоненты

### `DeviceConnection`
Хранит состояние активного устройства:
- `websocket`: текущее WebSocket-соединение устройства
- `pending`: `request_id -> Future`, где `Future` ожидает ответ устройства
- `pending_lock`: `asyncio.Lock` для потокобезопасной работы с `pending`

### Глобальные структуры
- `connections`: `device_id -> DeviceConnection`
- `connections_lock`: lock для безопасного доступа к `connections`

## API

### WebSocket: `/ws/{device_id}`
Устройство подключается к серверу и слушает команды.

Поведение:
1. При подключении сохраняется новая сессия для `device_id`.
2. Если была старая сессия того же `device_id`, она закрывается с причиной `replaced by a newer session`.
3. Входящие сообщения устройства парсятся, и по `request_id` находится ожидающий HTTP-запрос.
4. Результат пробрасывается в соответствующий `Future`.
5. При дисконнекте очищаются все незавершенные запросы устройства.

### HTTP: `GET /get-events/{device_id}`
Запрашивает у конкретного устройства список событий.

Поведение:
1. Проверяется, подключено ли устройство.
2. Генерируется `request_id` (`UUID`).
3. Создается `Future` и сохраняется в `pending`.
4. Устройству отправляется команда `get_events`.
5. Сервер ждет ответ от устройства до таймаута.
6. В любом случае запись из `pending` удаляется в `finally`.

## Формат сообщений

### Команда сервер -> устройство
```json
{
  "action": "get_events",
  "request_id": "<uuid>"
}
```

### Ответ устройство -> сервер
```json
{
  "request_id": "<uuid>",
  "result": <any-json>
}
```

`result` может быть массивом, объектом или другим валидным JSON-значением.

## Таймауты
- `REQUEST_TIMEOUT_SECONDS = 10.0`
- Если устройство не ответило за это время, HTTP-ответ:
```json
{
  "request_id": "<uuid>",
  "error": "device response timeout"
}
```

## Ошибки и edge-cases

### Устройство не подключено
```json
{
  "error": "device not connected"
}
```

### Некорректный payload от устройства
Сообщение игнорируется, сервер продолжает работу.

### Неизвестный `request_id` в сообщении устройства
Сообщение логируется и игнорируется.

### Дисконнект устройства во время ожидания
Все ожидающие `Future` завершаются ошибкой `device disconnected`.

## Последовательность (high-level)
1. Клиент вызывает `GET /get-events/{device_id}`.
2. Сервер создает `request_id` и сохраняет ожидание в `pending`.
3. Сервер отправляет команду в WebSocket устройства.
4. Устройство отвечает JSON с тем же `request_id`.
5. WebSocket-обработчик завершает нужный `Future`.
6. HTTP-обработчик возвращает `result` клиенту.

## Логирование
Используется функция `log()` с префиксом `[ws-bridge]`.
Логируются ключевые события:
- подключение/отключение устройств
- отправка запросов на устройство
- успешная корреляция ответа по `request_id`
- malformed payload и неизвестные `request_id`

## Concurrency и корректность
- `connections_lock` защищает глобальный реестр устройств.
- `pending_lock` защищает таблицу ожидающих запросов конкретного устройства.
- Очистка `pending` в `finally` защищает от утечек при таймауте/ошибках.

## Security и прод-рекомендации
Текущая реализация подходит для локальной/внутренней среды, но для production рекомендуется:
1. Добавить аутентификацию устройства на WebSocket (`token`, mTLS или подпись).
2. Ограничить доступ к HTTP endpoint по auth.
3. Ввести rate limiting на `GET /get-events/{device_id}`.
4. Валидировать и ограничивать размер входящих payload.
5. Перевести `print`-логи на структурированный логгер (уровни, trace-id).
6. Добавить метрики (таймауты, ошибки, latency).

## Как запускать (пример)
Из папки `mcp-server`:
```bash
uvicorn ws_bridge_server:app --host 0.0.0.0 --port 8080
```

## Быстрый smoke-check
1. Подключить тестовый WebSocket-клиент как устройство: `/ws/test-device`.
2. Вызвать `GET /get-events/test-device`.
3. Отправить в WebSocket ответ с тем же `request_id`.
4. Проверить, что HTTP получил `result`.
