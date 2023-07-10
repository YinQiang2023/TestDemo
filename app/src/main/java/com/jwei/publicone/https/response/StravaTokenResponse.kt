package com.jwei.publicone.https.response

/**
 * Created by Android on 2022/5/17.
 */
data class StravaTokenResponse(
    val access_token: String,
    val athlete: Athlete,
    val expires_at: Int,
    val expires_in: Int,
    val refresh_token: String,
    val token_type: String
)

data class Athlete(
    val badge_type_id: Int,
    val bio: Any,
    val city: Any,
    val country: String,
    val created_at: String,
    val firstname: String,
    val follower: Any,
    val friend: Any,
    val id: Int,
    val lastname: String,
    val premium: Boolean,
    val profile: String,
    val profile_medium: String,
    val resource_state: Int,
    val sex: String,
    val state: String,
    val summit: Boolean,
    val updated_at: String,
    val username: Any,
    val weight: Any
)