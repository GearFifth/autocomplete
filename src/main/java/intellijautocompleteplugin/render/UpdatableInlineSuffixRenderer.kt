package com.gitlab.plugin.codesuggestions.render

import com.intellij.codeInsight.inline.completion.InlineCompletionFontUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics
import java.awt.Rectangle

/**
 * @see com.intellij.codeInsight.inline.completion.render.InlineSuffixRenderer but the suffix is mutable
 */
class UpdatableInlineSuffixRenderer(private val editor: Editor, var suffix: String) : EditorCustomElementRenderer {
  private val font = InlineCompletionFontUtils.font(editor)
  private val width = editor.contentComponent.getFontMetrics(font).stringWidth(suffix)

  override fun calcWidthInPixels(inlay: Inlay<*>): Int = width
  override fun calcHeightInPixels(inlay: Inlay<*>): Int {
    return editor.contentComponent.getFontMetrics(font).height
  }

  override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
    g.color = InlineCompletionFontUtils.color(editor)
    g.font = font
    g.drawString(suffix, targetRegion.x, targetRegion.y + editor.ascent)
  }
}
