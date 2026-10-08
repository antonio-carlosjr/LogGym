package com.loggym.data.repository

/** RF-24, RF-40: planos e divisões precisam de nome. */
internal fun requireName(name: String): String {
    val trimmed = name.trim()
    require(trimmed.isNotEmpty()) { "O nome não pode ficar em branco." }
    return trimmed
}
