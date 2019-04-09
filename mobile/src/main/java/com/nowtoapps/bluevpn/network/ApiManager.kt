package com.nowtoapps.bluevpn.network

import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import com.nowtoapps.bluevpn.model.VpnResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ApiManager {
    companion object {
        private var instance: ApiManager? = null
        fun shared(): ApiManager {
            if (instance == null) {
                instance = ApiManager()
            }
            return instance!!
        }
    }

    fun getVPNData(listener: (VpnResponse?) -> Unit) {
        "https://pastebin.com/raw/dgfwctB2".httpGet().responseString { _, _, result ->
            val (data, error) = result
            GlobalScope.launch(Dispatchers.Main) {
                if (error != null) {
                    listener(null)
                } else {
                    val response = Gson().fromJson(data, VpnResponse::class.java)
                    listener(response)
                }
            }
        }
    }

    fun getApiVersion(listener: (Int) -> Unit) {
        "https://pastebin.com/raw/0gKtNPn0".httpGet().responseString { _, _, result ->
            val (data, error) = result
            GlobalScope.launch(Dispatchers.Main) {
                if (error != null) {
                    listener(1)
                } else {
                    listener(data?.toInt() ?: 1)
                }
            }
        }
    }
}