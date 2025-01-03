package com.cometchat.sampleapp.kotlin.fcm.data.interfaces

import com.cometchat.chat.models.User

/** Interface to handle back press events.  */
fun interface OnSampleUserSelected {
    /** Called when the back button is pressed.  */
    fun onSelect(user: User?)
}
