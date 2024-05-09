package com.example.detectorproj

import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.delay
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer

var json1: JSONObject = JSONObject()

open class WebSocketListener : WebSocketListener() {
    private val chunkSize = 4000

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        //webSocket.send("Hello World!")
        Log.e("Connection","got")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        output("Received : $text")
        json1 = JSONObject(text)
        Log.i("JSON RECEIVED", json1.toString())
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        output("Closing : $code / $reason")
    }


    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        output("Error : " + t.message)
    }

    fun output(text: String?) {
        Log.d("WebSocket", text!!)
    }

    fun sendFileInChunks(webSocket: WebSocket, filePath: String) {
        val file = File(filePath)
        file.inputStream().use { stream ->
            var bytes = ByteArray(chunkSize)
            var numRead: Int
            while (stream.read(bytes).also { numRead = it } != -1) {
                if (numRead < chunkSize) {
                    bytes = bytes.copyOf(numRead)
                }
                webSocket.send(ByteBuffer.wrap(bytes).toByteString())
            }
            // Indicate the end of file transmission
            webSocket.send(ByteBuffer.wrap("{'eof' : 1}".toByteArray()).toByteString())
        }
    }

    fun createJsonRequest(): String {
        val json = JSONObject()
        json.put("image_file", JSONObject().apply {
            put("timestamp", System.currentTimeMillis().toString())
            put("name", MainActivity.photoFile.toString())
        })
        json.put("isComplete", Detector.currentStatus)
        json.put("confidence", Detector.currentFirstCnf)
        json.put("speedMs", Detector.latencyTime)
        json.put("materials", listOf(
            JSONObject().apply {
                put("mlCode", 0)
                put("coords", listOf(Detector.module_x1, Detector.module_x2, Detector.module_y1, Detector.module_y2))
                put("conf", Detector.moduleConf)
            },
            JSONObject().apply {
                put("mlCode", 1)
                put("coords", listOf(Detector.antenna_x1, Detector.antenna_x2, Detector.antenna_y1, Detector.antenna_y2))
                put("conf", Detector.antennaConf)
            }
        ))
        json.put("username", LoginActivity.global_username)
        return json.toString()
    }

    fun createJsonRegistrationRequest(username: String, useremail: String, userpassword: String): String {
        val json = JSONObject()

        json.put("username", username)
        json.put("useremail", useremail)
        json.put("userpassword", userpassword)

        return json.toString()
    }

    fun createJsonLoginRequest(userdata: String, userpassword: String): String {
        val json = JSONObject()

        json.put("userdata", userdata)
        json.put("userpassword", userpassword)

        return json.toString()
    }
}