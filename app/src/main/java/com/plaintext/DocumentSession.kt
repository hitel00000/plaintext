package com.plaintext

import android.net.Uri
import android.os.Parcelable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class DocumentSession(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val lastSavedText: String = "",
    val currentFileUri: Uri? = null,
    val fileName: String = "Untitled.txt"
) : Parcelable {
    val isModified: Boolean
        get() = text != lastSavedText

    fun toTextFieldValue(): TextFieldValue =
        TextFieldValue(
            text = text,
            selection = TextRange(
                selectionStart.coerceIn(0, text.length),
                selectionEnd.coerceIn(0, text.length)
            )
        )

    companion object {
        fun createNew(title: String = "Untitled.txt"): DocumentSession =
            DocumentSession(
                id = UUID.randomUUID().toString(),
                text = "",
                selectionStart = 0,
                selectionEnd = 0,
                lastSavedText = "",
                currentFileUri = null,
                fileName = title
            )
    }
}
