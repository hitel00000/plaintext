package com.plaintext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
    var documentText by rememberSaveable { mutableStateOf("") }
    val fileName by remember { mutableStateOf("Untitled.txt") }

    Scaffold(modifier = modifier) { innerPadding ->
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
                Button(onClick = { /* Storage Access Framework open flow will be added next. */ }) {
                    Text("Open")
                }
                Button(onClick = { /* Storage Access Framework save flow will be added next. */ }) {
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

@Preview(showBackground = true)
@Composable
private fun EditorScreenPreview() {
    PlainTextApp()
}
