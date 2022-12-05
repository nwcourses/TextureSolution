package com.example.opengl01

import android.opengl.GLSurfaceView
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet


class OpenGLView(ctx: Context, callback: (SurfaceTexture) -> Unit)  :GLSurfaceView(ctx) {
    // Make the renderer an attribute of the OpenGLView so we can access
    // it from outside the OpenGLView
    val renderer = OpenGLRenderer(callback)
    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
    }
}
