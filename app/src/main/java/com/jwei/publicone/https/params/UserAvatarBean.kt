package com.jwei.publicone.https.params

data class UserAvatarBean(
    val userId: Long,
    val headImageFormat: String = "png",
    val headImageData: String
)
