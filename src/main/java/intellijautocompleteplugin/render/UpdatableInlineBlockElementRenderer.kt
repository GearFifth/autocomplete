package com.gitlab.plugin.codesuggestions.render

import com.intellij.codeInsight.inline.completion.InlineCompletionFontUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics
import java.awt.Rectangle

/**
 * @see com.intellij.codeInsight.inline.completion.render.InlineBlockElementRenderer but the lines are mutable
 */
class UpdatableInlineBlockElementRenderer(
  private val editor: Editor,
  var lines: List<String>
) : EditorCustomElementRenderer {
  private val font = InlineCompletionFontUtils.font(editor)
  private val width = editor
    .contentComponent
    .getFontMetrics(font)
    .stringWidth(lines.maxBy { it.length })

  override fun calcWidthInPixels(inlay: Inlay<*>) = width

  override fun calcHeightInPixels(inlay: Inlay<*>) = editor.lineHeight * lines.size

  override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
    g.color = InlineCompletionFontUtils.color(editor)
    g.font = font
    lines.forEachIndexed { i, line -> g.drawString(line, 0, targetRegion.y + editor.ascent + i * editor.lineHeight) }
  }
}
