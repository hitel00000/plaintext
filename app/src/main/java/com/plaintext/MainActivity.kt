package com.plaintext

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var externalUriState by mutableStateOf<Uri?>(null)
    private var sharedTextState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            PlainTextApp(
                externalUri = externalUriState,
                sharedText = sharedTextState,
                onExternalUriConsumed = { externalUriState = null },
                onSharedTextConsumed = { sharedTextState = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            sharedTextState = intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            val uri = intent.data
            if (uri != null) {
                tryPersistUriPermission(uri, intent.flags)
                externalUriState = uri
            }
        }
    }

    private fun tryPersistUriPermission(uri: Uri, flags: Int) {
        try {
            val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (takeFlags != 0) {
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        } catch (_: Exception) {
            // Non-persistable URIs (such as temporary FileProvider URIs) do not support persistable permissions.
        }
    }
}

@Composable
private fun PlainTextApp(
    externalUri: Uri? = null,
    sharedText: String? = null,
    onExternalUriConsumed: () -> Unit = {},
    onSharedTextConsumed: () -> Unit = {}
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            EditorScreen(
                externalUri = externalUri,
                sharedText = sharedText,
                onExternalUriConsumed = onExternalUriConsumed,
                onSharedTextConsumed = onSharedTextConsumed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(
    modifier: Modifier = Modifier,
    externalUri: Uri? = null,
    sharedText: String? = null,
    onExternalUriConsumed: () -> Unit = {},
    onSharedTextConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val horizontalScrollState = rememberScrollState()

    var documentText by rememberSaveable { mutableStateOf("") }
    var lastSavedText by rememberSaveable { mutableStateOf("") }
    var currentFileUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var fileName by rememberSaveable { mutableStateOf("Untitled.txt") }
    var isMonospace by rememberSaveable { mutableStateOf(false) }
    var isWordWrap by rememberSaveable { mutableStateOf(true) }

    var showMenu by remember { mutableStateOf(false) }
    var showNewConfirmDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val isModified = documentText != lastSavedText
    val wordCount by remember { derivedStateOf { DocumentStorage.countWords(documentText) } }
    val charCount by remember { derivedStateOf { DocumentStorage.countCharacters(documentText) } }

    BackHandler(enabled = isModified) {
        showExitConfirmDialog = true
    }

    fun loadFromUri(uri: Uri) {
        coroutineScope.launch {
            try {
                val content = DocumentStorage.readTextFromUri(contentResolver, uri)
                val name = DocumentStorage.queryDisplayName(contentResolver, uri) ?: "Document.txt"
                documentText = content
                lastSavedText = content
                currentFileUri = uri
                fileName = name
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Failed to open file: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    LaunchedEffect(externalUri) {
        if (externalUri != null) {
            loadFromUri(externalUri)
            onExternalUriConsumed()
        }
    }

    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            documentText = sharedText
            lastSavedText = ""
            currentFileUri = null
            fileName = "Shared.txt"
            onSharedTextConsumed()
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            loadFromUri(uri)
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            coroutineScope.launch {
                try {
                    DocumentStorage.writeTextToUri(contentResolver, uri, documentText)
                    val name = DocumentStorage.queryDisplayName(contentResolver, uri) ?: fileName
                    lastSavedText = documentText
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
                    DocumentStorage.writeTextToUri(contentResolver, uri, documentText)
                    lastSavedText = documentText
                    snackbarHostState.showSnackbar("Saved $fileName")
                } catch (e: Exception) {
                    // Fallback to CreateDocument if writing to existing URI fails (e.g. read-only external file)
                    snackbarHostState.showSnackbar(
                        message = "File is read-only. Please choose a location to save a copy."
                    )
                    createDocumentLauncher.launch(fileName.ifBlank { "Untitled.txt" })
                }
            }
        } else {
            createDocumentLauncher.launch(fileName.ifBlank { "Untitled.txt" })
        }
    }

    fun saveAsDocument() {
        createDocumentLauncher.launch(fileName.ifBlank { "Untitled.txt" })
    }

    fun shareCurrentDocument() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, documentText)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
        }
        val shareChooser = Intent.createChooser(sendIntent, "Share text via")
        context.startActivity(shareChooser)
    }

    fun performNewDocument() {
        documentText = ""
        lastSavedText = ""
        currentFileUri = null
        fileName = "Untitled.txt"
    }

    fun onNewClicked() {
        if (isModified) {
            showNewConfirmDialog = true
        } else {
            performNewDocument()
        }
    }

    if (showNewConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showNewConfirmDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Creating a new document will discard them.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNewConfirmDialog = false
                        performNewDocument()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("Exit without saving?") },
            text = { Text("You have unsaved changes. Exiting now will discard them.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        (context as? Activity)?.finish()
                    }
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val displayName = if (isModified) "$fileName •" else fileName

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                actions = {
                    IconButton(onClick = { onNewClicked() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Document"
                        )
                    }
                    IconButton(onClick = { openDocumentLauncher.launch(arrayOf("text/*", "*/*")) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_folder_open),
                            contentDescription = "Open Document"
                        )
                    }
                    IconButton(onClick = { saveDocument() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save),
                            contentDescription = "Save Document"
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save As…") },
                                onClick = {
                                    showMenu = false
                                    saveAsDocument()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share…") },
                                onClick = {
                                    showMenu = false
                                    shareCurrentDocument()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isMonospace) "Default Font" else "Monospace Font") },
                                onClick = {
                                    showMenu = false
                                    isMonospace = !isMonospace
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isWordWrap) "Disable Word Wrap" else "Enable Word Wrap") },
                                onClick = {
                                    showMenu = false
                                    isWordWrap = !isWordWrap
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            TextField(
                value = documentText,
                onValueChange = { documentText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .then(
                        if (!isWordWrap) {
                            Modifier.horizontalScroll(horizontalScrollState)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 4.dp),
                placeholder = {
                    Text(
                        text = "Start typing…",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            // Minimal bottom status info (word & character count)
            Text(
                text = "$wordCount words · $charCount chars",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorScreenPreview() {
    PlainTextApp()
}
