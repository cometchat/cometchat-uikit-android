package com.cometchat.uikit.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class UIKitIssueRegistry : IssueRegistry() {

    override val issues: List<Issue> = listOf(RawLogDetector.ISSUE, HardcodedContentDescriptionDetector.ISSUE)

    override val api: Int = CURRENT_API

    override val vendor: Vendor = Vendor(
        vendorName = "CometChat",
        feedbackUrl = "https://linear.app/cometchat/issue/ENG-38652",
        identifier = "com.cometchat.uikit.lint"
    )
}
