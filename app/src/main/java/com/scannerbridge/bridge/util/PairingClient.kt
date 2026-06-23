package com.amenskal.gridcad11.util

import android.util.Log
import okhttp3.*
import java.io.IOException

object PairingClient {
    private val client = OkHttpClient()

    fun sendPhoneIp(pcIp: String, port: Int, token: String, phoneIp: String) {
        val url = "http://$pcIp:$port/pair"
        val body = FormBody.Builder()
            .add("token", token)
            .add("ip", phoneIp)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PairingClient", "POST failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.i("PairingClient", "Phone IP sent successfully")
                } else {
                    Log.e("PairingClient", "Server responded: ${response.code}")
                }
                response.close()
            }
        })
    }
}
