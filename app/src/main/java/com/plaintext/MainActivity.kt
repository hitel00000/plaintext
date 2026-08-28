package com.plaintext

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlainTextApp()
        }
    }
}

@Composable
private fun PlainTextApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            EditorScreen()
        }
    }
}

@Composable
private fun EditorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var documentText by rememberSaveable { mutableStateOf("") }
    var currentFileUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var fileName by rememberSaveable { mutableStateOf("Untitled.txt") }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = readTextFromUri(contentResolver, uri)
                    val name = queryDisplayName(contentResolver, uri) ?: "Document.txt"
                    documentText = content
                    currentFileUri = uri
                    fileName = name
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = "Failed to open file: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    writeTextToUri(contentResolver, uri, documentText)
                    val name = queryDisplayName(contentResolver, uri) ?: fileName
                    currentFileUri = uri
                    fileName = name
                    snackbarHostState.showSnackbar("Saved $name")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = "Failed to save file: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun saveDocument() {
        val uri = currentFileUri
        if (uri != null) {
            coroutineScope.launch {
                try {
                    writeTextToUri(contentResolver, uri, documentText)
                    snackbarHostState.showSnackbar("Saved $fileName")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = "Failed to save file: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        } else {
            createDocumentLauncher.launch(fileName.ifBlank { "Untitled.txt" })
        }
    }

    fun createNewDocument() {
        documentText = ""
        currentFileUri = null
        fileName = "Untitled.txt"
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { createNewDocument() }) {
                    Text("New")
                }
                Button(onClick = { openDocumentLauncher.launch(arrayOf("text/*", "*/*")) }) {
                    Text("Open")
                }
                Button(onClick = { saveDocument() }) {
                    Text("Save")
                }
            }

            Text(text = fileName, style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = documentText,
                onValueChange = { documentText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Start typing…") },
            )
        }
    }
}

private suspend fun readTextFromUri(contentResolver: ContentResolver, uri: Uri): String =
    withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalStateException("Unable to open input stream")
    }

private suspend fun writeTextToUri(contentResolver: ContentResolver, uri: Uri, text: String): Unit =
    withContext(Dispatchers.IO) {
        contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(text)
                writer.flush()
            }
        } ?: throw IllegalStateException("Unable to open output stream")
    }

private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
    return try {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) cursor.getString(index) else null
            } else null
        }
    } catch (_: Exception) {
        null
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorScreenPreview() {
    PlainTextApp()
}
