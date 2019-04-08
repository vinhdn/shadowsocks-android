package com.nowtoapps.bluevpn.plugin

import com.nowtoapps.bluevpn.Core.app

object NoPlugin : Plugin() {
    override val id: String get() = ""
    override val label: CharSequence get() = app.getText(com.nowtoapps.bluevpn.core.R.string.plugin_disabled)
}
