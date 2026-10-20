package io.github.al3j4ndrolm.facilis.storage

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Local storage with no setup. Available everywhere (Activity, Fragment, Compose, ViewModel):
 *
 * ```
 * storage.save("name", "Ale")
 * val name = storage.get<String>("name")
 * ```
 */
val storage: FacilisStorage get() = FacilisStorage

/**
 * The default drawer, and the way to open named ones. Everything a [Drawer] can do works here
 * directly; use [drawer] to keep unrelated data apart.
 */
object FacilisStorage : Drawer(
    name = DEFAULT_DRAWER,
    file = DrawerFile(path = { Environment.fileFor(DEFAULT_DRAWER) }, writer = Environment.writer),
) {
    private val named = ConcurrentHashMap<String, Drawer>()

    /**
     * Opens the drawer called [name], creating it on first use. Each drawer is stored in its own
     * file, so clearing one never touches the others.
     *
     * ```
     * val settings = storage.drawer("settings")
     * settings.save("darkMode", true)
     * ```
     *
     * @param name letters, digits, `_` and `-` only.
     */
    fun drawer(name: String): Drawer {
        requireValidDrawerName(name)
        if (name == DEFAULT_DRAWER) return this
        return named.getOrPut(name) {
            Drawer(name, DrawerFile(path = { Environment.fileFor(name) }, writer = Environment.writer))
        }
    }

    /** Names of every drawer that has been opened or has data on disk, including "default". */
    fun drawers(): Set<String> {
        val onDisk = Environment.directory.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.map { it.name.removeSuffix(".json") }
            .orEmpty()
        return (onDisk + named.keys + DEFAULT_DRAWER).filter { isValidDrawerName(it) }.toSortedSet()
    }

    internal fun preload() {
        keys()
    }
}

internal const val DEFAULT_DRAWER = "default"

private val DRAWER_NAME = Regex("[A-Za-z0-9_-]{1,64}")

internal fun isValidDrawerName(name: String) = DRAWER_NAME.matches(name)

internal fun requireValidDrawerName(name: String) {
    require(isValidDrawerName(name)) {
        "Invalid drawer name \"$name\": use 1-64 letters, digits, '_' or '-'."
    }
}

/** Where drawers live on disk. Set up automatically at app start by [FacilisStorageInitializer]. */
internal object Environment {
    @Volatile
    private var filesDir: File? = null

    val writer = DrawerFile.newWriter()

    val directory: File
        get() = File(
            filesDir ?: error(
                "FacilisStorage isn't initialized. Make sure androidx.startup's InitializationProvider " +
                    "isn't removed from your merged manifest."
            ),
            // Kept from the DataDrawer days so apps that upgrade keep their saved data.
            "datadrawer",
        )

    fun install(filesDir: File) {
        this.filesDir = filesDir
    }

    fun fileFor(drawer: String) = File(directory, "$drawer.json")
}
