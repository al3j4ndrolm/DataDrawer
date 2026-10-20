package io.github.al3j4ndrolm.facilis.storage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf

/**
 * Like `remember { mutableStateOf(default) }`, but saved in a [Drawer]: the value is still there
 * after the app is closed, and every screen using the same key stays in sync.
 *
 * ```
 * var name by rememberStorage("name", default = "")
 * TextField(value = name, onValueChange = { name = it })
 * ```
 *
 * Works with any type a drawer can store, including your own `@Serializable` classes.
 */
@Composable
inline fun <reified T> rememberStorage(
    key: String,
    default: T,
    drawer: Drawer = storage,
): MutableState<T> = rememberStorage(key, default, composeSerializerFor<T>(), drawer)

/** [rememberStorage] using an explicit [serializer]. */
@Composable
fun <T> rememberStorage(
    key: String,
    default: T,
    serializer: KSerializer<T>,
    drawer: Drawer = storage,
): MutableState<T> {
    val currentDefault by rememberUpdatedState(default)
    val state = remember(drawer, key) {
        DrawerState(drawer.get(key, serializer) ?: default) { drawer.save(key, it, serializer) }
    }
    // Picks up changes made elsewhere: another screen, a ViewModel or a background thread.
    LaunchedEffect(state, drawer, key) {
        drawer.flow(key, serializer).collect { state.sync(it ?: currentDefault) }
    }
    return state
}

/**
 * The items of a [DrawerList] as Compose state, updated whenever the list changes.
 *
 * ```
 * val notes = storage.list<Note>("notes")
 * val items by notes.collectAsState()
 * ```
 */
@Composable
fun <T> DrawerList<T>.collectAsState(): State<List<T>> =
    remember(this) { flow() }.collectAsState(initial = remember(this) { all() })

/**
 * Holds the value locally so writes show up in the same frame (important for text fields), and
 * saves every write to the drawer.
 */
@Stable
private class DrawerState<T>(initial: T, private val save: (T) -> Unit) : MutableState<T> {
    private val local = mutableStateOf(initial)

    override var value: T
        get() = local.value
        set(newValue) {
            local.value = newValue
            save(newValue)
        }

    fun sync(stored: T) {
        local.value = stored
    }

    override fun component1(): T = value

    override fun component2(): (T) -> Unit = { value = it }
}

/** Same as the core library's helper; not part of the public API. */
@PublishedApi
internal inline fun <reified T> composeSerializerFor(): KSerializer<T> =
    try {
        serializer<T>()
    } catch (e: SerializationException) {
        throw IllegalArgumentException(
            "FacilisStorage can't store ${typeOf<T>()}. Mark the class with @Serializable and apply the " +
                "plugin id(\"org.jetbrains.kotlin.plugin.serialization\") in your module's build.gradle.kts.",
            e,
        )
    }
