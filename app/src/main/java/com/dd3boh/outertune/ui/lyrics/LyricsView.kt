package com.dd3boh.outertune.ui.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dd3boh.outertune.db.entities.LyricsEntity
import com.dd3boh.outertune.db.entities.LyricLine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun LyricsView(
    lyrics: LyricsEntity,
    currentPosition: Long,
    translations: Map<String, String>?,
    onLineClick: (LyricLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val gson = remember { Gson() }
    val type = remember { object : TypeToken<List<LyricLine>>() {}.type }
    val timingData = remember(lyrics.timingData) {
        lyrics.timingData?.let { gson.fromJson<List<LyricLine>>(it, type) }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (timingData != null) {
            items(timingData) { line ->
                LyricLineView(
                    line = line,
                    translation = translations?.get(line.text),
                    isActive = currentPosition in line.startTime..line.endTime,
                    onClick = { onLineClick(line) }
                )
            }
        } else {
            item {
                Text(
                    text = lyrics.content,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (translations != null && translations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = translations.values.first(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricLineView(
    line: LyricLine,
    translation: String?,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        if (translation != null) {
            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
} 