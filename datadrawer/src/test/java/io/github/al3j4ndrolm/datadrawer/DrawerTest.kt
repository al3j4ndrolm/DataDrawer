package io.github.al3j4ndrolm.datadrawer

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.Executor

@Serializable
data class User(val name: String, val age: Int, val tags: List<String> = emptyList())

@Serializable
data class Note(val text: String, val done: Boolean = false)

@Serializable
data class UserV1(val name: String, val age: Int)

class DrawerTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val file by lazy { File(temp.root, "test.json") }

    /** Writes run synchronously so tests can check the file right after a save. */
    private fun newDrawer(name: String = "test") = Drawer(name, DrawerStorage({ file }, Executor { it.run() }))

    // region Values

    @Test
    fun savesAndReadsSimpleValues() {
        val drawer = newDrawer()
        drawer.save("name", "Ale")
        drawer.save("age", 22)
        drawer.save("score", 9.5)
        drawer.save("big", 10_000_000_000L)
        drawer.save("premium", true)

        assertEquals("Ale", drawer.get<String>("name"))
        assertEquals(22, drawer.get<Int>("age"))
        assertEquals(9.5, drawer.get<Double>("score")!!, 0.0)
        assertEquals(10_000_000_000L, drawer.get<Long>("big"))
        assertEquals(true, drawer.get<Boolean>("premium"))
    }

    @Test
    fun missingKeyReturnsNullOrDefault() {
        val drawer = newDrawer()
        assertNull(drawer.get<String>("missing"))
        assertEquals("guest", drawer.get("missing", default = "guest"))
        assertEquals(0, drawer.get("count", default = 0))
    }

    @Test
    fun wrongTypeReturnsNullInsteadOfCrashing() {
        val drawer = newDrawer()
        drawer.save("age", 22)
        drawer.save("name", "Ale")

        assertNull(drawer.get<String>("age"))
        assertNull(drawer.get<Int>("name"))
        assertNull(drawer.get<User>("name"))
        assertEquals(-1, drawer.get("name", default = -1))
    }

    @Test
    fun savesAndReadsObjects() {
        val drawer = newDrawer()
        val user = User("Ale", 22, listOf("android", "kotlin"))
        drawer.save("user", user)
        assertEquals(user, drawer.get<User>("user"))
    }

    @Test
    fun savesAndReadsCollections() {
        val drawer = newDrawer()
        drawer.save("users", listOf(User("A", 1), User("B", 2)))
        drawer.save("scores", mapOf("math" to 90, "cs" to 100))

        assertEquals(listOf(User("A", 1), User("B", 2)), drawer.get<List<User>>("users"))
        assertEquals(mapOf("math" to 90, "cs" to 100), drawer.get<Map<String, Int>>("scores"))
    }

    @Test
    fun overwritesExistingValue() {
        val drawer = newDrawer()
        drawer.save("name", "Ale")
        drawer.save("name", "Alejandro")
        assertEquals("Alejandro", drawer.get<String>("name"))
    }

    @Test
    fun savingNullRemovesTheKey() {
        val drawer = newDrawer()
        drawer.save("name", "Ale")
        val nothing: String? = null
        drawer.save("name", nothing)
        assertFalse("name" in drawer)
    }

    @Test
    fun removeContainsKeysAndClear() {
        val drawer = newDrawer()
        drawer.save("a", 1)
        drawer.save("b", 2)

        assertTrue("a" in drawer)
        assertEquals(setOf("a", "b"), drawer.keys())

        drawer.remove("a")
        assertFalse("a" in drawer)
        assertEquals(setOf("b"), drawer.keys())

        drawer.clear()
        assertTrue(drawer.keys().isEmpty())
    }

    @Test
    fun allReturnsPlainValues() {
        val drawer = newDrawer()
        drawer.save("name", "Ale")
        drawer.save("age", 22)
        drawer.save("premium", true)
        drawer.save("user", User("Ale", 22))

        val all = drawer.all()
        assertEquals("Ale", all["name"])
        assertEquals(22L, all["age"])
        assertEquals(true, all["premium"])
        assertEquals(mapOf("name" to "Ale", "age" to 22L, "tags" to emptyList<Any>()), all["user"])
    }

    @Test
    fun explicitSerializerOverloadsWork() {
        val drawer = newDrawer()
        drawer.save("name", "Ale", String.serializer())
        assertEquals("Ale", drawer.get("name", String.serializer()))
    }

    @Test
    fun unserializableClassGivesAHelpfulError() {
        class NotSerializable(val x: Int)

        val drawer = newDrawer()
        val error = assertThrows(IllegalArgumentException::class.java) { drawer.save("x", NotSerializable(1)) }
        assertTrue(error.message!!.contains("@Serializable"))
    }

    // endregion

    // region Persistence

    @Test
    fun dataSurvivesANewInstance() {
        newDrawer().apply {
            save("name", "Ale")
            save("user", User("Ale", 22))
            list<Note>("notes").add(Note("Buy milk"))
        }

        val reopened = newDrawer()
        assertEquals("Ale", reopened.get<String>("name"))
        assertEquals(User("Ale", 22), reopened.get<User>("user"))
        assertEquals(listOf(Note("Buy milk")), reopened.list<Note>("notes").all())
    }

    @Test
    fun clearingDeletesTheFile() {
        val drawer = newDrawer()
        drawer.save("a", 1)
        assertTrue(file.exists())
        drawer.clear()
        assertFalse(file.exists())
    }

    @Test
    fun corruptFileStartsEmptyAndIsKept() {
        file.writeText("{ this is not json")

        val drawer = newDrawer()
        assertTrue(drawer.keys().isEmpty())
        assertTrue(temp.root.listFiles()!!.any { it.name.startsWith("test.json.corrupt-") })

        drawer.save("name", "Ale")
        assertEquals("Ale", newDrawer().get<String>("name"))
    }

    @Test
    fun removedFieldsDoNotBreakOldData() {
        newDrawer().save("user", User("Ale", 22, listOf("x")))
        // UserV1 has no "tags" field: the old data still reads.
        assertEquals(UserV1("Ale", 22), newDrawer().get<UserV1>("user"))
    }

    @Test
    fun writesAreCoalesced() {
        val queued = mutableListOf<Runnable>()
        val drawer = Drawer("test", DrawerStorage({ file }, Executor { queued += it }))
        repeat(100) { drawer.save("count", it) }

        assertEquals(1, queued.size)
        queued.single().run()
        assertEquals(99, newDrawer().get<Int>("count"))
    }

    @Test
    fun saveDuringAWriteIsNotLost() {
        val queued = mutableListOf<Runnable>()
        val drawer = Drawer("test", DrawerStorage({ file }, Executor { queued += it }))
        drawer.save("a", 1)
        val first = queued.removeAt(0)
        first.run()
        drawer.save("b", 2)

        assertEquals(1, queued.size)
        queued.single().run()
        assertEquals(setOf("a", "b"), newDrawer().keys())
    }

    // endregion

    // region Lists

    @Test
    fun listAddRemoveAndEdit() {
        val notes = newDrawer().list<Note>("notes")
        assertTrue(notes.isEmpty())

        notes.add(Note("a"))
        notes.add(Note("b"))
        notes.addAll(listOf(Note("c"), Note("d")))
        notes.add(0, Note("first"))
        assertEquals(listOf("first", "a", "b", "c", "d"), notes.all().map { it.text })

        assertTrue(notes.remove(Note("a")))
        assertFalse(notes.remove(Note("missing")))
        assertEquals(Note("first"), notes.removeAt(0))
        notes[0] = Note("B")
        assertEquals(1, notes.updateIf({ it.text == "c" }) { it.copy(done = true) })
        assertEquals(listOf(Note("B"), Note("c", done = true), Note("d")), notes.all())

        assertEquals(2, notes.removeIf { !it.done })
        assertEquals(listOf(Note("c", done = true)), notes.all())
        assertEquals(1, notes.size)
        assertTrue(Note("c", done = true) in notes)

        notes.clear()
        assertTrue(notes.isEmpty())
        assertFalse("notes" in newDrawer())
    }

    @Test
    fun listsOfSimpleValues() {
        val tags = newDrawer().list<String>("tags")
        tags.add("android")
        tags.add("kotlin")
        assertEquals(listOf("android", "kotlin"), newDrawer().get<List<String>>("tags"))
    }

    @Test
    fun sameListIsEqual() {
        val drawer = newDrawer()
        assertEquals(drawer.list<Note>("notes"), drawer.list<Note>("notes"))
        assertEquals(drawer.list<Note>("notes").hashCode(), drawer.list<Note>("notes").hashCode())
        assertFalse(drawer.list<Note>("notes") == drawer.list<Note>("other"))
    }

    // endregion

    // region Flows

    @Test
    fun flowEmitsCurrentValueThenChanges() = runTest(UnconfinedTestDispatcher()) {
        val drawer = newDrawer()
        drawer.save("name", "A")

        val values = async { drawer.flow<String>("name").take(3).toList() }
        drawer.save("other", 1) // a different key doesn't emit
        drawer.save("name", "B")
        drawer.remove("name")

        assertEquals(listOf("A", "B", null), values.await())
    }

    @Test
    fun listFlowEmitsItems() = runTest(UnconfinedTestDispatcher()) {
        val notes = newDrawer().list<Note>("notes")
        val values = async { notes.flow().take(3).toList() }
        notes.add(Note("a"))
        notes.add(Note("b"))

        assertEquals(listOf(emptyList(), listOf(Note("a")), listOf(Note("a"), Note("b"))), values.await())
        assertEquals(listOf(Note("a"), Note("b")), notes.flow().first())
    }

    // endregion

    @Test
    fun drawerNamesAreValidated() {
        assertTrue(isValidDrawerName("settings"))
        assertTrue(isValidDrawerName("user_42-cache"))
        assertFalse(isValidDrawerName(""))
        assertFalse(isValidDrawerName("../secrets"))
        assertFalse(isValidDrawerName("my drawer"))
        assertThrows(IllegalArgumentException::class.java) { requireValidDrawerName("a/b") }
    }
}
