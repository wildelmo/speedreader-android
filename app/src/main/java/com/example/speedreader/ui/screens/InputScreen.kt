package com.example.speedreader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.speedreader.data.TextExtractor
import com.example.speedreader.ui.ReadingViewModel
import kotlinx.coroutines.launch

@Composable
fun InputScreen(
    viewModel: ReadingViewModel,
    onNavigateToReading: () -> Unit
) {
    var pastedText by remember { mutableStateOf("") }
    var pdfStartPage by remember { mutableStateOf("") }
    var pdfEndPage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val state by viewModel.state.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        val mimeType = context.contentResolver.getType(uri) ?: ""
                        val text = when {
                            mimeType.contains("pdf") || uri.toString().endsWith(".pdf", ignoreCase = true) -> {
                                val sPage = pdfStartPage.toIntOrNull()
                                val ePage = pdfEndPage.toIntOrNull()
                                TextExtractor.extractPdf(context, uri, sPage, ePage)
                            }
                            mimeType.contains("epub") || uri.toString().endsWith(".epub", ignoreCase = true) -> {
                                TextExtractor.extractEpub(context, uri)
                            }
                            else -> {
                                TextExtractor.extractTxt(context, uri)
                            }
                        }
                        
                        if (text.isBlank()) {
                            errorMessage = "No text extracted from file."
                        } else if (text.length > 2 * 1024 * 1024) {
                            errorMessage = "File text is too large (max 2MB chars)."
                        } else {
                            viewModel.loadText(text)
                            onNavigateToReading()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        errorMessage = "Error loading file: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Speed Reading App", style = MaterialTheme.typography.headlineMedium)
        
        OutlinedTextField(
            value = pastedText,
            onValueChange = { if (it.length <= 2_000_000) pastedText = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("Paste text here") }
        )
        
        Button(
            onClick = {
                if (pastedText.isNotBlank()) {
                    viewModel.loadText(pastedText)
                    onNavigateToReading()
                } else {
                    errorMessage = "Please paste some text."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Load Pasted Text")
        }
        
        Button(
            onClick = {
                try {
                    val demoText = context.resources.openRawResource(com.example.speedreader.R.raw.demo).bufferedReader().use { it.readText() }
                    viewModel.loadText(demoText)
                    onNavigateToReading()
                } catch (e: Exception) {
                    e.printStackTrace()
                    errorMessage = "Error loading demo text: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Demo")
        }
        
        HorizontalDivider()
        
        Text("Or load a file (TXT, PDF, EPUB)", style = MaterialTheme.typography.titleMedium)
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = pdfStartPage,
                onValueChange = { pdfStartPage = it },
                label = { Text("PDF Start Pg") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = pdfEndPage,
                onValueChange = { pdfEndPage = it },
                label = { Text("PDF End Pg") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        Button(
            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Loading..." else "Select File")
        }
        
        if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
