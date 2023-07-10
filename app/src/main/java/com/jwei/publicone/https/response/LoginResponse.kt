package com.jwei.publicone.https.response

/**
 * Created by android
 * on 2021/7/14
 */
data class LoginResponse(
    val userId: Long, val registerTime: String, val authorization: String
) {
    override fun toString(): String {
        return "LoginResponse(userId=$userId, registerTime='$registerTime', authorization='$authorization')"
    }
}

