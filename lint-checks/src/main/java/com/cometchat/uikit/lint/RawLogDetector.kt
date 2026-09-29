package com.cometchat.uikit.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

/**
 * Forbids direct android.util.Log usage in the UI Kit library modules (ENG-38652).
 * All logging must go through com.cometchat.uikit.core.utils.CometChatLogger so
 * that nothing is written to logcat in production apps by default.
 */
class RawLogDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> =
        listOf("d", "e", "i", "v", "w", "wtf", "println")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!context.evaluator.isMemberInClass(method, "android.util.Log")) return
        // The gated logger itself is the single allowed call site.
        if (context.uastFile?.classes?.any {
                it.qualifiedName == "com.cometchat.uikit.core.utils.CometChatLogger"
            } == true
        ) {
            return
        }
        context.report(
            ISSUE,
            node,
            context.getLocation(node),
            "Raw `android.util.Log` is forbidden in UI Kit modules; " +
                "use `CometChatLogger` so logs stay gated in production (ENG-38652)."
        )
    }

    companion object {
        val ISSUE: Issue = Issue.create(
            id = "RawAndroidLog",
            briefDescription = "Raw android.util.Log call in library code",
            explanation = """
                UI Kit library code must not call `android.util.Log` directly: those \
                logs are emitted unconditionally, including in consumers' production \
                builds, which leaks diagnostic data and previously leaked user content \
                (GDPR/HIPAA risk). Route all logging through \
                `com.cometchat.uikit.core.utils.CometChatLogger`, which is disabled by \
                default unless the host app is debuggable or logging is explicitly enabled.
            """,
            category = Category.SECURITY,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(RawLogDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
