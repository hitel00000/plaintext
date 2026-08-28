package com.plaintext

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class ListItem(val isOrdered: Boolean, val marker: String, val text: String) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
}

object MarkdownParser {

    private fun isHorizontalRule(trimmed: String): Boolean {
        val nonSpace = trimmed.filter { !it.isWhitespace() }
        return nonSpace.length >= 3 && (nonSpace.all { it == '-' } || nonSpace.all { it == '*' } || nonSpace.all { it == '_' })
    }

    private fun parseHeading(trimmed: String): MarkdownBlock.Heading? {
        if (!trimmed.startsWith("#")) return null
        val level = trimmed.takeWhile { it == '#' }.length
        if (level in 1..6 && trimmed.length > level && trimmed[level] == ' ') {
            val text = trimmed.substring(level + 1).trim()
            return MarkdownBlock.Heading(level = level, text = text)
        }
        return null
    }

    private fun parseOrderedList(trimmed: String): MarkdownBlock.ListItem? {
        val sepIdx = trimmed.indexOfFirst { it == '.' || it == ')' }
        if (sepIdx in 1..9 && trimmed.length > sepIdx + 1 && trimmed[sepIdx + 1] == ' ') {
            val numStr = trimmed.substring(0, sepIdx)
            if (numStr.all { it.isDigit() }) {
                val marker = "$numStr."
                val text = trimmed.substring(sepIdx + 2).trim()
                return MarkdownBlock.ListItem(isOrdered = true, marker = marker, text = text)
            }
        }
        return null
    }

    private fun parseUnorderedList(trimmed: String): MarkdownBlock.ListItem? {
        if ((trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) && trimmed.length >= 2) {
            val text = trimmed.substring(2).trim()
            return MarkdownBlock.ListItem(isOrdered = false, marker = "•", text = text)
        }
        return null
    }

    private fun isBlockStart(trimmed: String): Boolean {
        if (trimmed.isEmpty()) return true
        if (trimmed.startsWith("```")) return true
        if (isHorizontalRule(trimmed)) return true
        if (parseHeading(trimmed) != null) return true
        if (trimmed.startsWith(">")) return true
        if (parseOrderedList(trimmed) != null) return true
        if (parseUnorderedList(trimmed) != null) return true
        return false
    }

    fun parse(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = markdown.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // 1. Fenced Code Block: ```lang
            if (line.trimStart().startsWith("```")) {
                val indent = line.indexOf("```")
                val lang = line.substring(indent + 3).trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size) {
                    val nextLine = lines[i]
                    if (nextLine.trimStart().startsWith("```")) {
                        i++
                        break
                    }
                    codeLines.add(nextLine)
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(language = lang, code = codeLines.joinToString("\n")))
                continue
            }

            val trimmed = line.trim()

            // 2. Blank line
            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // 3. Horizontal Rule: ---, ***, ___
            if (isHorizontalRule(trimmed)) {
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
                continue
            }

            // 4. Headings: # H1 ~ ###### H6
            val heading = parseHeading(trimmed)
            if (heading != null) {
                blocks.add(heading)
                i++
                continue
            }

