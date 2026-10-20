@file:Suppress("unused")

package io.github.al3j4ndrolm.datadrawer

import io.github.al3j4ndrolm.facilis.storage.FacilisStorage

// DataDrawer was renamed to FacilisStorage in 1.1.0. These aliases keep 1.0.0 code compiling.

@Deprecated(
    "DataDrawer is now FacilisStorage.",
    ReplaceWith("storage", "io.github.al3j4ndrolm.facilis.storage.storage"),
)
val datadrawer: FacilisStorage get() = FacilisStorage

@Deprecated(
    "DataDrawer is now FacilisStorage.",
    ReplaceWith("FacilisStorage", "io.github.al3j4ndrolm.facilis.storage.FacilisStorage"),
)
typealias DataDrawer = FacilisStorage

@Deprecated(
    "Moved to io.github.al3j4ndrolm.facilis.storage.",
    ReplaceWith("Drawer", "io.github.al3j4ndrolm.facilis.storage.Drawer"),
)
typealias Drawer = io.github.al3j4ndrolm.facilis.storage.Drawer

@Deprecated(
    "Moved to io.github.al3j4ndrolm.facilis.storage.",
    ReplaceWith("DrawerList<T>", "io.github.al3j4ndrolm.facilis.storage.DrawerList"),
)
typealias DrawerList<T> = io.github.al3j4ndrolm.facilis.storage.DrawerList<T>
