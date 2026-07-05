package com.boardmark.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val PlaceholderPalette = listOf(
    Color(0xFF1B6AC9), Color(0xFF2B6A8F), Color(0xFF4C5B92),
    Color(0xFF8E5CB0), Color(0xFF3E8E63), Color(0xFFC97A1B),
)

private fun colorForDomain(domain: String): Color {
    val index = abs(domain.hashCode()) % PlaceholderPalette.size
    return PlaceholderPalette[index]
}

@Composable
fun DomainPlaceholder(domain: String, modifier: Modifier = Modifier) {
    val initial = domain.trimStart().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorForDomain(domain)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = 32.sp,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
