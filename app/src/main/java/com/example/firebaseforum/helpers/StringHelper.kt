package com.example.firebaseforum.helpers

//Extension function for the String class that capitalizes the first letter of the string.
fun String.myCapitalize(): String {
    // Replace the first character of the string with its uppercase version
    return this.replaceFirstChar { firstLetter: Char -> firstLetter.uppercase() }
}
