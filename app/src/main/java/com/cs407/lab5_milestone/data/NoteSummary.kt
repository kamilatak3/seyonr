package com.cs407.lab5_milestone

import java.util.Date

data class NoteSummary(
    val noteId: Int,
    val noteTitle: String,
    val noteAbstract: String,
    val lastEdited: Date
)
