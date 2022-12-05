package com.example.opengl01

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var permissions = arrayOf(Manifest.permission.CAMERA)
    private var surfaceTexture: SurfaceTexture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openGLView = OpenGLView(this) {
            surfaceTexture = it
            if (!startCamera()) {
                ActivityCompat.requestPermissions(this, permissions, 0)
            }
        }
        setContentView(openGLView)


        /*
        findViewById<Button>(R.id.plusX).setOnClickListener {
            openGLView.renderer.translateCamera(1f, 0f, 0f)
        }
        findViewById<Button>(R.id.minusX).setOnClickListener {
            openGLView.renderer.translateCamera(-1f, 0f, 0f)
        }
        findViewById<Button>(R.id.plusY).setOnClickListener {
            openGLView.renderer.translateCamera(0f, 1f, 0f)
        }
        findViewById<Button>(R.id.minusY).setOnClickListener {
            openGLView.renderer.translateCamera(0f, -1f, 0f)
        }
        findViewById<Button>(R.id.plusZ).setOnClickListener {
            openGLView.renderer.translateCamera(0f, 0f, 1f)
        }
        findViewById<Button>(R.id.minusZ).setOnClickListener {
            openGLView.renderer.translateCamera(0f, 0f, -1f)
        }
        findViewById<Button>(R.id.rotateClockwise).setOnClickListener {
            openGLView.renderer.rotateCamera(-10f)
        }
        findViewById<Button>(R.id.rotateAnticlockwise).setOnClickListener {
            openGLView.renderer.rotateCamera(10f)
        }
        findViewById<Button>(R.id.cameraForward).setOnClickListener {
            openGLView.renderer.moveCamera(1.0f)
        }
        findViewById<Button>(R.id.cameraBack).setOnClickListener {
            openGLView.renderer.moveCamera(-1.0f)
        }

         */
    }

    private fun checkPermissions(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
           startCamera()
        } else {
            AlertDialog.Builder(this).setPositiveButton("OK", null)
                .setMessage("Will not work as camera permission not granted").show()
        }
    }

    private fun startCamera(): Boolean {
        Log.d("OpenGL01Log", "startCamera()")
        if (checkPermissions()) {
            Log.d("OpenGL01Log", "startCamera() ready to go")
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    val provider: (SurfaceRequest) -> Unit = { request ->
                        val resolution = request.resolution
                        surfaceTexture?.apply {

                            setDefaultBufferSize(resolution.width, resolution.height)
                            val surface = Surface(this)
                            request.provideSurface(
                                surface,
                                ContextCompat.getMainExecutor(this@MainActivity.baseContext))
                            { }

                        }
                    }
                    it.setSurfaceProvider(provider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview)

                } catch (e: Exception) {
                    Log.e("OpenGL01Log", e.stackTraceToString())
                }
            }, ContextCompat.getMainExecutor(this))
            return true
        } else {
            return false
        }
    }
}