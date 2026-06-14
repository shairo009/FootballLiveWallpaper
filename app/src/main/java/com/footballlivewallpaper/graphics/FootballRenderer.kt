package com.footballlivewallpaper.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import com.footballlivewallpaper.physics.PhysicsEngine
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class FootballRenderer(
    private val context: Context,
    private val physics: PhysicsEngine
) : GLSurfaceView.Renderer {

    private val sphere = Sphere(0.85f, 32)
    private var footballTextureId = 0

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var program = 0
    private var posHandle = 0
    private var normHandle = 0
    private var texHandle = 0
    private var mvpHandle = 0
    private var modelHandle = 0
    private var lightPosHandle = 0
    private var texSamplerHandle = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.08f, 0.08f, 0.15f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        program = createProgram(vertexShaderCode, fragmentShaderCode)
        posHandle = GLES20.glGetAttribLocation(program, "vPosition")
        normHandle = GLES20.glGetAttribLocation(program, "vNormal")
        texHandle = GLES20.glGetAttribLocation(program, "vTexCoord")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        modelHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        lightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        texSamplerHandle = GLES20.glGetUniformLocation(program, "uTexture")

        footballTextureId = loadFootballTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 24f, 0f, 0f, 0f, 0f, 1f, 0f)

        val vFOV = Math.toRadians(45.0).toFloat()
        val boxHeight = 2.0f * Math.tan(vFOV / 2.0).toFloat() * 24f
        val boxWidth = boxHeight * ratio
        physics.initBalls(boxWidth / 2f, boxHeight / 2f, 5f)
    }

    override fun onDrawFrame(gl: GL10?) {
        physics.update()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glUniform3f(lightPosHandle, 5f, 20f, 10f)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, footballTextureId)
        GLES20.glUniform1i(texSamplerHandle, 0)

        for (ball in physics.balls) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, ball.x, ball.y, ball.z)
            Matrix.rotateM(modelMatrix, 0, ball.rotX * 57.29f, 1f, 0f, 0f)
            Matrix.rotateM(modelMatrix, 0, ball.rotY * 57.29f, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, ball.rotZ * 57.29f, 0f, 0f, 1f)

            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)

            sphere.draw(posHandle, normHandle, texHandle)
        }
    }

    private fun loadFootballTexture(): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        if (textureHandle[0] != 0) {
            val bitmap = createFootballBitmap()
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
            bitmap.recycle()
        }
        return textureHandle[0]
    }

    /**
     * FIX 4: Draw a proper football (soccer ball) texture —
     * 12 black pentagons surrounded by 20 white hexagons,
     * with stitching lines connecting them.
     */
    private fun createFootballBitmap(): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // --- White base ---
        canvas.drawColor(Color.WHITE)

        val cx = size / 2f
        val cy = size / 2f
        val pentR = 52f   // pentagon radius (circumradius)
        val hexR  = 52f   // hexagon radius

        // --- Pentagon positions (centre + ring of 5 + outer ring of 5 + bottom ring) ---
        // A standard football unwrap approximation on a square texture
        // We place pentagons at the "poles" and around the equator

        val pentagonCentres = listOf(
            // Top area
            floatArrayOf(cx, cy - 155f),
            // Upper ring (5)
            floatArrayOf(cx - 130f, cy - 75f),
            floatArrayOf(cx + 130f, cy - 75f),
            floatArrayOf(cx - 75f,  cy - 35f),
            floatArrayOf(cx + 75f,  cy - 35f),
            // Lower ring (5) — mirror of upper
            floatArrayOf(cx - 130f, cy + 75f),
            floatArrayOf(cx + 130f, cy + 75f),
            floatArrayOf(cx - 75f,  cy + 35f),
            floatArrayOf(cx + 75f,  cy + 35f),
            // Bottom area
            floatArrayOf(cx, cy + 155f),
            // Left/right edge wrap pentagons (partial, for texture tiling)
            floatArrayOf(cx - 220f, cy),
            floatArrayOf(cx + 220f, cy)
        )

        // --- Hexagon positions (fill the gaps between pentagons) ---
        val hexagonCentres = listOf(
            floatArrayOf(cx,         cy),
            floatArrayOf(cx - 155f,  cy - 155f),
            floatArrayOf(cx + 155f,  cy - 155f),
            floatArrayOf(cx - 155f,  cy + 155f),
            floatArrayOf(cx + 155f,  cy + 155f),
            floatArrayOf(cx,         cy - 85f),
            floatArrayOf(cx,         cy + 85f),
            floatArrayOf(cx - 100f,  cy - 155f),
            floatArrayOf(cx + 100f,  cy - 155f),
            floatArrayOf(cx - 100f,  cy + 155f),
            floatArrayOf(cx + 100f,  cy + 155f),
            floatArrayOf(cx - 210f,  cy - 80f),
            floatArrayOf(cx + 210f,  cy - 80f),
            floatArrayOf(cx - 210f,  cy + 80f),
            floatArrayOf(cx + 210f,  cy + 80f),
            floatArrayOf(cx - 200f,  cy),
            floatArrayOf(cx + 200f,  cy),
            floatArrayOf(cx - 50f,   cy - 110f),
            floatArrayOf(cx + 50f,   cy - 110f),
            floatArrayOf(cx - 50f,   cy + 110f),
            floatArrayOf(cx + 50f,   cy + 110f)
        )

        // Draw hexagons first (white with border)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        for (h in hexagonCentres) {
            drawPolygon(canvas, h[0], h[1], hexR, 6, 0f, paint)
        }
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(40, 40, 40)
        paint.strokeWidth = 5f
        for (h in hexagonCentres) {
            drawPolygon(canvas, h[0], h[1], hexR, 6, 0f, paint)
        }

        // Draw pentagons on top (black)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(20, 20, 20)
        for (p in pentagonCentres) {
            drawPolygon(canvas, p[0], p[1], pentR, 5, -Math.PI.toFloat() / 2f, paint)
        }
        // Pentagon outlines (slightly lighter so stitching shows)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(80, 80, 80)
        paint.strokeWidth = 3f
        for (p in pentagonCentres) {
            drawPolygon(canvas, p[0], p[1], pentR, 5, -Math.PI.toFloat() / 2f, paint)
        }

        // Stitching lines — short dashes along hexagon edges near pentagons
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(160, 160, 160)
        paint.strokeWidth = 2f
        drawStitching(canvas, paint, pentagonCentres, pentR)

        return bitmap
    }

    /** Draw a regular polygon centred at (cx, cy) with given circumradius, sides, and angle offset */
    private fun drawPolygon(
        canvas: Canvas, cx: Float, cy: Float,
        radius: Float, sides: Int, startAngle: Float, paint: Paint
    ) {
        val path = Path()
        for (i in 0 until sides) {
            val angle = startAngle + i * 2.0 * Math.PI / sides
            val px = cx + radius * cos(angle).toFloat()
            val py = cy + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    /** Draw small stitching marks radiating outward from each pentagon edge midpoint */
    private fun drawStitching(canvas: Canvas, paint: Paint, centres: List<FloatArray>, radius: Float) {
        val sides = 5
        val startAngle = -Math.PI.toFloat() / 2f
        val stitchLen = 10f
        for (p in centres) {
            for (i in 0 until sides) {
                val a1 = startAngle + i * 2.0 * Math.PI / sides
                val a2 = startAngle + (i + 1) * 2.0 * Math.PI / sides
                val midAngle = ((a1 + a2) / 2.0).toFloat()
                val mx = p[0] + radius * cos(midAngle.toDouble()).toFloat()
                val my = p[1] + radius * sin(midAngle.toDouble()).toFloat()
                val ox = cos(midAngle.toDouble()).toFloat()
                val oy = sin(midAngle.toDouble()).toFloat()
                canvas.drawLine(mx, my, mx + ox * stitchLen, my + oy * stitchLen, paint)
            }
        }
    }

    private fun createProgram(vSource: String, fSource: String): Int {
        val vShader = loadShader(GLES20.GL_VERTEX_SHADER, vSource)
        val fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fSource)
        if (vShader == 0 || fShader == 0) return 0
        
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vShader)
        GLES20.glAttachShader(prog, fShader)
        GLES20.glLinkProgram(prog)
        
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e("FootballRenderer", "Could not link program: " + GLES20.glGetProgramInfoLog(prog))
            GLES20.glDeleteProgram(prog)
            return 0
        }
        return prog
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("FootballRenderer", "Could not compile shader $type: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    companion object {
        private const val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            attribute vec4 vPosition;
            attribute vec3 vNormal;
            attribute vec2 vTexCoord;
            varying vec3 vNormalOut;
            varying vec3 vWorldPos;
            varying vec2 vTexCoordOut;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                vWorldPos = vec3(uModelMatrix * vPosition);
                vNormalOut = mat3(uModelMatrix) * vNormal;
                vTexCoordOut = vTexCoord;
            }
        """

        private const val fragmentShaderCode = """
            precision mediump float;
            uniform vec3 uLightPos;
            uniform sampler2D uTexture;
            varying vec3 vNormalOut;
            varying vec3 vWorldPos;
            varying vec2 vTexCoordOut;
            void main() {
                vec3 norm = normalize(vNormalOut);
                vec3 lightDir = normalize(uLightPos - vWorldPos);
                float diff = max(dot(norm, lightDir), 0.25);
                // Specular highlight
                vec3 viewDir = normalize(vec3(0.0, 0.0, 24.0) - vWorldPos);
                vec3 reflectDir = reflect(-lightDir, norm);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0) * 0.4;
                vec4 texColor = texture2D(uTexture, vTexCoordOut);
                gl_FragColor = texColor * diff + vec4(spec, spec, spec, 0.0);
            }
        """
    }
}
