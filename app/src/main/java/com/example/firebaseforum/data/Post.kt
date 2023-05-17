package com.example.firebaseforum.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
/*
This code defines a data class named Post. The @Parcelize annotation indicates that instances of this class can be serialized and deserialized using the Android Parcelable interface. The class has three properties: author, message, and timestamp.

The author property is a nullable String that represents the author of the post. The message property is also a nullable String that contains the text of the post. The timestamp property is a nullable Long that represents the time when the post was created.

This class is designed to be used in conjunction with Firebase Realtime Database to store and retrieve posts in the database.
 */
@Parcelize
data class Post(val author:String? = null, val message:String? = null, val timestamp: Long? = null) :
    Parcelable
