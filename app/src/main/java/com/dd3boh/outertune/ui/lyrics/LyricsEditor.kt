package com.dd3boh.outertune.ui.lyrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dd3boh.outertune.db.entities.LyricsEntity
import com.dd3boh.outertune.db.entities.LyricLine
import com.dd3boh.outertune.db.entities.LyricsSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditor(
    lyrics: LyricsEntity?,
    currentPosition: Long,
    onLyricsSave: (LyricsEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var content by remember(lyrics) { mutableStateOf(lyrics?.content ?: "") }
    var lines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var editingLine by remember { mutableStateOf<Int?>(null) }
    
    val gson = remember { Gson() }
    
    LaunchedEffect(lyrics?.timingData) {
        lyrics?.timingData?.let {
            val type = object : TypeToken<List<LyricLine>>() {}.type
            lines = gson.fromJson(it, type)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Zone de texte pour les paroles
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Paroles") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            )
        )

        // Section de synchronisation
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Synchronisation",
                    style = MaterialTheme.typography.titleMedium
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lines) { line ->
                        LyricLineEditor(
                            line = line,
                            isEditing = editingLine == lines.indexOf(line),
                            currentPosition = currentPosition,
                            onStartEdit = { editingLine = lines.indexOf(line) },
                            onDelete = {
                                lines = lines - line
                            },
                            onUpdate = { updatedLine ->
                                lines = lines.toMutableList().apply {
                                    set(lines.indexOf(line), updatedLine)
                                }
                                editingLine = null
                            }
                        )
                    }
                }

                // Bouton pour ajouter un nouveau timestamp
                OutlinedButton(
                    onClick = {
                        lines = lines + LyricLine(
                            startTime = currentPosition,
                            endTime = currentPosition + 3000, // 3 secondes par défaut
                            text = ""
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ajouter un timestamp")
                }
            }
        }

        // Boutons de sauvegarde
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    onLyricsSave(
                        lyrics?.copy(
                            content = content,
                            timingData = gson.toJson(lines),
                            isSynced = lines.isNotEmpty(),
                            source = LyricsSource.USER
                        ) ?: LyricsEntity(
                            songId = "",  // À remplir par le ViewModel
                            languageCode = "fr", // À détecter ou sélectionner
                            content = content,
                            timingData = gson.toJson(lines),
                            isSynced = lines.isNotEmpty(),
                            source = LyricsSource.USER
                        )
                    )
                }
            ) {
                Text("Sauvegarder")
            }
        }
    }
}

@Composable
private fun LyricLineEditor(
    line: LyricLine,
    isEditing: Boolean,
    currentPosition: Long,
    onStartEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (LyricLine) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(line) { mutableStateOf(line.text) }
    var startTime by remember(line) { mutableStateOf(line.startTime) }
    var endTime by remember(line) { mutableStateOf(line.endTime) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Texte") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = (startTime / 1000f).toString(),
                        onValueChange = { startTime = (it.toFloatOrNull() ?: 0f).times(1000).toLong() },
                        label = { Text("Début (s)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = (endTime / 1000f).toString(),
                        onValueChange = { endTime = (it.toFloatOrNull() ?: 0f).times(1000).toLong() },
                        label = { Text("Fin (s)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            onUpdate(line.copy(
                                text = text,
                                startTime = startTime,
                                endTime = endTime
                            ))
                        }
                    ) {
                        Text("Valider")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = line.text)
                        Text(
                            text = "${line.startTime/1000}s - ${line.endTime/1000}s",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Row {
                        IconButton(onClick = onStartEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                        }
                    }
                }
            }
        }
    }
} 