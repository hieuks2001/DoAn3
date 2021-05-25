package com.example.firebase.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(var uid: String?, var username: String, var profileImageUrl: String) : Parcelable {
    constructor() : this("", "", "")
}