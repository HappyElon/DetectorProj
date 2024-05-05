package com.example.detectorproj

object Constants {
    const val MODEL_PATH = "module_20240427_2_float32.tflite"
    const val LABELS_PATH = "labels.txt"
    const val CAT = "cat_to_send.jpg"
    const val sendloginfo = "{\"username\":\"admin\", \"password\":\"admin\"}"
    const val URL_BASE = "ws://192.168.1.2:8050/ws/save_result"
}