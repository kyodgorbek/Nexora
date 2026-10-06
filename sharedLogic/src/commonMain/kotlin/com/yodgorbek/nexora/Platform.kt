package com.yodgorbek.nexora

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform