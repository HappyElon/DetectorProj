package com.example.detectorproj

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.detectorproj.Constants.CAT
import com.example.detectorproj.Constants.LABELS_PATH
import com.example.detectorproj.Constants.MODEL_PATH
import com.example.detectorproj.Constants.URL_BASE
import com.example.detectorproj.Constants.sendloginfo
import com.example.detectorproj.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@SuppressLint("StaticFieldLeak")
lateinit var completionDetectorTextView: TextView
@SuppressLint("StaticFieldLeak")
lateinit var takeAPhotoButton: ImageButton

class MainActivity : AppCompatActivity(), Detector.DetectorListener {

    private val PERMISSION_REQUEST_CODE = 101

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private lateinit var binding: ActivityMainBinding
    private lateinit var previewView: PreviewView
    private lateinit var root: View
    private val isFrontCamera = false
    private val client = OkHttpClient()

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null

    private val viewModel by viewModels<MainViewModel>()

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private val TAG = "CameraXBasic"
    private val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private val REQUEST_CODE_PERMISSIONS = 10

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA
        ).toTypedArray()

        var photoFile: File? = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)){
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_main_land)
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_main)
        }
        //startCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                !viewModel.isReady.value
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // пологировать это

        completionDetectorTextView = findViewById(R.id.completionDetector)
        takeAPhotoButton = findViewById(R.id.takePhotoButton)
        previewView = findViewById(R.id.view_finder)
        root = findViewById(R.id.drawer_layout)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigationView)
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
        navigationView.setNavigationItemSelectedListener(  NavigationView.OnNavigationItemSelectedListener() {
            when(it.itemId) {
                R.id.home -> {
                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                }
                R.id.contact -> {
                    val intent = Intent(this, ReportActivity::class.java)
                    startActivity(intent)
                }
                R.id.reports -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri = Uri.parse("content://Android/media/com.example.detectorproj/Detector App")
                    intent.setDataAndType(uri, "image/*")
                    startActivity(intent)
                    // ИСПРАВИТЬ, ОНО ОТКРЫВАЕТ КАРТИНКУ КОТОРОЙ НЕТ
                }
                R.id.About -> {
                    val intent = Intent(this, InfoActivity::class.java)
                    startActivity(intent)
                }
                R.id.login -> {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
            return@OnNavigationItemSelectedListener false
        })

        cameraExecutor = Executors.newSingleThreadExecutor()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        cameraExecutor.execute {
            detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
            detector?.setup()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        //bindListeners()

        takeAPhotoButton.setOnClickListener {
            takePhoto()
        }
        outputDirectory = getOutputDirectory()
    }

    private fun connectToWebSocket(client: OkHttpClient){
        Log.d("WebSocket","Connecting")

        val request: Request = Request
            .Builder()
            .url(URL_BASE)
            .build()
        val listener = WebSocketListener()
        val ws: WebSocket = client.newWebSocket(request, listener)
        ws.send(sendloginfo)
        ws.send(listener.createJsonRequest())
        Log.i("file check", photoFile.toString())
        listener.sendFileInChunks(ws, photoFile.toString())
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connectToWebSocket(client)
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply {
                mkdir()
            }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir
        else filesDir

    }

    private fun takePhoto() {

        val imageCapture = imageCapture?:return

        photoFile = File(outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())+".jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile!!).build() // !! added remove if not needed

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                    if (checkPermissions()){
                        connectToWebSocket(client)
                    } else {
                        requestPermissions()
                    }
                }
            }
        )
    }

//    private fun bindListeners() {
//        binding.apply {
//            isGpu.setOnCheckedChangeListener { buttonView, isChecked ->
//                cameraExecutor.submit {
//                    detector?.setup(isGpu = isChecked)
//                }
//                if (isChecked) {
//                    buttonView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.orange))
//                } else {
//                    buttonView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.gray))
//                }
//            }
//        }
//    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))

    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display?.rotation?: return

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview =  Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build(). also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector?.detect(rotatedBitmap)
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer,
                imageCapture
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)

        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector?.close()
        cameraExecutor.shutdown()
    }

//    override fun onStart() {
//        super.onStart()
//        completionDetectorTextView = findViewById(R.id.completionDetector)
//        takeAPhotoButton = findViewById(R.id.takePhotoButton)
//        previewView = findViewById(R.id.view_finder)
//        root = findViewById(R.id.drawer_layout)
//    }

    override fun onResume() {
        super.onResume()

//        completionDetectorTextView = findViewById(R.id.completionDetector)
//        takeAPhotoButton = findViewById(R.id.takePhotoButton)
//        previewView = findViewById(R.id.view_finder)
//        root = findViewById(R.id.drawer_layout)

        if (allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
            //ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS) // non-original
        }
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            binding.overlay.clear()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }
    }
}

fun changeButtonStatus(buttonStatus: String){
    if (buttonStatus == "complete"){
        completionDetectorTextView.text = "COMPLETE"
        completionDetectorTextView.setTextColor(Color.GREEN)
    } else if (buttonStatus == "incomplete"){
        completionDetectorTextView.text = "INCOMPLETE"
        completionDetectorTextView.setTextColor(Color.RED)
    }
}