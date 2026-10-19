package io.github.al3j4ndrolm.datadrawer

import android.content.Context
import androidx.startup.Initializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Reads and writes one drawer's JSON file. Writes are coalesced and run on [writer]. */
internal class DrawerStorage(
    private val file: () -> File,
    private val writer: Executor,
) {
    private val writePending = AtomicBoolean(false)

    fun read(): Map<String, JsonElement> {
        val target = file()
        if (!target.exists()) return emptyMap()
        return try {
            json.parseToJsonElement(target.readText()).jsonObject.toMap()
        } catch (e: Exception) {
            // Keep the unreadable file instead of overwriting it on the next save.
            val backup = File(target.parentFile, "${target.name}.corrupt-${System.currentTimeMillis()}")
            target.renameTo(backup)
            warn("Couldn't read ${target.name}; moved it to ${backup.name} and started empty.", e)
            emptyMap()
        }
    }

    /**
     * Writes [snapshot] to disk soon. Several saves in a row produce one write of the latest state.
     */
    fun scheduleWrite(snapshot: () -> Map<String, JsonElement>) {
        if (!writePending.compareAndSet(false, true)) return
        writer.execute {
            // Cleared before taking the snapshot, so a save that lands during the write schedules
            // another one instead of being lost.
            writePending.set(false)
            write(snapshot())
        }
    }

    private fun write(data: Map<String, JsonElement>) {
        val target = file()
        try {
            target.parentFile?.mkdirs()
            if (data.isEmpty()) {
                target.delete()
                return
            }
            // Write to a temporary file and rename it over the real one, so a crash mid-write
            // never leaves a half-written drawer.
            val temp = File(target.parentFile, "${target.name}.tmp")
            FileOutputStream(temp).use { out ->
                out.write(json.encodeToString(JsonObject.serializer(), JsonObject(data)).toByteArray())
                out.fd.sync()
            }
            if (!temp.renameTo(target)) {
                target.delete()
                check(temp.renameTo(target)) { "rename failed" }
            }
        } catch (e: Exception) {
            warn("Couldn't save ${target.name}.", e)
        }
    }

    companion object {
        fun newWriter(): Executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "DataDrawer-writer").apply { isDaemon = true }
        }
    }
}

internal fun warn(message: String, error: Throwable? = null) {
    try {
        android.util.Log.w("DataDrawer", message, error)
    } catch (_: Throwable) {
        // Not running on Android (unit tests).
        System.err.println("DataDrawer: $message ${error ?: ""}")
    }
}

/** Points DataDrawer at the app's files folder at startup, then loads the default drawer in the background. */
internal class DataDrawerInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Environment.install(context.applicationContext.filesDir)
        Environment.writer.execute { DataDrawer.preload() }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
