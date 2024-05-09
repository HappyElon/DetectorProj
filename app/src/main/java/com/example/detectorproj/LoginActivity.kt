package com.example.detectorproj

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

class LoginActivity: AppCompatActivity() {

    private lateinit var loginEnterField: EditText
    private lateinit var passwordEnterField: EditText
    private lateinit var loginButton: Button
    private lateinit var loginVerifier: TextView
    private val logins_list = arrayOf("admin", "user")
    private val passwords_list = arrayOf("admin", "user")

    companion object {
        var isLogged = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginEnterField = findViewById(R.id.loginEnter)
        passwordEnterField = findViewById(R.id.passwordEnter)
        loginButton = findViewById(R.id.enterButton)
        loginVerifier = findViewById(R.id.correctLoginVerifier)

        loginButton.setOnClickListener{
            val loginText = loginEnterField.text.toString()
            val passwordText = passwordEnterField.text.toString()

            for ((login, password) in logins_list.zip(passwords_list)){
                if ((loginText == login) && (passwordText == password)){
                    // вставить функцию что типо всё залогинилось и вообще отлично
                    loginVerifier.text = "Вход в систему"
                    loginVerifier.setTextColor(Color.GREEN)
                    isLogged = true

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    loginVerifier.text = "Данные неверны"
                    loginVerifier.setTextColor(Color.RED)
                }
            }
        }
    }
}