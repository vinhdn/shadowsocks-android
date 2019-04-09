package com.nowtoapps.bluevpn.model

import com.nowtoapps.bluevpn.database.Profile

data class VpnResponse(var version: Int,
                       var data: List<Profile>)