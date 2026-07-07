package com.wirewaypro.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An in-progress quote-builder form, autosaved on every change so a crash or an
 * app kill never loses an estimate the contractor was midway through typing.
 *
 * Distinct from [QuoteEntity]: a draft is the UNSAVED editor state (including
 * half-typed numeric text), not a committed quote. It's keyed by [draftKey] —
 * the quote id when editing, or a "new-estimate" / "new-invoice" sentinel for a
 * fresh quote — so reopening the same builder restores exactly where the user
 * left off. The draft is deleted once the quote is successfully saved.
 *
 * [contentJson] holds the serialized form so the wide, evolving set of builder
 * fields lives in one column instead of a brittle one-column-per-field table.
 */
@Entity(tableName = "quote_drafts")
data class QuoteDraftEntity(
    @PrimaryKey val draftKey: String,
    val contentJson: String,
    val updatedAt: Long,
)
