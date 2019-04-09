package com.nowtoapps.bluevpn

import android.view.View

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible(isVisible: Boolean = true) {
    if (isVisible) {
        this.visibility = View.VISIBLE
    } else {
        this.visibility = View.GONE
    }
}