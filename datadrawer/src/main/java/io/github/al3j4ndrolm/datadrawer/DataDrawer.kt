package io.github.al3j4ndrolm.datadrawer

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Local storage with no setup. Available everywhere (Activity, Fragment, Compose, ViewModel):
 *
 * ```
 * datadrawer.save("name", "Ale")
 * val name = datadrawer.get<String>("name")
 * ```
 */
val datadrawer: DataDrawer get() = DataDrawer

/**
 * The default drawer, and the way to open named ones. Everything a [Drawer] can do works here
 * directly; use [drawer] to keep unrelated data apart.
 */
object DataDrawer : Drawer(
    name = DEFAULT_DRAWER,
    storage = DrawerStorage(file = { Environment.fileFor(DEFAULT_DRAWER) }, writer = Environment.writer),
) {
    private val named = ConcurrentHashMap<String, Drawer>()

    /**
     * Opens the drawer called [name], creating it on first use. Each drawer is stored in its own
     * file, so clearing one never touches the others.
     *
     * ```
     * val settings = datadrawer.drawer("settings")
     * settings.save("darkMode", true)
     * ```
     *
     * @param name letters, digits, `_` and `-` only.
     */
    fun drawer(name: String): Drawer {
        requireValidDrawerName(name)
        if (name == DEFAULT_DRAWER) return this
        return named.getOrPut(name) {
            Drawer(name, DrawerStorage(file = { Environment.fileFor(name) }, writer = Environment.writer))
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

/** Where drawers live on disk. Set up automatically at app start by [DataDrawerInitializer]. */
internal object Environment {
    @Volatile
    private var filesDir: File? = null

    val writer = DrawerStorage.newWriter()

    val directory: File
        get() = File(
            filesDir ?: error(
                "DataDrawer isn't initialized. Make sure androidx.startup's InitializationProvider " +
                    "isn't removed from your merged manifest."
            ),
            "datadrawer",
        )

    fun install(filesDir: File) {
        this.filesDir = filesDir
    }

    fun fileFor(drawer: String) = File(directory, "$drawer.json")
}
