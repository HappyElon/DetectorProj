package com.example.detectorproj

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import com.example.detectorproj.Constants.URL_USER_LOGIN
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import com.google.gson.annotations.SerializedName
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class RequestDataModel(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("error") val error: String?,
    @SerializedName("errorCode") val errorCode: Int?,
    @SerializedName("data") val data: Data?
)

data class Data(
    @SerializedName("user_id") val user_id: String,
    @SerializedName("user_name") val user_name: String,
    @SerializedName("user_email") val user_email: String
)

interface ApiService {
    @GET(URL_USER_LOGIN)
    fun fetchData(
        @Query("userdata") username: String,
        @Query("userpassword") password: String
    ): Call<RequestDataModel>
}

class LoginActivity: AppCompatActivity() {

    private lateinit var loginEnterField: EditText
    private lateinit var passwordEnterField: EditText
    private lateinit var loginButton: Button
    private lateinit var registrationButton: Button
    private lateinit var loginVerifier: TextView
    private lateinit var header_title: TextView

    companion object {
        var result = ""
        var global_username = "Unknown"
        var global_email = "Unknown"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginEnterField = findViewById(R.id.loginEnter)
        passwordEnterField = findViewById(R.id.passwordEnter)
        loginButton = findViewById(R.id.enterButton)
        registrationButton = findViewById(R.id.registrationButton)
        loginVerifier = findViewById(R.id.correctLoginVerifier)

        //header_title = header.findViewById(R.id.header_title)

        loginButton.setOnClickListener{
            val loginText = loginEnterField.text.toString()
            val passwordText = passwordEnterField.text.toString()

            connectToRest(loginText, passwordText) { isLogged ->
                if (isLogged) {
                    loginVerifier.text = "Авторизация успешна"
                    loginVerifier.setTextColor(Color.WHITE)
                    Log.i("global username", global_username)
                    //header_title.text = global_username

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        registrationButton.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun connectToRest(userdata: String, userpassword: String, callback: (Boolean) -> Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl(URL_USER_LOGIN)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)

        val call = service.fetchData(userdata, userpassword)

        call.enqueue(object : retrofit2.Callback<RequestDataModel> {
            override fun onResponse(call: Call<RequestDataModel>, response: retrofit2.Response<RequestDataModel>) {
                if (response.isSuccessful) {
                    val data = response.body()
                    println("Успешный ответ: $data")
                    Log.i("RESULT BODY", response.body()?.ok.toString())
                    result = response.body()?.ok.toString()
                    global_username = response.body()?.data?.user_name.toString()
                    global_email = response.body()?.data?.user_email.toString()
                    callback(result == "true")
                } else {
                    println("Ошибка: ${response.errorBody()?.string()}")
                    result = "false"
                    callback(false)
                }
            }

            override fun onFailure(call: Call<RequestDataModel>, t: Throwable) {
                println("Ошибка: ${t.message}")
                result = "false"
                callback(false)
            }
        })
    }
}

fun userDataCoder(){

}