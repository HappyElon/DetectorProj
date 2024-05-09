package com.example.detectorproj

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.json.JSONObject
import java.util.concurrent.CountDownLatch

class RegistrationActivity: AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var upperText: TextView
    private lateinit var userName: EditText
    private lateinit var userEmail: EditText
    private lateinit var userPassword: EditText
    private lateinit var confirmButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        upperText = findViewById(R.id.confirmedStatus)
        userName = findViewById(R.id.userName)
        userEmail = findViewById(R.id.userEmail)
        userPassword = findViewById(R.id.userPassword)
        confirmButton = findViewById(R.id.confirmButton)

        confirmButton.setOnClickListener {
            val userNameEntered = userName.text.toString()
            val userEmailEntered = userEmail.text.toString()
            val userPasswordEntered = userPassword.text.toString()

            val json = connectToWebSocket(client, userNameEntered, userEmailEntered, userPasswordEntered)
            Log.i("JSON", json.toString())
            if (json.length() > 0){
                if (json.get("ok") == true){
                    upperText.text = "Регистрация успешна"
                    upperText.setTextColor(Color.GREEN)

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    upperText.text = "Ошибка" + json.get("errorCode") + ": " + json.get("error")
                    upperText.setTextColor(Color.RED)
                }
            }
        }
    }

    private fun connectToWebSocket(client: OkHttpClient, username: String, useremail: String, userpassword: String): JSONObject {
        Log.d("WebSocket for registration","Connecting")

        val latch = CountDownLatch(1)
        val request: Request = Request
            .Builder()
            .url(Constants.URL_USER_CREATOR)
            .build()
        val listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                output("Received : $text")
                json1 = JSONObject(text)
                Log.i("JSON RECEIVED", json1.toString())
                latch.countDown()
            }
        }
        val ws: WebSocket = client.newWebSocket(request, listener)
        ws.send(Constants.sendloginfo)
        ws.send(listener.createJsonRegistrationRequest(username, useremail, userpassword))

        try {
            latch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return json1
    }

//    private fun connectToWebSocket(client: OkHttpClient, username: String, useremail: String, userpassword: String): JSONObject{
//        Log.d("WebSocket for registration","Connecting")
//
//        val request: Request = Request
//            .Builder()
//            .url(Constants.URL_USER_CREATOR)
//            .build()
//        val listener = WebSocketListener()
//        val ws: WebSocket = client.newWebSocket(request, listener)
//        ws.send(Constants.sendloginfo)
//        ws.send(listener.createJsonRegistrationRequest(username, useremail, userpassword))
//
//        return json1
//    }
}