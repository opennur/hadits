package org.opennur.hadits.model

data class Book(
    val id: String,
    val name: String,
    val available: Int,
)

data class Hadith(
    val id: String,
    val bookId: String,
    val bookName: String,
    val number: Int,
    val arabic: String,
    val translation: String,
    val isFavorite: Boolean = false,
)
