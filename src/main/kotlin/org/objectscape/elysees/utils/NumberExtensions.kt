package org.objectscape.elysees.utils

fun Long.max(number: Long) : Long {
    if (this > number) {
        return this
    }
    return number
}