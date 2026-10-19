# DataDrawer

[![JitPack](https://jitpack.io/v/al3j4ndrolm/DataDrawer.svg)](https://jitpack.io/#al3j4ndrolm/DataDrawer)
[![Build](https://github.com/al3j4ndrolm/DataDrawer/actions/workflows/build.yml/badge.svg)](https://github.com/al3j4ndrolm/DataDrawer/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Android local storage in one line.** No DataStore setup, no Room boilerplate, no `suspend`.

<p align="center">
  <img src="docs/demo.gif" alt="Type a name and add notes, close the app, reopen it: everything is still there" width="270">
</p>

```kotlin
datadrawer.save("name", "Ale")
val name = datadrawer.get<String>("name")
```

Works from an Activity, Fragment, Compose or a ViewModel. No `Context`, no `init()`.

## Jetpack Compose

```kotlin
var name by rememberDrawer("name", default = "")

TextField(value = name, onValueChange = { name = it })
```

Like `remember { mutableStateOf("") }`, but it's still there after the app is closed.

## Objects and lists

```kotlin
@Serializable
data class Note(val text: String, val done: Boolean = false)

datadrawer.save("note", Note("Buy milk"))
val note = datadrawer.get<Note>("note")

val notes = datadrawer.list<Note>("notes")
notes.add(Note("Buy milk"))
notes.remove(Note("Buy milk"))
notes.all()                                  // List<Note>
```

In Compose: `val items by notes.collectAsState()`.

## Drawers

Keep unrelated data apart. Each drawer is its own file:

```kotlin
val settings = datadrawer.drawer("settings")
settings.save("darkMode", true)

datadrawer.drawer("session").clear()         // wipes only that drawer
```

## Install

Add JitPack to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then add one dependency (min SDK 23):

```kotlin
// Compose apps: rememberDrawer and list state, plus everything else
implementation("com.github.al3j4ndrolm.DataDrawer:datadrawer-compose:1.0.0")

// Views apps
implementation("com.github.al3j4ndrolm.DataDrawer:datadrawer:1.0.0")
```

To save your own classes, also apply the serialization plugin in that module:

```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "<your Kotlin version>"
}
```

## API

| | |
|---|---|
| `save(key, value)` | Saves text, numbers, booleans, lists, maps or `@Serializable` objects. `null` removes |
| `get<T>(key)` / `get(key, default)` | Reads a value; `null` or `default` if missing or a different type |
| `flow<T>(key)` | Emits the value now and on every change |
| `list<T>(key)` | A saved list: `add`, `remove`, `removeAt`, `removeIf`, `updateIf`, `all`, `flow` |
| `contains`, `remove`, `clear`, `keys`, `all` | Inspect and clean up |
| `drawer(name)`, `drawers()` | Named drawers |

## Good to know

- Reads are instant: each drawer is kept in memory. Saves return immediately and are written to disk in the background within milliseconds.
- Made for settings, small objects and lists of up to a few thousand items. For large or searchable datasets, use Room.
- Data is stored as plain JSON in your app's private folder. Don't store passwords or tokens.
- Changed a class? Removed fields are ignored, and new fields need a default value to read old data.

## Contributing

```bash
./gradlew :datadrawer:testDebugUnitTest :datadrawer-compose:assembleRelease :sample:assembleDebug
```

`datadrawer/` is the core library, `datadrawer-compose/` the Compose helpers, and `sample/` a demo app.

## License

[MIT](LICENSE)
