# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease

# Unit tests
./gradlew test                               # All modules
./gradlew :domain:test                       # Domain only
./gradlew :data:testDebugUnitTest            # Data only
./gradlew test --tests AddEventUseCaseTest   # Single test class

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint / checks
./gradlew lintDebug
./gradlew check                              # lint + all tests
```

## Architecture

### Architecture Decision Rule

- Do not propose any technology until the user flow proves it is necessary.
  Start with user flow first, architecture second, technology third.
- Before planning implementation, describe the user flow in plain language:
  who does what, where the action happens, what data is used, what result is shown.
- Separate product goal, learning goal, and technical implementation.
  If the goal is learning, explicitly mark which parts are educational-only and not production-clean.
- Prefer the simplest architecture that satisfies the confirmed user flow.
  Do not add backend, WebSocket, MCP, sync, workers, queues, caches, or agents unless they solve a concrete requirement.
- If a proposed solution requires another device, server, app, background process, or network connection to stay online,
  explicitly call this out as an operational dependency and ask for confirmation.

Clean Architecture — 3 Gradle modules:

| Module | Plugin | Role |
|--------|--------|------|
| `:domain` | `kotlin.jvm` | Entities, use-cases, repository interfaces. Zero Android deps. KMP-ready. |
| `:data` | `android.library` | Room + Ktor implementations. Binds `CalendarEventRepository` interface to impl. |
| `:app` | `android.application` | Compose UI, Koin init, navigation, notifications, WorkManager, widget. |

Package root: `ua.danichapps.radiantdays`

### Layer Interaction

Reactive reads flow up as `Flow<List<CalendarEvent>>` (Room emits on every DB change — no manual refresh needed). Write operations are `suspend` functions returning `DomainResult<T>`.

### DomainResult

All suspend operations return `DomainResult<T>` (sealed `Success` / `Error`). Use the provided `.onSuccess {}`, `.onError {}`, `.map {}` extensions — never throw exceptions across layer boundaries.

### Dependency Injection (Koin)

- `DataModule` lives in `:data`; `DomainModule` + `PresentationModule` live in `:app/di/`
- Use-cases → `factory {}` (stateless, new instance per injection)
- ViewModels → `viewModel {}` in `PresentationModule`
- All modules started in `RadiantDaysApp.onCreate()`

### Validation

All validation lives in use-cases, never in ViewModels or UI:
- Blank title → `DomainResult.Error`
- `endTime ≤ startTime` → `DomainResult.Error`
- `UpdateEventUseCase`: `id == 0L` → error (unsaved event guard)

### ViewModel Pattern

Every ViewModel exposes:
- `uiState: StateFlow<*UiState>` — persistent state surviving config changes
- `events: Channel<*UiEvent>` — one-shot events (navigation, snackbar), collected via `LaunchedEffect`

### Room Database

- Single DB file: `calendar.db`, version 1; schema exported to `data/schemas/`
- `fallbackToDestructiveMigration()` is enabled for development — add explicit migrations before any production release
- All timestamps are `Long` (epoch ms) across every layer

### Notifications & Widget

- `EventNotificationWorker` (WorkManager, 15-min periodic): calls `GetUpcomingEventsUseCase`, delegates to `EventNotificationManager.showEventNotification()`
- `CalendarWidget` (Glance, 30-min refresh): shows today's top-3 events; resolves use-cases via `GlobalContext.get()` (Koin global context) since Glance runs outside standard DI scope
