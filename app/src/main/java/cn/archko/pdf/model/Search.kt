package cn.archko.pdf.model

import androidx.compose.runtime.Immutable

@Immutable
data class SearchSuggestionGroup(
    val id: Long,
    val name: String,
    val suggestions: List<String>
)
