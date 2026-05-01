package me.rerere.rikkahub.ui.components.richtext

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import me.rerere.rikkahub.ui.components.webview.WebView
import me.rerere.rikkahub.ui.components.webview.rememberWebViewState
import me.rerere.rikkahub.utils.base64Encode
import me.rerere.rikkahub.utils.toCssHex

/**
 * Build HTML page for markdown preview with support for:
 * - Markdown rendering via marked.js
 * - LaTeX math via KaTeX
 * - Mermaid diagrams
 * - Syntax highlighting via highlight.js
 */
fun buildMarkdownPreviewHtml(context: Context, markdown: String, colorScheme: ColorScheme): String {
    val htmlTemplate = context.assets.open("html/mark.html").bufferedReader().use { it.readText() }

    return htmlTemplate
        .replace("{{MARKDOWN_BASE64}}", markdown.base64Encode())
        .replace("{{BACKGROUND_COLOR}}", colorScheme.background.toCssHex())
        .replace("{{ON_BACKGROUND_COLOR}}", colorScheme.onBackground.toCssHex())
        .replace("{{SURFACE_COLOR}}", colorScheme.surface.toCssHex())
        .replace("{{ON_SURFACE_COLOR}}", colorScheme.onSurface.toCssHex())
        .replace("{{SURFACE_VARIANT_COLOR}}", colorScheme.surfaceVariant.toCssHex())
        .replace("{{ON_SURFACE_VARIANT_COLOR}}", colorScheme.onSurfaceVariant.toCssHex())
        .replace("{{PRIMARY_COLOR}}", colorScheme.primary.toCssHex())
        .replace("{{OUTLINE_COLOR}}", colorScheme.outline.toCssHex())
        .replace("{{OUTLINE_VARIANT_COLOR}}", colorScheme.outlineVariant.toCssHex())
}

@Composable
fun MarkdownWebBlock(
    content: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    var contentHeight by remember(content) { mutableIntStateOf(120) }
    val height = with(density) { contentHeight.toDp() }
    val jsInterface = remember(content) {
        MarkdownWebInterface { height ->
            val measuredHeight = (height * density.density).toInt().coerceAtLeast(1)
            if (measuredHeight != contentHeight) {
                contentHeight = measuredHeight
            }
        }
    }
    val html = remember(context, content, colorScheme) {
        buildMarkdownPreviewHtml(context, content, colorScheme)
    }
    val webViewState = rememberWebViewState(
        data = html,
        mimeType = "text/html",
        encoding = "UTF-8",
        interfaces = mapOf("AndroidInterface" to jsInterface),
        settings = {
            builtInZoomControls = false
            displayZoomControls = false
        }
    )

    WebView(
        state = webViewState,
        modifier = modifier
            .animateContentSize()
            .height(height),
        onCreated = {
            it.isVerticalScrollBarEnabled = false
            it.isHorizontalScrollBarEnabled = false
        },
        onUpdated = {
            it.evaluateJavascript("notifyMarkdownHeight();", null)
        }
    )
}

private class MarkdownWebInterface(
    private val onHeightChanged: (Int) -> Unit,
) {
    @JavascriptInterface
    fun updateHeight(height: Int) {
        onHeightChanged(height)
    }
}
