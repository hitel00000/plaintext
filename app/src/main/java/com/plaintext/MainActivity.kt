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
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.UUID

data class CreateDocumentRequest(
    val fileName: String,
    val mimeType: String = DocumentStorage.getMimeTypeForFileName(fileName)
)

class DynamicCreateDocumentContract : ActivityResultContract<CreateDocumentRequest, Uri?>() {
    override fun createIntent(context: Context, input: CreateDocumentRequest): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
    }
}

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
        val wantFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (wantFlags == 0) return
        try {
            contentResolver.takePersistableUriPermission(uri, wantFlags)
            return
        } catch (_: Exception) {}

        if ((wantFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
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
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    var sessions by rememberSaveable {
        mutableStateOf(listOf(DocumentSession.createNew("Untitled.txt")))
    }
    var activeIndex by rememberSaveable { mutableIntStateOf(0) }

    val safeIndex = activeIndex.coerceIn(0, (sessions.size - 1).coerceAtLeast(0))
    val currentSession = if (sessions.isNotEmpty()) sessions[safeIndex] else DocumentSession.createNew()

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(currentSession.toTextFieldValue())
    }
    var isMonospace by rememberSaveable { mutableStateOf(false) }
    var isWordWrap by rememberSaveable { mutableStateOf(true) }

    var showMenu by remember { mutableStateOf(false) }
    var showSwitcherSheet by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var sessionToCloseIndex by remember { mutableStateOf<Int?>(null) }

    fun syncCurrentSessionToState(value: TextFieldValue = textFieldValue) {
        if (sessions.isNotEmpty() && safeIndex in sessions.indices) {
            val updated = sessions.toMutableList()
            updated[safeIndex] = updated[safeIndex].copy(
                text = value.text,
                selectionStart = value.selection.start,
                selectionEnd = value.selection.end
            )
            sessions = updated
        }
    }

    fun switchToSession(index: Int) {
        syncCurrentSessionToState()
        val targetIndex = index.coerceIn(0, sessions.size - 1)
        activeIndex = targetIndex
        val targetSession = sessions[targetIndex]
        textFieldValue = targetSession.toTextFieldValue()
        showSwitcherSheet = false
    }

    fun createNewDocument() {
        syncCurrentSessionToState()
        val newTitle = "Untitled ${sessions.size + 1}.txt"
        val newSession = DocumentSession.createNew(newTitle)
        sessions = sessions + newSession
        activeIndex = sessions.size - 1
        textFieldValue = newSession.toTextFieldValue()
        showSwitcherSheet = false
    }

    fun closeSession(indexToClose: Int, force: Boolean = false) {
        if (indexToClose !in sessions.indices) return
        val target = sessions[indexToClose]
        if (!force && target.isModified) {
            sessionToCloseIndex = indexToClose
            return
        }

        val updated = sessions.toMutableList()
        updated.removeAt(indexToClose)

        if (updated.isEmpty()) {
            val fresh = DocumentSession.createNew("Untitled.txt")
            sessions = listOf(fresh)
            activeIndex = 0
            textFieldValue = fresh.toTextFieldValue()
        } else {
            sessions = updated
            val newActive = when {
                safeIndex >= updated.size -> updated.size - 1
                indexToClose < safeIndex -> safeIndex - 1
                else -> safeIndex
            }
            activeIndex = newActive
            textFieldValue = updated[newActive].toTextFieldValue()
        }
    }

    fun loadFromUri(uri: Uri) {
        syncCurrentSessionToState()
        val existingIndex = sessions.indexOfFirst { it.currentFileUri == uri }
        if (existingIndex != -1) {
            switchToSession(existingIndex)
            return
        }

        coroutineScope.launch {
            try {
                val content = DocumentStorage.readTextFromUri(contentResolver, uri)
                val name = DocumentStorage.queryDisplayName(contentResolver, uri) ?: "Document.txt"
                val newSession = DocumentSession(
                    text = content,
                    selectionStart = 0,
                    selectionEnd = 0,
                    lastSavedText = content,
                    currentFileUri = uri,
                    fileName = name
                )

                // Replace if currently single pristine empty untitled file
                if (sessions.size == 1 && currentSession.currentFileUri == null && currentSession.text.isEmpty()) {
                    sessions = listOf(newSession)
                    activeIndex = 0
                    textFieldValue = newSession.toTextFieldValue()
                } else {
                    sessions = sessions + newSession
                    activeIndex = sessions.size - 1
                    textFieldValue = newSession.toTextFieldValue()
                }
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
            syncCurrentSessionToState()
            val newSession = DocumentSession(
                text = sharedText,
                selectionStart = 0,
                selectionEnd = 0,
                lastSavedText = "",
                currentFileUri = null,
                fileName = "Shared.txt"
            )
            sessions = sessions + newSession
            activeIndex = sessions.size - 1
            textFieldValue = newSession.toTextFieldValue()
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
            } catch (_: Exception) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }
            loadFromUri(uri)
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = DynamicCreateDocumentContract()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }
            coroutineScope.launch {
                try {
                    DocumentStorage.writeTextToUri(contentResolver, uri, textFieldValue.text)
                    val name = DocumentStorage.queryDisplayName(contentResolver, uri) ?: currentSession.fileName
                    val targetSessionId = currentSession.id
                    val idx = sessions.indexOfFirst { it.id == targetSessionId }
                    val targetIdx = if (idx != -1) idx else safeIndex
                    val updated = sessions.toMutableList()
                    updated[targetIdx] = updated[targetIdx].copy(
                        lastSavedText = textFieldValue.text,
                        currentFileUri = uri,
                        fileName = name
                    )
                    sessions = updated
                    snackbarHostState.showSnackbar("Saved $name")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = "Failed to save file: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun launchSaveAs(fileName: String) {
        val safeName = fileName.ifBlank { "Untitled.txt" }
        val mimeType = DocumentStorage.getMimeTypeForFileName(safeName)
        createDocumentLauncher.launch(CreateDocumentRequest(fileName = safeName, mimeType = mimeType))
    }

    fun saveDocument() {
        val uri = currentSession.currentFileUri
        val targetSessionId = currentSession.id
        if (uri != null) {
            coroutineScope.launch {
                try {
                    DocumentStorage.writeTextToUri(contentResolver, uri, textFieldValue.text)
                    val idx = sessions.indexOfFirst { it.id == targetSessionId }
                    val targetIdx = if (idx != -1) idx else safeIndex
                    val updated = sessions.toMutableList()
                    updated[targetIdx] = updated[targetIdx].copy(
                        lastSavedText = textFieldValue.text
                    )
                    sessions = updated
                    snackbarHostState.showSnackbar("Saved ${currentSession.fileName}")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = "File is read-only. Please choose a location to save a copy."
                    )
                    launchSaveAs(currentSession.fileName)
                }
            }
        } else {
            launchSaveAs(currentSession.fileName)
        }
    }

    fun saveAsDocument() {
        launchSaveAs(currentSession.fileName)
    }

    fun shareCurrentDocument() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textFieldValue.text)
            putExtra(Intent.EXTRA_SUBJECT, currentSession.fileName)
        }
        val shareChooser = Intent.createChooser(sendIntent, "Share text via")
        context.startActivity(shareChooser)
    }

    val documentText = textFieldValue.text
    val isModified = documentText != currentSession.lastSavedText
    val wordCount = DocumentStorage.countWords(documentText)
    val charCount = DocumentStorage.countCharacters(documentText)

    BackHandler(enabled = isModified || sessions.size > 1) {
        if (isModified) {
            showExitConfirmDialog = true
        } else if (sessions.size > 1) {
            showSwitcherSheet = true
        }
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("Exit without saving?") },
            text = { Text("You have unsaved changes in '${currentSession.fileName}'. Exiting now will discard them.") },
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

    sessionToCloseIndex?.let { closeIdx ->
        val doc = sessions.getOrNull(closeIdx)
        if (doc != null) {
            AlertDialog(
                onDismissRequest = { sessionToCloseIndex = null },
                title = { Text("Discard '${doc.fileName}'?") },
                text = { Text("This document has unsaved changes. Closing it will discard them.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            sessionToCloseIndex = null
                            closeSession(closeIdx, force = true)
                        }
                    ) {
                        Text("Discard & Close")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToCloseIndex = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    val sheetState = rememberModalBottomSheetState()
    if (showSwitcherSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSwitcherSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open Documents (${sessions.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { createNewDocument() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Document",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    itemsIndexed(sessions, key = { _, s -> s.id }) { idx, session ->
                        val isSelected = idx == safeIndex
                        val sessionModified = if (isSelected) isModified else session.isModified
                        val sessionWords = if (isSelected) wordCount else DocumentStorage.countWords(session.text)
                        val sessionChars = if (isSelected) charCount else DocumentStorage.countCharacters(session.text)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { switchToSession(idx) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (sessionModified) "${session.fileName} •" else session.fileName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$sessionWords words · $sessionChars chars",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }

                                IconButton(
                                    onClick = { closeSession(idx) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Document",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val displayName = if (isModified) "${currentSession.fileName} •" else currentSession.fileName

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSwitcherSheet = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Document",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { createNewDocument() }) {
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
            val scrollbarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

            BasicTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    syncCurrentSessionToState(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .drawScrollbar(verticalScrollState, color = scrollbarColor)
                    .verticalScroll(verticalScrollState)
                    .then(
                        if (!isWordWrap) {
                            Modifier.horizontalScroll(horizontalScrollState)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (documentText.isEmpty()) {
                            Text(
                                text = "Start typing…",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
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

private fun Modifier.drawScrollbar(
    state: ScrollState,
    color: Color,
    thickness: Dp = 4.dp,
    padding: Dp = 2.dp
): Modifier = this.drawWithContent {
    drawContent()

    val totalLength = state.maxValue.toFloat()
    if (totalLength > 0) {
        val viewportLength = size.height
        val contentHeight = totalLength + viewportLength
        val thumbHeight = ((viewportLength / contentHeight) * viewportLength).coerceAtLeast(32.dp.toPx())
        val scrollProgress = state.value.toFloat() / totalLength
        val thumbOffset = scrollProgress * (viewportLength - thumbHeight)

        val thicknessPx = thickness.toPx()
        val paddingPx = padding.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(
                x = size.width - thicknessPx - paddingPx,
                y = thumbOffset
            ),
            size = Size(
                width = thicknessPx,
                height = thumbHeight
            ),
            cornerRadius = CornerRadius(thicknessPx / 2, thicknessPx / 2)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorScreenPreview() {
    PlainTextApp()
}
