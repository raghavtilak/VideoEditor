package com.raghav.gfgffmpeg

import java.text.CharacterIterator
import java.text.StringCharacterIterator

//This method returns the seconds in hh:mm:ss time format
fun Int.getTime(): String {
    val hr = this / 3600
    val rem = this % 3600
    val mn = rem / 60
    val sec = rem % 60
    return String.format("%02d", hr) + ":" + String.format("%02d", mn) + ":" + String.format("%02d", sec)
}

fun Long.humanReadableByteCountSI(): String {
    var bytes = this
    if (-1000 < bytes && bytes < 1000) {
        return "$bytes B"
    }
    val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
    while (bytes <= -999950 || bytes >= 999950) {
        bytes /= 1000
        ci.next()
    }
    return String.format("%.1f %cB", bytes / 1000.0, ci.current())
}
