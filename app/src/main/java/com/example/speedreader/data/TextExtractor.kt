package com.example.speedreader.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.util.zip.ZipInputStream

object TextExtractor {

    suspend fun extractTxt(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
    }

    suspend fun extractPdf(context: Context, uri: Uri, startPage: Int? = null, endPage: Int? = null): String {
        PDFBoxResourceLoader.init(context)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            if (startPage != null && startPage > 0) {
                stripper.startPage = startPage
            }
            if (endPage != null && endPage > 0) {
                stripper.endPage = endPage
            }
            val text = stripper.getText(document)
            document.close()
            return text
        }
        return ""
    }

    suspend fun extractEpub(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            extractEpubFromStream(inputStream)
        } ?: ""
    }

    private fun extractEpubFromStream(inputStream: InputStream): String {
        val zipIn = ZipInputStream(inputStream)
        val files = mutableMapOf<String, String>()

        var entry = zipIn.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val content = zipIn.readBytes().toString(Charsets.UTF_8)
                files[entry.name] = content
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        // 1. Find container.xml for OPF path
        val containerXml = files["META-INF/container.xml"] ?: return ""
        val opfPathMatch = Regex("full-path=\"([^\"]+\\.opf)\"").find(containerXml)
        val opfPath = opfPathMatch?.groupValues?.get(1) ?: return ""
        
        // 2. Read OPF
        val opfContent = files[opfPath] ?: return ""
        val opfDir = if (opfPath.contains('/')) opfPath.substringBeforeLast('/') + "/" else ""
        
        // 3. Extract Manifest (id -> href)
        val manifestItems = Regex("<item[^>]+id=\"([^\"]+)\"[^>]+href=\"([^\"]+)\"[^>]*>").findAll(opfContent)
        val itemHrefMap = mutableMapOf<String, String>()
        for (match in manifestItems) {
            val id = match.groupValues[1]
            val href = match.groupValues[2]
            itemHrefMap[id] = href
        }

        // 4. Extract Spine (idref order)
        val spineItems = Regex("<itemref[^>]+idref=\"([^\"]+)\"[^>]*>").findAll(opfContent)
        
        val extractedText = StringBuilder()
        for (match in spineItems) {
            val idref = match.groupValues[1]
            val href = itemHrefMap[idref]
            if (href != null) {
                val filePath = opfDir + href
                val fileContent = files[filePath]
                if (fileContent != null) {
                    // Extract text from body
                    val bodyContent = extractBodyText(fileContent)
                    extractedText.append(bodyContent).append("\\n")
                }
            }
        }
        
        return extractedText.toString()
    }

    private fun extractBodyText(html: String): String {
        val bodyMatch = Regex("<body[^>]*>(.*?)</body>", RegexOption.DOT_MATCHES_ALL).find(html)
        val content = bodyMatch?.groupValues?.get(1) ?: html
        
        // Strip HTML tags
        val noTags = content.replace(Regex("<[^>]*>"), " ")
        
        // Unescape basic HTML entities
        return noTags
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
