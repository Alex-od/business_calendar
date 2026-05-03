# Radiant Days: developer overview

## Purpose

Radiant Days is an Android calendar application for creating, editing, viewing, and deleting calendar events. The app uses a Clean Architecture split across three Gradle modules and a single-activity Jetpack Compose UI.

Current package and application id:

```text
ua.danichapps.radiantdays
```

## Module structure

```text
:domain
:data
:app
```

### `:domain`

The domain module contains the business model, repository contract, and use cases. It has no Android dependencies and is designed to stay portable.

Key packages:

```text
ua.danichapps.radiantdays.domain.model
ua.danichapps.radiantdays.domain.repository
ua.danichapps.radiantdays.domain.usecase
```

Important classes:

- `CalendarEvent` - main business entity.
- `CalendarEvent.folderGuid` - optional folder GUID for grouping events.
- `DomainResult` - success/error result wrapper used across suspend operations.
- `CalendarEventRepository` - repository interface consumed by use cases and ViewModels.
- `AddEventUseCase`, `UpdateEventUseCase`, `DeleteEventUseCase` - write operations with validation.
- `GetEventsForDayUseCase`, `GetEventsForMonthUseCase`, `GetUpcomingEventsUseCase` - read/query operations.

Domain rules should live here, not in ViewModels or Compose screens.

### `:data`

The data module implements local and remote data access and binds `CalendarEventRepository` to its implementation.

Key packages:

```text
ua.danichapps.radiantdays.data.di
ua.danichapps.radiantdays.data.local
ua.danichapps.radiantdays.data.remote
ua.danichapps.radiantdays.data.repository
```

Responsibilities:

- Room database storage.
- Entity/DTO/domain mapping.
- Ktor remote data source contracts.
- Repository implementation.
- Koin data bindings.

Room database:

```text
calendar.db
```

Current schema version:

```text
4
```

Main tables:

- `notes` - persisted calendar records.
- `folders` - event folder metadata with `guid` as the primary key.

The `notes.folder_guid` column is nullable. Existing events are migrated with `NULL`, meaning they do not belong to any folder until assigned. The column references `folders.guid` and is indexed for future folder-based queries.

Room schemas are exported under:

```text
data/schemas
```

### `:app`

The app module contains Android entry points, Compose UI, navigation, dependency injection setup, notifications, WorkManager, Glance widget, and the WebSocket bridge client.

Key packages:

```text
ua.danichapps.radiantdays.di
ua.danichapps.radiantdays.ui
ua.danichapps.radiantdays.notification
ua.danichapps.radiantdays.widget
ua.danichapps.radiantdays.sync
```

## Application startup

Entry point:

```text
ua.danichapps.radiantdays.RadiantDaysApp
```

Startup responsibilities:

- Start Koin with `dataModule`, `domainModule`, and `presentationModule`.
- Resolve and manage `WebSocketBridgeClient`.
- Register activity lifecycle callbacks for foreground/background bridge lifecycle.
- Create the notification channel.
- Schedule `EventNotificationWorker`.

The app uses the `RadiantDaysApp` application class; the product name, project name, package, namespace, and application id have been renamed to Radiant Days.

## UI and navigation

Main activity:

```text
ua.danichapps.radiantdays.MainActivity
```

`MainActivity` hosts Compose content and delegates all screen routing to `AppNavigation`.

Navigation destinations are declared in:

```text
ua.danichapps.radiantdays.ui.navigation.Screen
```

Current routes:

- `calendar` - main calendar screen.
- `settings` - empty settings placeholder screen.
- `add_event/{selectedDayMillis}` - create event screen.
- `edit_event/{eventId}` - edit existing event screen.

### Calendar screen

File:

```text
app/src/main/java/ua/danichapps/radiantdays/ui/calendar/CalendarScreen.kt
```

Responsibilities:

- Show month grid.
- Show events for the selected day.
- Navigate to add/edit flows.
- Delete events.
- Open the toolbar overflow menu.

The toolbar has a three-dot overflow menu. Selecting `Settings` navigates to the settings screen.

### Add/Edit event screen

File:

```text
app/src/main/java/ua/danichapps/radiantdays/ui/addevent/AddEditEventScreen.kt
```

Responsibilities:

- Create new events.
- Edit existing events.
- Show validation errors through one-shot UI events.
- Navigate back after successful save.

### Settings screen

File:

```text
app/src/main/java/ua/danichapps/radiantdays/ui/settings/SettingsScreen.kt
```

Current state:

- Empty placeholder screen.
- Has a top app bar.
- Has back navigation.

This screen is ready to receive future settings sections.

## State management pattern

ViewModels expose:

- `StateFlow<...UiState>` for persistent UI state.
- `Channel<...UiEvent>` for one-shot events such as snackbars and navigation.

Compose screens collect state with lifecycle awareness and consume one-shot events in `LaunchedEffect`.

## Dependency injection

Koin is used for dependency injection.

Modules:

- `dataModule` in `:data`.
- `domainModule` in `:app`.
- `presentationModule` in `:app`.

Guidelines:

- Use cases are stateless and should be registered as factories.
- ViewModels are registered in the presentation module.
- Data implementations are registered in the data module.

## Background work and notifications

Notifications are handled by:

```text
ua.danichapps.radiantdays.notification.EventNotificationManager
ua.danichapps.radiantdays.notification.EventNotificationWorker
```

The worker periodically checks upcoming events and delegates notification display to the manager.

Required permissions are declared in `AndroidManifest.xml`, including:

- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`
- `WAKE_LOCK`
- `INTERNET`

## Widget

The home-screen widget lives under:

```text
ua.danichapps.radiantdays.widget
```

It uses Glance and reads events through Koin global context because widgets run outside the usual Compose screen scope.

Widget provider configuration:

```text
app/src/main/res/xml/calendar_widget_info.xml
```

## WebSocket bridge

The app module contains a WebSocket bridge client under:

```text
ua.danichapps.radiantdays.sync
```

Key classes:

- `WebSocketBridgeClient`
- `BridgeJsonCodec`
- `DeviceIdProvider`

Build config fields:

```text
WS_BRIDGE_HOST = "10.0.2.2"
WS_BRIDGE_PORT = 8000
```

More detailed bridge documentation already exists:

```text
documentation/ws_bridge_server.md
documentation/ws_bridge_server_sequence.md
```

## Build and verification commands

From the project root:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lintDebug
```

The latest verified install command:

```bash
./gradlew :app:installDebug
```

It installed successfully on:

```text
X-treme PQ53 - Android 8.1.0
```

## Development guidelines

- Keep validation inside domain use cases.
- Keep ViewModels focused on UI state, events, and use case orchestration.
- Do not throw exceptions across layer boundaries; return `DomainResult`.
- Keep Compose screens declarative and avoid embedding business rules in UI.
- Use repository interfaces from `:domain`; implementations belong in `:data`.
- Add explicit Room migrations before production releases when changing the schema.
- Keep logs consistent with the project convention, for example:

```kotlin
Log.d("qqwe_tag ClassName, functionName, param:", message)
```

## Current known placeholders

- The settings screen exists but intentionally has no settings yet.
- The application class is `RadiantDaysApp`.
- `fallbackToDestructiveMigration()` may be acceptable for development but should be replaced with explicit migrations before production release.
