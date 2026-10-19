package io.github.al3j4ndrolm.datadrawer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf

/**
 * A named group of saved values. Values can be text, numbers, booleans, lists, maps or your own
 * `@Serializable` classes.
 *
 * Reads are instant (everything is kept in memory) and saves return immediately; writing to disk
 * happens in the background. Safe to use from any thread.
 */
open class Drawer internal constructor(
    /** This drawer's name; the default drawer is called "default". */
    val name: String,
    private val storage: DrawerStorage,
) {
    private val state: MutableStateFlow<Map<String, JsonElement>> by lazy { MutableStateFlow(storage.read()) }

    // region Values

    /**
     * Saves [value] under [key], replacing what was there. Saving `null` removes the key.
     *
     * ```
     * datadrawer.save("name", "Ale")
     * datadrawer.save("user", User("Ale", 22))   // a @Serializable class
     * ```
     */
    inline fun <reified T> save(key: String, value: T) {
        if (value == null) remove(key) else save(key, value, serializerFor<T>())
    }

    /** Saves [value] using an explicit [serializer]. */
    fun <T> save(key: String, value: T, serializer: KSerializer<T>) {
        if (value == null) {
            remove(key)
            return
        }
        val element = json.encodeToJsonElement(serializer, value)
        edit { it + (key to element) }
    }

    /**
     * Returns the value saved under [key], or `null` if there is none or it was saved as a
     * different type.
     *
     * ```
     * val name = datadrawer.get<String>("name")
     * ```
     */
    inline fun <reified T> get(key: String): T? = get(key, serializerFor<T>())

    /** Returns the value saved under [key], or [default] if there is none. */
    inline fun <reified T> get(key: String, default: T): T = get(key, serializerFor<T>()) ?: default

    /** Reads [key] using an explicit [serializer]. */
    fun <T> get(key: String, serializer: KSerializer<T>): T? =
        state.value[key]?.let { decode(serializer, it) }

    /**
     * Emits the value under [key] now and every time it changes. Emits `null` when the key is
     * missing.
     */
    inline fun <reified T> flow(key: String): Flow<T?> = flow(key, serializerFor<T>())

    /** [flow] using an explicit [serializer]. */
    fun <T> flow(key: String, serializer: KSerializer<T>): Flow<T?> =
        state.map { it[key] }.distinctUntilChanged().map { element -> element?.let { decode(serializer, it) } }

    /** Whether something is saved under [key]. */
    operator fun contains(key: String): Boolean = key in state.value

    /** Removes [key]. Does nothing if it isn't there. */
    fun remove(key: String) {
        if (key in state.value) edit { it - key }
    }

    /** Removes everything in this drawer. Other drawers are not affected. */
    fun clear() {
        if (state.value.isNotEmpty()) edit { emptyMap() }
    }

    /** Every key in this drawer. */
    fun keys(): Set<String> = state.value.keys

    /**
     * Everything in this drawer as plain Kotlin values: `String`, `Long`, `Double`, `Boolean`,
     * `List`, `Map` or `null`. Objects come back as `Map`s; use [get] to read them as your class.
     */
    fun all(): Map<String, Any?> = state.value.mapValues { (_, element) -> element.toPlain() }

    // endregion

    // region Lists

    /**
     * A list saved under [key], edited item by item.
     *
     * ```
     * val notes = datadrawer.list<Note>("notes")
     * notes.add(Note("Buy milk"))
     * notes.all()
     * ```
     */
    inline fun <reified T> list(key: String): DrawerList<T> = list(key, serializerFor<T>())

    /** [list] using an explicit item [serializer]. */
    fun <T> list(key: String, serializer: KSerializer<T>): DrawerList<T> = DrawerList(this, key, serializer)

    /**
     * Atomically replaces the value under [key] with [transform] of the current one. Returning
     * `null` removes the key. [transform] may run more than once if another thread writes at the
     * same time, so it must not have side effects.
     */
    internal fun <T> update(key: String, serializer: KSerializer<T>, transform: (T?) -> T?) {
        edit { current ->
            val old = current[key]?.let { decode(serializer, it) }
            val new = transform(old)
            if (new == null) current - key else current + (key to json.encodeToJsonElement(serializer, new))
        }
    }

    // endregion

    override fun toString(): String = "Drawer($name)"

    private fun edit(transform: (Map<String, JsonElement>) -> Map<String, JsonElement>) {
        state.update(transform)
        storage.scheduleWrite { state.value }
    }

    private fun <T> decode(serializer: KSerializer<T>, element: JsonElement): T? =
        try {
            json.decodeFromJsonElement(serializer, element)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
}

internal val json = Json {
    // Lets you remove fields from a saved class without losing the rest of its data.
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * kotlinx.serialization's [serializer], with an error that says how to fix the usual mistake.
 * Not part of the public API.
 */
@PublishedApi
internal inline fun <reified T> serializerFor(): KSerializer<T> =
    try {
        serializer<T>()
    } catch (e: SerializationException) {
        throw IllegalArgumentException(
            "DataDrawer can't store ${typeOf<T>()}. Mark the class with @Serializable and apply the " +
                "plugin id(\"org.jetbrains.kotlin.plugin.serialization\") in your module's build.gradle.kts.",
            e,
        )
    }

private fun JsonElement.toPlain(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
    is JsonArray -> map { it.toPlain() }
    is JsonObject -> mapValues { (_, value) -> value.toPlain() }
}
