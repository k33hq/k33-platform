package com.k33.platform.utils.logging

fun Any.prettyPrint(): String {
    var indentLevel = 0
    val indentWidth = 2

    fun padding() = "".padStart(indentLevel * indentWidth)

    val charIterator = toString().iterator()

    var ignoreSpace = false
    var isFirstEqualsChar = true

    return buildString {
        while (charIterator.hasNext()) {
            when (val char = charIterator.next()) {
                '(', '[', '{' -> {
                    indentLevel++
                    appendLine(char)
                    append(padding())
                    isFirstEqualsChar = true
                }
                ')', ']', '}' -> {
                    indentLevel--
                    appendLine()
                    append(padding())
                    append(char)
                    isFirstEqualsChar = true
                }
                ',' -> {
                    appendLine(char)
                    append(padding())
                    ignoreSpace = true
                    isFirstEqualsChar = true
                }
                ' ' -> {
                    if (!ignoreSpace) {
                        append(char)
                    }
                    ignoreSpace = false
                }
                '=' -> {
                    if (isFirstEqualsChar) {
                        append(" = ")
                        isFirstEqualsChar = false
                    } else {
                        append(char)
                    }
                }
                else -> {
                    append(char)
                }
            }
        }
    }
}