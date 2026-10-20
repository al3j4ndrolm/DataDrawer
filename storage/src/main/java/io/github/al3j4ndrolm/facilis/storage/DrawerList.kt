package io.github.al3j4ndrolm.facilis.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer

/**
 * A list saved in a [Drawer], edited item by item. Every change is saved immediately.
 *
 * ```
 * val notes = storage.list<Note>("notes")
 * notes.add(Note("Buy milk"))
 * notes.remove(note)
 * val all: List<Note> = notes.all()
 * ```
 */
class DrawerList<T> internal constructor(
    private val drawer: Drawer,
    /** The key this list is saved under. */
    val key: String,
    private val itemSerializer: KSerializer<T>,
) {
    private val serializer = ListSerializer(itemSerializer)

    /** A snapshot of every item, in order. */
    fun all(): List<T> = drawer.get(key, serializer).orEmpty()

    /** Emits the items now and every time the list changes. */
    fun flow(): Flow<List<T>> = drawer.flow(key, serializer).map { it.orEmpty() }

    val size: Int get() = all().size

    fun isEmpty(): Boolean = size == 0

    operator fun get(index: Int): T = all()[index]

    operator fun contains(item: T): Boolean = item in all()

    /** Adds [item] at the end. */
    fun add(item: T) = edit { it + item }

    /** Adds [item] at [index]. */
    fun add(index: Int, item: T) = edit { it.toMutableList().apply { add(index, item) } }

    /** Adds every item in [items] at the end. */
    fun addAll(items: Iterable<T>) = edit { it + items }

    /** Replaces the item at [index]. */
    operator fun set(index: Int, item: T) = edit { it.toMutableList().apply { set(index, item) } }

    /** Replaces every item matching [predicate] with [transform] of it. Returns how many changed. */
    fun updateIf(predicate: (T) -> Boolean, transform: (T) -> T): Int {
        var changed = 0
        edit { items ->
            changed = 0
            items.map { if (predicate(it)) transform(it).also { changed++ } else it }
        }
        return changed
    }

    /** Removes the first occurrence of [item]. Returns whether it was there. */
    fun remove(item: T): Boolean {
        var removed = false
        edit { items -> items.toMutableList().apply { removed = remove(item) } }
        return removed
    }

    /** Removes and returns the item at [index]. */
    fun removeAt(index: Int): T {
        var removed: T? = null
        edit { items -> items.toMutableList().apply { removed = removeAt(index) } }
        @Suppress("UNCHECKED_CAST")
        return removed as T
    }

    /** Removes every item matching [predicate]. Returns how many were removed. */
    fun removeIf(predicate: (T) -> Boolean): Int {
        var count = 0
        edit { items -> items.filterNot(predicate).also { count = items.size - it.size } }
        return count
    }

    /** Removes every item. */
    fun clear() = drawer.remove(key)

    override fun equals(other: Any?): Boolean =
        other is DrawerList<*> && other.drawer === drawer && other.key == key &&
            other.itemSerializer.descriptor == itemSerializer.descriptor

    override fun hashCode(): Int = 31 * (31 * drawer.hashCode() + key.hashCode()) + itemSerializer.descriptor.hashCode()

    override fun toString(): String = "DrawerList(${drawer.name}/$key)"

    private fun edit(transform: (List<T>) -> List<T>) {
        drawer.update(key, serializer) { current -> transform(current.orEmpty()).ifEmpty { null } }
    }
}