            // 5. Blockquote: > text
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().substring(1).trimStart())
                    i++
                }
                blocks.add(MarkdownBlock.Blockquote(text = quoteLines.joinToString("\n")))
                continue
            }

            // 6. Ordered List: 1. text
            val orderedList = parseOrderedList(trimmed)
            if (orderedList != null) {
                blocks.add(orderedList)
                i++
                continue
            }

            // 7. Unordered List: * text, - text, + text
            val unorderedList = parseUnorderedList(trimmed)
            if (unorderedList != null) {
                blocks.add(unorderedList)
                i++
                continue
            }

            // 8. Paragraph (combine continuous non-empty lines until blank line or block start)
            val paragraphLines = mutableListOf<String>()
            while (i < lines.size) {
                val current = lines[i]
                val currentTrimmed = current.trim()
                if (currentTrimmed.isEmpty() || (paragraphLines.isNotEmpty() && isBlockStart(currentTrimmed))) {
                    break
                }
                paragraphLines.add(current)
                i++
            }
            if (paragraphLines.isNotEmpty()) {
                blocks.add(MarkdownBlock.Paragraph(text = paragraphLines.joinToString("\n")))
            }
        }

        return blocks
    }

    fun parseInlineFormatting(
        text: String,
        codeBackgroundColor: Color = Color.Unspecified,
        codeTextColor: Color = Color.Unspecified,
        linkColor: Color = Color.Unspecified
    ): AnnotatedString {
        return buildAnnotatedString {
            var idx = 0
            val length = text.length

            while (idx < length) {
                // Inline code: `code`
                if (text[idx] == '`') {
                    val endBacktick = text.indexOf('`', idx + 1)
                    if (endBacktick != -1) {
                        val codeContent = text.substring(idx + 1, endBacktick)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBackgroundColor,
                                color = codeTextColor
                            )
                        )
                        append(" $codeContent ")
                        pop()
                        idx = endBacktick + 1
                        continue
                    }
                }

                // Bold: **text** or __text__
                if (idx + 1 < length && (text.substring(idx, idx + 2) == "**" || text.substring(idx, idx + 2) == "__")) {
                    val delimiter = text.substring(idx, idx + 2)
                    val endBold = text.indexOf(delimiter, idx + 2)
                    if (endBold != -1) {
                        val content = text.substring(idx + 2, endBold)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(content)
                        pop()
                        idx = endBold + 2
                        continue
                    }
                }

                // Strikethrough: ~~text~~
                if (idx + 1 < length && text.substring(idx, idx + 2) == "~~") {
                    val endStrike = text.indexOf("~~", idx + 2)
                    if (endStrike != -1) {
                        val content = text.substring(idx + 2, endStrike)
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        append(content)
                        pop()
                        idx = endStrike + 2
                        continue
                    }
                }

                // Italic: *text* or _text_
                if (text[idx] == '*' || text[idx] == '_') {
                    val delim = text[idx]
                    val endItalic = text.indexOf(delim, idx + 1)
                    if (endItalic != -1 && endItalic > idx + 1 && text[endItalic - 1] != ' ') {
                        val content = text.substring(idx + 1, endItalic)
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(content)
                        pop()
                        idx = endItalic + 1
                        continue
                    }
                }

                // Link: [title](url)
                if (text[idx] == '[') {
                    val closeBracket = text.indexOf(']', idx + 1)
                    if (closeBracket != -1 && closeBracket + 1 < length && text[closeBracket + 1] == '(') {
                        val closeParen = text.indexOf(')', closeBracket + 2)
                        if (closeParen != -1) {
                            val title = text.substring(idx + 1, closeBracket)
                            pushStyle(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(title)
                            pop()
                            idx = closeParen + 1
                            continue
                        }
                    }
                }

                append(text[idx])
                idx++
            }
        }
    }
}

@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    val scrollState = rememberScrollState()

    val codeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val codeTextColor = MaterialTheme.colorScheme.primary
    val linkColor = MaterialTheme.colorScheme.primary

    SelectionContainer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (blocks.isEmpty()) {
                Text(
                    text = "No content to preview",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp)
                            2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 28.sp)
                            3 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp)
                            4 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
                            5 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)
                            else -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
                        }
                        Text(
                            text = MarkdownParser.parseInlineFormatting(
                                text = block.text,
                                codeBackgroundColor = codeBg,
                                codeTextColor = codeTextColor,
                                linkColor = linkColor
                            ),
                            style = style,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (block.level <= 2) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 1.dp
                            )
                        }
                    }

                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = MarkdownParser.parseInlineFormatting(
                                text = block.text,
                                codeBackgroundColor = codeBg,
                                codeTextColor = codeTextColor,
                                linkColor = linkColor
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    is MarkdownBlock.CodeBlock -> {
                        val codeScrollState = rememberScrollState()
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (block.language.isNotBlank()) {
                                    Text(
                                        text = block.language.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                Box(modifier = Modifier.fillMaxWidth().horizontalScroll(codeScrollState)) {
                                    Text(
                                        text = block.code,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownBlock.Blockquote -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = MarkdownParser.parseInlineFormatting(
                                    text = block.text,
                                    codeBackgroundColor = codeBg,
                                    codeTextColor = codeTextColor,
                                    linkColor = linkColor
                                ),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is MarkdownBlock.ListItem -> {
                        Row(modifier = Modifier.fillMaxWidth().padding(start = 6.dp)) {
                            Text(
                                text = "${block.marker} ",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = MarkdownParser.parseInlineFormatting(
                                    text = block.text,
                                    codeBackgroundColor = codeBg,
                                    codeTextColor = codeTextColor,
                                    linkColor = linkColor
                                ),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    is MarkdownBlock.HorizontalRule -> {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            thickness = 1.5.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
