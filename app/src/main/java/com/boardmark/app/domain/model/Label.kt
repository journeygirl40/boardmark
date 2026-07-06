package com.boardmark.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Label(
    val id: Long,
    val name: String,
)
