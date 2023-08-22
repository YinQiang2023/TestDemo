package com.smartwear.publicwatch.https.response

/**
 * Created by android
 * on 2021/8/6
 */
data class RegisterResponse(val userId: Long, val registerTime: String, val authorization: String) {
    override fun toString(): String {
        return "RegisterResponse(userId=$userId, registerTime='$registerTime', authorization='$authorization')"
    }
}

