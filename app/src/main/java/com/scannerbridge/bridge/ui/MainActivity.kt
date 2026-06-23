package com.amenskal.gridcad11

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amenskal.gridcad11.ui.CameraBridgeFragment
import com.amenskal.gridcad11.ui.QrScanFragment
import com.amenskal.gridcad11.util.NetworkUtils
import com.amenskal.gridcad11.util.PairingClient

class MainActivity : AppCompatActivity(), QrScanFragment.QrScanListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, QrScanFragment())
                .commit()
        }
    }

    override fun onQrDecoded(qrData: String) {
        // Expected format: "pc_ip|port|token"
        val parts = qrData.split("|")
        if (parts.size != 3) {
            Log.e("Main", "Invalid QR data: $qrData")
            Toast.makeText(this, "Invalid QR data", Toast.LENGTH_SHORT).show()
            return
        }
        val pcIp = parts[0]
        val port = parts[1].toIntOrNull() ?: 8765
        val token = parts[2]

        val phoneIp = NetworkUtils.getLocalIpAddress()
        if (phoneIp == null) {
            Toast.makeText(this, "Could not get local IP", Toast.LENGTH_SHORT).show()
            return
        }

        // Send phone IP to PC pairing gate
        PairingClient.sendPhoneIp(pcIp, port, token, phoneIp)

        // Switch to streaming fragment (UVC webcam)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, CameraBridgeFragment())
            .commit()

        Toast.makeText(this, "Paired! Streaming started.", Toast.LENGTH_SHORT).show()
    }
}
