package com.unipass.core.types

enum class ChainType(val value: String) {
    eth("eth"),
    polygon("polygon"),
    bsc("bsc"),
    rangers("rangers"),
    scroll("scroll")
}

enum class UniPassTheme(val value: String){
    dark("dark"),
    light("light")
}


data class AppSettings(
    val chain: ChainType = ChainType.polygon,
    val appName: String? = null,
    val appIcon: String? = null,
    val theme: UniPassTheme = UniPassTheme.dark
)