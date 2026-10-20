@file:Suppress("DEPRECATION")

package io.github.al3j4ndrolm.facilis.storage

import io.github.al3j4ndrolm.datadrawer.DataDrawer
import io.github.al3j4ndrolm.datadrawer.datadrawer
import org.junit.Assert.assertSame
import org.junit.Test

/** Code written for DataDrawer 1.0.0 must keep compiling and point at the same storage. */
class CompatTest {
    @Test
    fun oldNamesStillWork() {
        assertSame(FacilisStorage, datadrawer)
        val drawer: io.github.al3j4ndrolm.datadrawer.Drawer = storage
        val asOldType: DataDrawer = FacilisStorage
        assertSame(storage, drawer)
        assertSame(storage, asOldType)
    }
}
