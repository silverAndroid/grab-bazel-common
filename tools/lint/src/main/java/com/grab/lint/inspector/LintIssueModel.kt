package com.grab.lint.inspector

import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.TextFormat

data class LintCheckModel(
    val jar:String,
    val issues: List<LintIssueModel>
){
    companion object {
        fun fromJar(jar: String, issues: List<Issue>): LintCheckModel {
            return LintCheckModel(
                jar,
                issues.map { it.toLintIssueModel() }
            )
        }
    }
}

data class LintIssueModel(
    val id: String,
    val category: String,
    val priority: Int,
    val Severity: String,
    val enabledByDefault: Boolean,
    val briefDescription: String,
)

fun Issue.toLintIssueModel(): LintIssueModel {
    return LintIssueModel(
        this.id,
        this.category.name,
        this.priority,
        this.defaultSeverity.name,
        this.isEnabledByDefault(),
        this.getBriefDescription(TextFormat.RAW),
    )
}