package com.linuxgods.kreiger.intellij.idea.readable.whitespace

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.ScaleAwarePresentationFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.util.DocumentUtil
import org.jetbrains.annotations.Nls
import javax.swing.JComponent
import javax.swing.JPanel

private const val PREVIEW_TEXT = """class Foo {
    int foo() {
        if (false) {
            return 1;
        }
        // Extra line inserted below right brace
        System.out.println("Hello, world!");
        // Extra line inserted above return
        return 2;
    }
}
"""

class ReadableWhitespaceInlayProvider : InlayHintsProvider<NoSettings> {
    override fun getCollectorFor(psiFile: PsiFile, editor: Editor, o: NoSettings, sink: InlayHintsSink): InlayHintsCollector? {
        val f = PresentationFactory(editor)
        val sf: InlayPresentationFactory = ScaleAwarePresentationFactory(editor, f)
        return object : InlayHintsCollector {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                when (element) {
                    is PsiReturnStatement -> {
                        addLineAboveReturn(element)
                    }
                    is PsiWhiteSpace -> {
                        addLineUnderBrace(element)
                    }
                }
                return true
            }

            private fun addLineAboveReturn(rs: PsiReturnStatement) {
                if (rs.parent !is PsiCodeBlock) return
                if ((rs.parent as PsiCodeBlock).statementCount < 2) return
                val e: PsiElement? = skipCommentsBackwards(rs)
                if (e == null) return
                sink.addBlockElement(e.startOffset, false, true, 0, sf.text(""))
            }

            private fun skipCommentsBackwards(rs: PsiReturnStatement): PsiElement? {
                var result: PsiElement? = rs
                var e: PsiElement? = rs.prevSibling
                while (e != null) {
                    when (e) {
                        is PsiWhiteSpace -> {
                            if (multiLineWhitespace(e)) return null
                        }
                        is PsiComment -> {
                            result = e
                        }
                        else -> {
                            break
                        }
                    }
                    e = e.prevSibling
                }
                return result
            }

            private fun multiLineWhitespace(e: PsiWhiteSpace): Boolean {
                val startLine = editor.document.getLineNumber(e.startOffset)
                val endLine = editor.document.getLineNumber(e.endOffset)
                return endLine - startLine > 1
            }

            private fun addLineUnderBrace(ws: PsiWhiteSpace) {
                if (PsiTreeUtil.skipWhitespacesAndCommentsForward(ws) is PsiReturnStatement) return
                if (PsiTreeUtil.lastChild(ws.prevSibling).elementType != JavaTokenType.RBRACE) return
                val nextLine = editor.document.getLineNumber(ws.startOffset) + 1
                if (DocumentUtil.isLineEmpty(editor.document, nextLine)) return
                val offset = editor.document.getLineStartOffset(nextLine)
                sink.addBlockElement(offset, true, true, 0, sf.text(""))
            }
        }
    }

    override fun createSettings(): NoSettings {
        return NO_SETTINGS
    }

    override val name: @Nls(capitalization = Nls.Capitalization.Sentence) String
        get() = "Readable whitespace"

    override val key: SettingsKey<NoSettings>
        get() = SettingsKey("readable.whitespace")

    override val previewText: String
        get() = PREVIEW_TEXT

    override fun createConfigurable(o: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(changeListener: ChangeListener): JComponent {
                return JPanel()
            }
        }
    }

    companion object {
        val NO_SETTINGS: NoSettings = NoSettings()
    }
}
