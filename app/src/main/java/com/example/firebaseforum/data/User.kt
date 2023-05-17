package com.example.firebaseforum.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
/*
The @Parcelize annotation above the class definition is a shorthand for generating the necessary code to implement the Parcelable interface.
This reduces the amount of boilerplate code required to write and maintain.
Simple data model for representing a User
 */
@Parcelize
data class User(val email:String? = null) : Parcelable
