package com.btelo.coding.domain.model

data class SlashCommand(
    val name: String,
    val description: String
) {
    val displayName: String get() = "/$name"

    companion object {
        val builtInCommands = listOf(
            SlashCommand("brainstorming", "Generate creative ideas and solutions"),
            SlashCommand("brand-guidelines", "Create brand style guides and visual standards"),
            SlashCommand("canvas-design", "Create beautiful visual art in .png and .pdf"),
            SlashCommand("check-commit-push", "Check changes, commit, and push to remote"),
            SlashCommand("claude-api", "Configure Claude API settings"),
            SlashCommand("deploy-iphone", "Deploy to iPhone device"),
            SlashCommand("fix-and-commit-and-push", "Fix and commit push")
        )
    }
}
