package com.example.detectorproj

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class ReportActivity: AppCompatActivity(){

    private lateinit var reportText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        reportText = findViewById(R.id.reportText)

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

        reportText.setText(json.toString())
    }
}