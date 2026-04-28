package com.btelo.coding.domain.model

data class ProxyEntry(
    val id: String,
    val address: String,
    val fullAddress: String,
    val status: ProxyStatus,
    val errorMessage: String? = null
)

enum class ProxyStatus {
    ACTIVE, ERROR
}
