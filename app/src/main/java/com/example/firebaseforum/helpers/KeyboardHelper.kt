package com.example.firebaseforum.helpers

import android.content.Context
import android.os.IBinder
import android.view.inputmethod.InputMethodManager

/*
The KeyboardHelper object contains a function to hide the software keyboard.

The hideSoftwareKeyboard function takes two parameters: context, which is the context of the calling activity,
and windowToken, which is the token of the window that is currently showing the keyboard.

The InputMethodManager object is obtained using the getSystemService method of the context.

The hideSoftInputFromWindow method of the InputMethodManager is then called with the windowToken and 0 as its parameters to hide the keyboard.
 */
object KeyboardHelper {
    fun hideSoftwareKeyboard(context: Context,windowToken: IBinder) {
        // Get the InputMethodManager object
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // Hide the software keyboard
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}