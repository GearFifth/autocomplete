package com.gitlab.plugin.codesuggestions.render

import com.intellij.codeInsight.inline.completion.elements.InlineCompletionElement
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.ex.util.EditorActionAvailabilityHint
import com.intellij.openapi.editor.ex.util.addActionAvailabilityHint
import com.intellij.openapi.util.Disposer
import java.awt.Rectangle

/**
 * @see com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement that can be updated.
 */
class UpdatableInlineCompletionGrayElement(
  override var text: String,
  val editor: Editor,
  /*
    If we create a homegrown solution, we should move this state in the SuggestionTracker.
    It is best we avoid keeping any state in the inline element.
   */
  val isPartiallyAccepted: Boolean = false
) : InlineCompletionElement {
  private val presentable by lazy { Presentable(this) }
  var onDone: () -> Unit = {}

  override fun toPresentable(): InlineCompletionElement.Presentable = presentable

  fun cancel() {
    presentable.dispose()
  }

  fun update(completion: String) {
    if (presentable.isDisposed || completion.isEmpty() || isPartiallyAccepted) {
      return
    }

    text = completion

    val lines = text.lines()
    presentable.updateSuffix(editor, lines)

    if (lines.size > 1) {
      presentable.updateBlock(editor, lines)
    } else {
      presentable.clearBlock()
    }
  }

  /**
   * @see com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement.Presentable that can be updated.
   */
  @Suppress("TooManyFunctions")
  class Presentable(override val element: InlineCompletionElement) : InlineCompletionElement.Presentable {
    var isDisposed = false

    private var suffixInlay: Inlay<UpdatableInlineSuffixRenderer>? = null
    private var blockInlay: Inlay<UpdatableInlineBlockElementRenderer>? = null

    override fun isVisible(): Boolean = suffixInlay != null || blockInlay != null

    override fun render(editor: Editor, offset: Int) {
      val text = element.text.takeIf { it.isNotEmpty() }
        ?: return

      val lines = text.lines()
      renderSuffix(editor, lines, offset)
      if (lines.size > 1) {
        renderBlock(lines.drop(1), editor, offset)
      }
    }

    override fun getBounds(): Rectangle? {
      val bounds = suffixInlay?.bounds?.let { Rectangle(it) }
      blockInlay?.bounds?.let { bounds?.add(Rectangle(it)) }
      return bounds
    }

    override fun startOffset(): Int? = suffixInlay?.offset
    override fun endOffset(): Int? = suffixInlay?.offset

    override fun dispose() {
      blockInlay?.also(Disposer::dispose)
      blockInlay = null

      suffixInlay?.also(Disposer::dispose)
      suffixInlay = null

      isDisposed = true
    }

    fun renderSuffix(editor: Editor, lines: List<String>, offset: Int) {
      // Ensure inline completion renders on the left to the caret after moving it
      editor.forceLeanLeft()

      var line = lines.first()
      if (line.isEmpty()) {
        line = " "
      }

      editor.inlayModel.execute(true) {
        val element = editor.inlayModel.addInlineElement(
          offset,
          true,
          UpdatableInlineSuffixRenderer(editor, line)
        ) ?: return@execute

        element.addActionAvailabilityHint(
          EditorActionAvailabilityHint(
            IdeActions.ACTION_INSERT_INLINE_COMPLETION,
            EditorActionAvailabilityHint.AvailabilityCondition.CaretOnStart,
          )
        )

        suffixInlay = element
      }
    }

    fun updateSuffix(editor: Editor, lines: List<String>) {
      suffixInlay?.dispose()
      renderSuffix(editor, lines, editor.caretModel.offset)
    }

    fun renderBlock(
      lines: List<String>,
      editor: Editor,
      offset: Int
    ) {
      val element = editor.inlayModel.addBlockElement(
        offset,
        true,
        false,
        1,
        UpdatableInlineBlockElementRenderer(editor, lines)
      ) ?: return

      blockInlay = element
    }

    // NOTE: Re-rendering the block without disposing the existing inlay prevents scrolling.
    fun updateBlock(editor: Editor, lines: List<String>) {
      if (blockInlay != null) {
        blockInlay?.renderer?.lines = lines.drop(1)
        blockInlay?.update()
        blockInlay?.repaint()
      } else {
        renderBlock(lines.drop(1), editor, editor.caretModel.offset)
      }
    }

    fun clearBlock() {
      blockInlay?.dispose()
      blockInlay = null
    }

    private fun Editor.forceLeanLeft() {
      val visualPosition = caretModel.visualPosition
      if (visualPosition.leansRight) {
        val leftLeaningPosition = VisualPosition(visualPosition.line, visualPosition.column, false)
        caretModel.moveToVisualPosition(leftLeaningPosition)
      }
    }
  }
}
