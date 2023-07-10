package com.jwei.publicone.expansion

import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

//post, postDelay
fun AppCompatActivity.post(action: () -> Unit) {
    Handler(mainLooper).post { action() }
}

fun AppCompatActivity.postDelay(delay: Long = 0, action: () -> Unit) {
    Handler(mainLooper).postDelayed({ action() }, delay)
}

fun Fragment.postDelay(delay: Long = 0, action: () -> Unit) {
    activity?.mainLooper?.let { Handler(it).postDelayed({ action() }, delay) }
}
