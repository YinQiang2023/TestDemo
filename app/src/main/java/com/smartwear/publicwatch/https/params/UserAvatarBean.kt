package com.smartwear.publicwatch.https.params

data class UserAvatarBean(
    val userId: Long,
    val headImageFormat: String = "png",
    val headImageData: String
)
