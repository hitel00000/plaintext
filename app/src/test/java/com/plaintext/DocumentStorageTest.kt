package com.plaintext

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.StandardCharsets

class DocumentStorageTest {

    @Test
    fun sanitizeBom_removesBomPrefix() {
        val withBom = "\uFEFFHello World"
        val withoutBom = "Hello World"

        assertEquals("Hello World", DocumentStorage.sanitizeBom(withBom))
        assertEquals("Hello World", DocumentStorage.sanitizeBom(withoutBom))
    }

    @Test
    fun countWords_calculatesCorrectly() {
        assertEquals(0, DocumentStorage.countWords(""))
        assertEquals(0, DocumentStorage.countWords("   \n\t  "))
        assertEquals(2, DocumentStorage.countWords("Hello World"))
        assertEquals(3, DocumentStorage.countWords("  Kotlin   Jetpack   Compose  "))
        assertEquals(3, DocumentStorage.countWords("안녕하세요 텍스트 에디터"))
    }

    @Test
    fun countCharacters_calculatesCorrectly() {
        assertEquals(0, DocumentStorage.countCharacters(""))
        assertEquals(5, DocumentStorage.countCharacters("Hello"))
        assertEquals(11, DocumentStorage.countCharacters("Hello World"))
        assertEquals(5, DocumentStorage.countCharacters("안녕하세요"))
    }

    @Test
    fun decodeBytesToText_handlesUtf8WithBom() {
        val originalText = "Hello PlainText"
        val utf8Bytes = originalText.toByteArray(StandardCharsets.UTF_8)
        val bomBytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + utf8Bytes

        val decoded = DocumentStorage.decodeBytesToText(bomBytes)
        assertEquals("Hello PlainText", decoded)
    }

    @Test
    fun decodeBytesToText_handlesStandardUtf8() {
        val originalText = "다람쥐 헌 쳇바퀴에 타고파"
        val bytes = originalText.toByteArray(StandardCharsets.UTF_8)

        val decoded = DocumentStorage.decodeBytesToText(bytes)
        assertEquals(originalText, decoded)
    }
}
