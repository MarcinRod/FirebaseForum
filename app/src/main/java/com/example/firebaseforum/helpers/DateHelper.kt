package com.example.firebaseforum.helpers

import java.text.SimpleDateFormat
import java.util.*

// Extension function to convert a Long timestamp to a formatted date string
fun Long.toDateString():String{
    // Create a SimpleDateFormat object with the desired date format
    val sdf = SimpleDateFormat(
        "dd.MM.yyyy HH:mm",
        Locale.getDefault()
    )
    // Create a Date object from the Long timestamp and format it with the SimpleDateFormat
    return sdf.format(Date(this))
}