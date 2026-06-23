package com.amenskal.gridcad11.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.amenskal.gridcad11.R
import com.amenskal.gridcad11.server.FrameBridge
import com.amenskal.gridcad11.server.MjpegServer
import com.amenskal.gridcad11.server.StreamForegroundService
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.common.UVCCameraHandler
import com.serenegiant.usb.widget.CameraViewInterface
import java.nio.ByteBuffer

class CameraBridgeFragment : Fragment(), CameraViewInterface.OnFrameCapturedCallback {

    private lateinit var usbMonitor: USBMonitor
    private lateinit var cameraHandler: UVCCameraHandler
    private lateinit var cameraView: CameraViewInterface
    private lateinit var statusText: TextView

    private val frameBridge = FrameBridge()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StreamForegroundService.start(requireContext())

        // CameraView is inflated from layout, but we need it for handler creation.
        // We'll defer handler creation until onViewCreated where cameraView is available.
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera_bridge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraView = view.findViewById(R.id.cameraView)
        statusText = view.findViewById(R.id.statusText)

        // Now create camera handler with the cameraView reference
        cameraHandler = UVCCameraHandler.createHandler(
            requireContext(),
            cameraView,
            UVCCameraHandler.createParams(1280, 720, 30),
            this  // frame callback
        )

        // Init USB monitor
        usbMonitor = USBMonitor(requireContext(), object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: android.hardware.usb.UsbDevice?) {
                device?.let { usbMonitor.requestPermission(it) }
            }

            override fun onConnect(
                device: android.hardware.usb.UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?
            ) {
                cameraHandler.open(ctrlBlock)
            }

            override fun onDisconnect(
                device: android.hardware.usb.UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?
            ) {
                cameraHandler.close()
            }

            override fun onDettach(device: android.hardware.usb.UsbDevice?) {
                cameraHandler.close()
            }

            override fun onCancel(device: android.hardware.usb.UsbDevice?) {
                // permission denied
            }
        })

        // Start MJPEG server
        MjpegServer.start(8080)

        usbMonitor.register()
        val list = usbMonitor.deviceList
        if (list.isNotEmpty()) {
            usbMonitor.requestPermission(list[0])
        }
    }

    override fun onResume() {
        super.onResume()
        usbMonitor.register()
    }

    override fun onPause() {
        super.onPause()
        usbMonitor.unregister()
        cameraHandler.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        MjpegServer.stop()
        StreamForegroundService.stop(requireContext())
        usbMonitor.destroy()
        frameBridge.release()
    }

    override fun onFrameCaptured(data: ByteBuffer, width: Int, height: Int) {
        frameBridge.pushFrame(data.array(), width, height)
        requireActivity().runOnUiThread {
            statusText.text = "Streaming ${width}x${height}"
        }
    }
}
