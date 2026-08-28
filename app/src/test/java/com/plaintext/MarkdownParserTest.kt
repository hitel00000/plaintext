package com.plaintext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun parse_headingsParsedCorrectly() {
        val md = "# Heading 1\n## Heading 2\n### Heading 3"
        val blocks = MarkdownParser.parse(md)

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Heading && (blocks[0] as MarkdownBlock.Heading).level == 1)
        assertEquals("Heading 1", (blocks[0] as MarkdownBlock.Heading).text)
        assertTrue(blocks[1] is MarkdownBlock.Heading && (blocks[1] as MarkdownBlock.Heading).level == 2)
        assertEquals("Heading 2", (blocks[1] as MarkdownBlock.Heading).text)
        assertTrue(blocks[2] is MarkdownBlock.Heading && (blocks[2] as MarkdownBlock.Heading).level == 3)
        assertEquals("Heading 3", (blocks[2] as MarkdownBlock.Heading).text)
    }

    @Test
    fun parse_codeBlocksParsedCorrectly() {
        val md = "```kotlin\nval x = 10\nprintln(x)\n```"
        val blocks = MarkdownParser.parse(md)

        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.CodeBlock)
        val codeBlock = blocks[0] as MarkdownBlock.CodeBlock
        assertEquals("kotlin", codeBlock.language)
        assertEquals("val x = 10\nprintln(x)", codeBlock.code)
    }

    @Test
    fun parse_blockquotesParsedCorrectly() {
        val md = "> First line\n> Second line"
        val blocks = MarkdownParser.parse(md)

        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Blockquote)
        assertEquals("First line\nSecond line", (blocks[0] as MarkdownBlock.Blockquote).text)
    }

    @Test
    fun parse_listsParsedCorrectly() {
        val md = "- Item 1\n- Item 2\n1. Numbered 1\n2. Numbered 2"
        val blocks = MarkdownParser.parse(md)

        assertEquals(4, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.ListItem && !(blocks[0] as MarkdownBlock.ListItem).isOrdered)
        assertEquals("Item 1", (blocks[0] as MarkdownBlock.ListItem).text)
        assertTrue(blocks[2] is MarkdownBlock.ListItem && (blocks[2] as MarkdownBlock.ListItem).isOrdered)
        assertEquals("Numbered 1", (blocks[2] as MarkdownBlock.ListItem).text)
    }

    @Test
    fun parse_horizontalRuleParsedCorrectly() {
        val md = "Paragraph 1\n---\nParagraph 2"
        val blocks = MarkdownParser.parse(md)

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        assertTrue(blocks[1] is MarkdownBlock.HorizontalRule)
        assertTrue(blocks[2] is MarkdownBlock.Paragraph)
    }

    @Test
    fun parseInlineFormatting_extractsTextProperly() {
        val formatted = MarkdownParser.parseInlineFormatting("**Bold** and *Italic* and `code` and [link](https://example.com)")
        assertEquals("Bold and Italic and  code  and link", formatted.text)
    }
}
