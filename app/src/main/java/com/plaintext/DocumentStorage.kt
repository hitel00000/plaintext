package com.plaintext

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object DocumentStorage {
    const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10MB limit

    fun sanitizeBom(text: String): String {
        return if (text.startsWith("\uFEFF")) {
            text.substring(1)
        } else {
            text
        }
    }

    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    fun countCharacters(text: String): Int {
        return text.length
    }

    suspend fun readTextFromUri(contentResolver: ContentResolver, uri: Uri): String =
        withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArrayOutputStream()
                val data = ByteArray(8192)
                var totalBytes = 0L
                var bytesRead: Int

                while (inputStream.read(data).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    if (totalBytes > MAX_FILE_SIZE_BYTES) {
                        throw IllegalStateException("File exceeds maximum supported size (10 MB)")
                    }
                    buffer.write(data, 0, bytesRead)
                }

                val bytes = buffer.toByteArray()
                decodeBytesToText(bytes)
            } ?: throw IllegalStateException("Unable to open input stream for URI")
        }

    fun getMimeTypeForFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) return "text/plain"

        val mimeFromMap = try {
            android.webkit.MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(extension)
        } catch (_: Exception) {
            null
        }
        if (!mimeFromMap.isNullOrBlank()) {
            return mimeFromMap
        }

        return when (extension) {
            "md", "markdown" -> "text/markdown"
            "json" -> "application/json"
            "yaml", "yml" -> "text/yaml"
            "xml" -> "text/xml"
            "csv" -> "text/csv"
            "tsv" -> "text/tab-separated-values"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "text/javascript"
            "log", "txt", "conf", "ini", "properties" -> "text/plain"
            else -> "*/*"
        }
    }

    suspend fun writeTextToUri(contentResolver: ContentResolver, uri: Uri, text: String): Unit =
        withContext(Dispatchers.IO) {
            val outputStream = try {
                contentResolver.openOutputStream(uri, "wt")
            } catch (e: SecurityException) {
                // Fast-fail on permission denial: avoid slow retries across IPC
                throw e
            } catch (_: Exception) {
                try {
                    contentResolver.openOutputStream(uri, "w")
                } catch (e: SecurityException) {
                    throw e
                } catch (_: Exception) {
                    contentResolver.openOutputStream(uri)
                }
            } ?: throw IllegalStateException("Unable to open output stream (read-only or restricted URI)")

            outputStream.use { stream ->
                stream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                    writer.write(text)
                    writer.flush()
                }
            }
        }

    fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
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

    internal fun decodeBytesToText(bytes: ByteArray): String {
        // Try UTF-8 with BOM check
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }

        // Try standard UTF-8
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
            decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } catch (_: Exception) {
            // Fallback to platform default / ISO-8859-1 if UTF-8 decoding fails
            try {
                String(bytes, Charset.defaultCharset())
            } catch (_: Exception) {
                String(bytes, StandardCharsets.ISO_8859_1)
            }
        }
    }
}
