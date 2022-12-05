package com.example.opengl01

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRenderer(val textureAvailableCallback: (SurfaceTexture) -> Unit) : GLSurfaceView.Renderer {

    // Must negate y when calculating texcoords from vertex coords as bitmap image data assumes
    // y increases downwards
    val texVertexShaderSrc =
        "attribute vec4 aVertex;\n" +
                "varying vec2 vTextureValue;\n" +
                "void main (void)\n" +
                "{\n" +
                "gl_Position = aVertex;\n" +
                "vTextureValue = vec2(0.5*(1.0 + aVertex.x), 0.5*(1.0-aVertex.y));\n" +
                "}\n"
    val texFragmentShaderSrc =
        "#extension GL_OES_EGL_image_external: require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureValue;\n" +
                "uniform samplerExternalOES uTexture;\n" +
                "void main(void)\n" +
                "{\n" +
                "gl_FragColor = texture2D(uTexture,vTextureValue);\n" +
                "}\n"


    var texShaderProgram = -1



    var texBuffer: FloatBuffer? = null


    lateinit var texIndexBuffer: ShortBuffer


    var cameraFeedSurfaceTexture: SurfaceTexture? = null



    // We initialise the OpenGL view here
    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background colour (red=0, green=0, blue=0, alpha=1)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Enable depth testing - will cause nearer 3D objects to automatically
        // be drawn over further objects
        GLES20.glClearDepthf(1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // http://stackoverflow.com/questions/6414003/using-surfacetexture-in-android
        val GL_TEXTURE_EXTERNAL_OES = 0x8d65
        val textureId = IntArray(1)
        GLES20.glGenTextures(1, textureId, 0)
        if (textureId[0] != 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId[0])

            // Mag filters not really needed here...

            cameraFeedSurfaceTexture = SurfaceTexture(textureId[0])



            val texVertexShader = compileShader(GLES20.GL_VERTEX_SHADER, texVertexShaderSrc)
            val texFragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, texFragmentShaderSrc)
            texShaderProgram = linkShader(texVertexShader, texFragmentShader)

            createCameraRect()

            val refShaderVar = GLES20.glGetUniformLocation(texShaderProgram, "uTexture")
            GLES20.glUniform1i(refShaderVar, 0)


            textureAvailableCallback(cameraFeedSurfaceTexture!!)
        }
    }

    // We draw our shapes here
    override fun onDrawFrame(unused: GL10) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)


        // Camera
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        cameraFeedSurfaceTexture?.updateTexImage()

        //textureInterface?.select()
        GLES20.glUseProgram(texShaderProgram)

        if(texBuffer == null) {
            Log.d("OpenGL01Log", "null tex buffer")
            return
        }

        val attrVarRef = GLES20.glGetAttribLocation(texShaderProgram, "aVertex")
        texBuffer?.position(0)
        texIndexBuffer.position(0)

        GLES20.glEnableVertexAttribArray(attrVarRef)
        GLES20.glVertexAttribPointer(attrVarRef, 3, GLES20.GL_FLOAT, false, 0, texBuffer)

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            texIndexBuffer.limit(),
            GLES20.GL_UNSIGNED_SHORT,
            texIndexBuffer
        )

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Overlay


    }

    // Used if the screen is resized
    override fun onSurfaceChanged(unused: GL10, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
        val hfov = 60.0f
        val aspect: Float = w.toFloat() / h
    }

    fun compileShader(shaderType: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(shaderType)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun linkShader(vertexShader: Int, fragmentShader: Int): Int {
        val shaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(shaderProgram, vertexShader)
        GLES20.glAttachShader(shaderProgram, fragmentShader)
        GLES20.glLinkProgram(shaderProgram)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(
                "OpenGL01Log",
                "Error linking shader program: " + GLES20.glGetProgramInfoLog(shaderProgram)
            )
            GLES20.glDeleteProgram(shaderProgram)
            return -1
        }
        GLES20.glUseProgram(shaderProgram)
        Log.d("OpenGL01Log", "Shader program = $shaderProgram")
        return shaderProgram
    }

    fun makeBuffer(vertices: FloatArray): FloatBuffer {
        val bbuf: ByteBuffer = ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES)
        bbuf.order(ByteOrder.nativeOrder())
        val fbuf: FloatBuffer = bbuf.asFloatBuffer()
        fbuf.put(vertices)
        fbuf.position(0)
        return fbuf
    }

    fun makeIndexBuffer(indices: ShortArray): ShortBuffer {
        val bbuf: ByteBuffer = ByteBuffer.allocateDirect(indices.size * Short.SIZE_BYTES)
        bbuf.order(ByteOrder.nativeOrder())
        val sbuf: ShortBuffer = bbuf.asShortBuffer()
        sbuf.put(indices)
        sbuf.position(0)
        return sbuf
    }

    private fun createCameraRect() {

        val cameraRect = floatArrayOf(-1f, 1f, 0f, -1f, -1f, 0f, 1f, -1f, 0f, 1f, 1f, 0f)
        val indices = shortArrayOf(0, 1, 2, 2, 3, 0)

        texBuffer = makeBuffer(cameraRect)
        texIndexBuffer = makeIndexBuffer(indices)
    }
}