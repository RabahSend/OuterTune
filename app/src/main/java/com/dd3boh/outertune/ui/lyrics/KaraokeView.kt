package com.dd3boh.outertune.ui.lyrics

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dd3boh.outertune.db.entities.LyricLine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

@Composable
fun KaraokeView(
    content: String,
    timingData: String?,
    currentPosition: Long,
    translations: Map<String, String>,
    selectedTranslation: String?,
    modifier: Modifier = Modifier
) {
    val lines = remember(timingData) {
        if (timingData != null) {
            val type = object : TypeToken<List<LyricLine>>() {}.type
            Gson().fromJson<List<LyricLine>>(timingData, type)
        } else {
            content.split("\n").map { text ->
                LyricLine(
                    startTime = 0,
                    endTime = 0,
                    text = text
                )
            }
        }
    }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Trouve la ligne active
    val activeLineIndex = remember(currentPosition, lines) {
        lines.indexOfLast { line ->
            currentPosition >= line.startTime && currentPosition <= line.endTime
        }.takeIf { it >= 0 }
    }
    
    // Défilement automatique
    LaunchedEffect(activeLineIndex) {
        activeLineIndex?.let { index ->
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index.coerceAtMost(lines.size - 1)
                )
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(lines) { line ->
                KaraokeLine(
                    line = line,
                    translation = selectedTranslation?.let { langCode ->
                        translations[langCode]?.split("\n")
                            ?.getOrNull(lines.indexOf(line))
                    },
                    isActive = lines.indexOf(line) == activeLineIndex,
                    progress = if (lines.indexOf(line) == activeLineIndex) {
                        ((currentPosition - line.startTime).toFloat() /
                                (line.endTime - line.startTime).toFloat())
                            .coerceIn(0f, 1f)
                    } else {
                        if (currentPosition > line.endTime) 1f else 0f
                    }
                )
            }
        }
    }
}

@Composable
private fun KaraokeLine(
    line: LyricLine,
    translation: String?,
    isActive: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        )
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Texte principal
        Text(
            text = buildKaraokeText(
                text = line.text,
                progress = if (isActive) animatedProgress else progress
            ),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            ),
            textAlign = TextAlign.Center
        )
        
        // Traduction
        translation?.let { translatedText ->
            Text(
                text = translatedText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        // Indicateur de progression
        if (isActive && line.startTime != line.endTime) {
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(2.dp)
                    .clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

private fun buildKaraokeText(
    text: String,
    progress: Float
): AnnotatedString {
    return buildAnnotatedString {
        val splitIndex = (text.length * progress).toInt()
        
        // Partie colorée (déjà chantée)
        append(
            AnnotatedString(
                text.take(splitIndex),
                SpanStyle(color = MaterialTheme.colorScheme.primary)
            )
        )
        
        // Partie non colorée (à venir)
        append(
            AnnotatedString(
                text.drop(splitIndex),
                SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        )
    }
} 