package com.example.detectorproj

import android.util.Log
import android.widget.Toast
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer


class WebSocketListener : WebSocketListener() {

    private val chunkSize = 4000
    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocket.send("Hello World!")
        Log.e("burak","baglandi")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        output("Received : $text")
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

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
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
            webSocket.send(ByteBuffer.wrap("{'eof':1}".toByteArray()).toByteString())
        }
    }

    fun createJsonRequest(): String {
        val json = JSONObject()
        json.put("image_file", JSONObject().apply {
            put("timestamp", System.currentTimeMillis().toString())
            put("name", "name.jpg")
        })
        json.put("isComplete", "completion_status")
        json.put("confidence", 0.5)
        json.put("speedMs", 100)
        json.put("materials", listOf(
            JSONObject().apply {
                put("mlCode", 0)
                put("coords", listOf(0.0, 0.0, 0.0, 0.0))
                put("conf", 0.0)
            },
            JSONObject().apply {
                put("mlCode", 1)
                put("coords", listOf(0.0, 0.0, 0.0, 0.0))
                put("conf", 0.0)
            }
        ))
        return json.toString()
    }
}