package com.cometchat.uikit.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.USwitchClauseExpression
import org.jetbrains.uast.USwitchExpression
import org.jetbrains.uast.UastBinaryOperator

/**
 * Flags hardcoded string literals assigned to `contentDescription` (Compose)
 * or passed to `setContentDescription(...)` (View) in library src/main
 * (ENG-38656 / X4). Accessibility text must come from string resources so it
 * is translated for screen-reader users in every locale.
 */
class HardcodedContentDescriptionDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(ULiteralExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitLiteralExpression(node: ULiteralExpression) {
                val value = node.value
                if (value !is String || value.isEmpty()) return

                // Compose: `contentDescription = "literal"` (assignment) or
                // named argument `contentDescription = "literal"`.
                val binary = node.getParentOfType<UBinaryExpression>(strict = true)
                val isAssignedToCd = binary != null &&
                    binary.operator == UastBinaryOperator.ASSIGN &&
                    binary.leftOperand.asRenderString().endsWith("contentDescription")

                // Named-argument or direct-conditional form: the literal is the
                // value of `contentDescription = ...`. Bound the walk to the
                // value expression only (when/if/elvis branches), stopping at the
                // first non-conditional ancestor, to avoid matching unrelated
                // literals that merely share a function with a contentDescription.
                val srcHasNamedArg = run {
                    var cur = node.uastParent
                    var depth = 0
                    while (cur != null && depth < 6) {
                        val psi = cur.sourcePsi
                        val text = psi?.text ?: ""
                        // Direct hit: this ancestor is the contentDescription assignment
                        // and starts with it (so the literal is inside its value).
                        val trimmed = text.trimStart()
                        if (trimmed.startsWith("contentDescription")) {
                            val eq = trimmed.indexOf('=')
                            val rhs = if (eq >= 0) trimmed.substring(eq + 1) else ""
                            if (!rhs.contains("R.string") &&
                                !rhs.contains("getString") &&
                                !rhs.contains("stringResource")
                            ) return@run true
                        }
                        // Keep climbing only through conditional value expressions.
                        val isConditional = cur is org.jetbrains.uast.USwitchExpression ||
                            cur is org.jetbrains.uast.USwitchClauseExpression ||
                            cur is org.jetbrains.uast.UIfExpression ||
                            cur is org.jetbrains.uast.UBinaryExpression ||
                            cur is org.jetbrains.uast.UBlockExpression ||
                            cur is org.jetbrains.uast.UParenthesizedExpression
                        if (!isConditional && !trimmed.startsWith("contentDescription")) break
                        cur = cur.uastParent
                        depth++
                    }
                    false
                }

                // View: `setContentDescription("literal")`.
                val call = node.getParentOfType<UQualifiedReferenceExpression>(strict = true)
                val isSetterArg = call?.asRenderString()?.contains("setContentDescription") == true

                if (isAssignedToCd || srcHasNamedArg || isSetterArg) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "Hardcoded `contentDescription`; use a string resource so " +
                            "screen readers speak the localized text (ENG-38656)."
                    )
                }
            }
        }

    companion object {
        val ISSUE: Issue = Issue.create(
            id = "HardcodedContentDescription",
            briefDescription = "Hardcoded accessibility description in library code",
            explanation = """
                UI Kit library code must not assign a hardcoded string literal to \
                `contentDescription` (Compose) or pass one to \
                `setContentDescription()` (View). Those strings are read aloud by \
                screen readers (TalkBack) and must be localized. Move the text into \
                a string resource and reference it via `stringResource(...)` or \
                `context.getString(...)` so it is translated for every locale.
            """,
            category = Category.A11Y,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(
                HardcodedContentDescriptionDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
