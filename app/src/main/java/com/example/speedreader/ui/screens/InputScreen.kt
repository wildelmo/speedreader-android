package com.example.speedreader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.speedreader.data.TextExtractor
import com.example.speedreader.ui.ReadingViewModel
import com.example.speedreader.ui.theme.ZenAccentRed
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

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
            .safeDrawingPadding()
            .padding(horizontal = if (isLandscape) 24.dp else 16.dp)
            .padding(vertical = if (isLandscape) 8.dp else 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 16.dp)
    ) {
        // ZEN Speed Reader title with red "E" matching flash style
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Z",
                style = if (isLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                "E",
                style = if (isLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                color = ZenAccentRed,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                "N Speed Reader",
                style = if (isLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        
        OutlinedTextField(
            value = pastedText,
            onValueChange = { pastedText = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (isLandscape) 80.dp else 120.dp, max = if (isLandscape) 150.dp else 300.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isLandscape) 48.dp else 56.dp),
            contentPadding = PaddingValues(if (isLandscape) 8.dp else 16.dp)
        ) {
            Text("Load Pasted Text", style = MaterialTheme.typography.bodyMedium)
        }
        
        Button(
            onClick = {
                try {
                    val demoText = context.resources.openRawResource(com.example.speedreader.R.raw.demo).bufferedReader().use { it.readText() }
                    viewModel.loadText(demoText)
                    viewModel.configureDemoDefaults()
                    onNavigateToReading()
                } catch (e: Exception) {
                    e.printStackTrace()
                    errorMessage = "Error loading demo text: ${e.message}"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isLandscape) 48.dp else 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            contentPadding = PaddingValues(if (isLandscape) 8.dp else 16.dp)
        ) {
            Text("Demo", style = MaterialTheme.typography.bodyMedium)
        }
        
        HorizontalDivider()
        
        Text(
            "Or load a file (TXT, PDF, EPUB)",
            style = if (isLandscape) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium
        )
        
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
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isLandscape) 48.dp else 56.dp),
            enabled = !isLoading,
            contentPadding = PaddingValues(if (isLandscape) 8.dp else 16.dp)
        ) {
            Text(if (isLoading) "Loading..." else "Select File", style = MaterialTheme.typography.bodyMedium)
        }
        
        if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
