package com.example.detectorproj

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.detectorproj.Constants.URL_GET_DATA
import com.example.detectorproj.Constants.URL_USER_LOGIN
import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class RequestReports(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("error") val error: String?,
    @SerializedName("errorCode") val errorCode: Int?,
    @SerializedName("data") val data: List<ReportData>
)

data class ReportData(
    @SerializedName("d") val serverDate: String,
    @SerializedName("username") val username: String,
    @SerializedName("is_complete") val completionStatus: String,
    @SerializedName("cnt") val cnt: String,
    @SerializedName("speed_ms") val speedMs: String
)

interface ReportService {
    @GET(URL_GET_DATA)
    fun fetchData(

    ): Call<RequestReports>
}

class CustomRecyclerAdapter(var names: List<String>) : RecyclerView
.Adapter<CustomRecyclerAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val largeTextView: TextView = itemView.findViewById(R.id.textViewLarge)
        val smallTextView: TextView = itemView.findViewById(R.id.textViewSmall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_recycler, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.largeTextView.text = names[position]
        holder.smallTextView.text = "кот"
    }

    override fun getItemCount() = names.size
}

class ReportActivity: AppCompatActivity(){

    companion object {
        var transmitData: List<ReportData>? = null
    }

    private lateinit var adapter: CustomRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        val recyclerView: RecyclerView = findViewById(R.id.reportsRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CustomRecyclerAdapter(mutableListOf())
        recyclerView.adapter = adapter

        connectToRest()
    }

    private fun connectToRest() {
        val retrofit = Retrofit.Builder()
            .baseUrl(URL_GET_DATA)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ReportService::class.java)

        val call = service.fetchData()

        call.enqueue(object : retrofit2.Callback<RequestReports> {
            override fun onResponse(call: Call<RequestReports>, response: retrofit2.Response<RequestReports>) {
                if (response.isSuccessful) {
                    val data = response.body()
                    println("Успешный ответ: $data")
                    Log.i("RESULT BODY", response.body()?.ok.toString())
                    transmitData = response.body()?.data
                    transmitData?.let {
                        adapter.names = fillList(it).toMutableList()
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    println("Ошибка: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<RequestReports>, t: Throwable) {
                println("Ошибка: ${t.message}")
            }
        })
    }

    private fun fillList(receivedData: List<ReportData>): List<String> {
        return receivedData.mapIndexed { index, reportData ->
            "$index ${reportData.serverDate} ${reportData.username} ${reportData.completionStatus} ${reportData.cnt} ${reportData.speedMs}"
        }
    }
}