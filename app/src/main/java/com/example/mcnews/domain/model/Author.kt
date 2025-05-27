
package com.example.mcnews.domain.model

data class Author(
    val userId: Int,
    val firstName: String,
    val lastName: String
) {
    val fullName: String get() = "$firstName $lastName"
}
