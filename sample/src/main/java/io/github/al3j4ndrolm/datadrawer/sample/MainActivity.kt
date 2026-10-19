package io.github.al3j4ndrolm.datadrawer.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.al3j4ndrolm.datadrawer.collectAsState
import io.github.al3j4ndrolm.datadrawer.datadrawer
import io.github.al3j4ndrolm.datadrawer.rememberDrawer
import kotlinx.serialization.Serializable

@Serializable
data class Profile(val name: String = "", val age: Int = 0)

@Serializable
data class Note(val text: String, val done: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Plain values from anywhere, no setup.
        if (savedInstanceState == null) datadrawer.save("opens", datadrawer.get("opens", 0) + 1)

        enableEdgeToEdge()
        setContent {
            // A separate drawer for settings.
            val darkMode by rememberDrawer("darkMode", false, drawer = datadrawer.drawer("settings"))
            MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { SampleScreen() }
            }
        }
    }
}

@Composable
private fun SampleScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val opens by rememberDrawer("opens", 0)
        Text("DataDrawer", style = MaterialTheme.typography.headlineMedium)
        Text("You've opened this app $opens times. Close it and come back: everything below is still here.")

        HorizontalDivider()
        Section("Values")
        var name by rememberDrawer("name", default = "")
        OutlinedTextField(name, { name = it }, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth())

        HorizontalDivider()
        Section("Objects")
        var profile by rememberDrawer("profile", Profile())
        OutlinedTextField(
            value = profile.name,
            onValueChange = { profile = profile.copy(name = it) },
            label = { Text("Profile name") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Age: ${profile.age}")
            Button(onClick = { profile = profile.copy(age = profile.age + 1) }) { Text("+1") }
        }

        HorizontalDivider()
        Section("Lists")
        val notes = datadrawer.list<Note>("notes")
        val items by notes.collectAsState()
        var draft by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(draft, { draft = it }, label = { Text("New note") }, modifier = Modifier.weight(1f))
            Button(onClick = { notes.add(Note(draft.trim())); draft = "" }, enabled = draft.isNotBlank()) { Text("Add") }
        }
        items.forEach { note ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = note.done,
                    onCheckedChange = { done -> notes.updateIf({ it == note }) { it.copy(done = done) } },
                )
                Text(
                    note.text,
                    modifier = Modifier.weight(1f),
                    textDecoration = if (note.done) TextDecoration.LineThrough else null,
                )
                TextButton(onClick = { notes.remove(note) }) { Text("Delete") }
            }
        }

        HorizontalDivider()
        Section("Drawers")
        var darkMode by rememberDrawer("darkMode", false, drawer = datadrawer.drawer("settings"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dark mode (saved in the \"settings\" drawer)", modifier = Modifier.weight(1f))
            Switch(checked = darkMode, onCheckedChange = { darkMode = it })
        }
        TextButton(onClick = { datadrawer.clear() }) { Text("Clear the default drawer") }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}
