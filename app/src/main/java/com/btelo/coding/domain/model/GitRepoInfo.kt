package com.btelo.coding.domain.model

data class GitRepoInfo(
    val name: String,
    val path: String,
    val currentBranch: String,
    val lastModified: Long
)
