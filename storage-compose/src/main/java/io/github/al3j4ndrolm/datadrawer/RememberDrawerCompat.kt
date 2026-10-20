@file:Suppress("unused", "DEPRECATION")

package io.github.al3j4ndrolm.datadrawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import io.github.al3j4ndrolm.facilis.storage.FacilisStorage
import io.github.al3j4ndrolm.facilis.storage.rememberStorage
import kotlinx.serialization.KSerializer
import io.github.al3j4ndrolm.facilis.storage.collectAsState as storageCollectAsState

// DataDrawer was renamed to FacilisStorage in 1.1.0. These aliases keep 1.0.0 code compiling.

@Deprecated(
    "DataDrawer is now FacilisStorage.",
    ReplaceWith("rememberStorage(key, default, drawer)", "io.github.al3j4ndrolm.facilis.storage.rememberStorage"),
)
@Composable
inline fun <reified T> rememberDrawer(
    key: String,
    default: T,
    drawer: Drawer = FacilisStorage,
): MutableState<T> = rememberStorage(key, default, drawer)

@Deprecated(
    "DataDrawer is now FacilisStorage.",
    ReplaceWith("rememberStorage(key, default, serializer, drawer)", "io.github.al3j4ndrolm.facilis.storage.rememberStorage"),
)
@Composable
fun <T> rememberDrawer(
    key: String,
    default: T,
    serializer: KSerializer<T>,
    drawer: Drawer = FacilisStorage,
): MutableState<T> = rememberStorage(key, default, serializer, drawer)

@Deprecated(
    "Moved to io.github.al3j4ndrolm.facilis.storage.",
    ReplaceWith("collectAsState()", "io.github.al3j4ndrolm.facilis.storage.collectAsState"),
)
@Composable
fun <T> DrawerList<T>.collectAsState(): State<List<T>> = storageCollectAsState()
