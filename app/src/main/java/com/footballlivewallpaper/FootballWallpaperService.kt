package com.footballlivewallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class FootballWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return FootballEngine()
    }

    inner class FootballEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        
        // Ball properties
        private var x = 100f
        private var y = 100f
        private var vx = 10f
        private var vy = 15f
        private val ballRadius = 50f
        
        private val bgPaint = Paint().apply { color = Color.parseColor("#2E7D32") } // Grass green
        private val ballPaint = Paint().apply { 
            color = Color.WHITE
            isAntiAlias = true
        }
        private val patternPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                draw()
                if (visible) {
                    handler.postDelayed(this, 16) // ~60 FPS
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            this.visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    // Update physics
                    updatePhysics(canvas.width.toFloat(), canvas.height.toFloat())
                    
                    // Draw background
                    canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), bgPaint)
                    
                    // Draw football field lines (optional but looks nice)
                    drawFieldLines(canvas)
                    
                    // Draw football
                    drawFootball(canvas, x, y)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        private fun updatePhysics(width: Float, height: Float) {
            x += vx
            y += vy

            if (x - ballRadius < 0 || x + ballRadius > width) {
                vx = -vx
                x = if (x - ballRadius < 0) ballRadius else width - ballRadius
            }
            if (y - ballRadius < 0 || y + ballRadius > height) {
                vy = -vy
                y = if (y - ballRadius < 0) ballRadius else height - ballRadius
            }
        }

        private fun drawFieldLines(canvas: Canvas) {
            val linePaint = Paint().apply {
                color = Color.WHITE
                alpha = 100
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            
            // Center line
            canvas.drawLine(0f, h / 2, w, h / 2, linePaint)
            // Center circle
            canvas.drawCircle(w / 2, h / 2, 150f, linePaint)
        }

        private fun drawFootball(canvas: Canvas, cx: Float, cy: Float) {
            // Main ball body
            canvas.drawCircle(cx, cy, ballRadius, ballPaint)
            
            // Simple pentagon patterns to make it look like a football
            canvas.drawCircle(cx, cy, ballRadius, patternPaint)
            
            // Draw some lines/shapes inside to mimic a football
            for (i in 0 until 5) {
                val angle = Math.toRadians((i * 72).toDouble())
                val px = cx + (Math.cos(angle) * ballRadius).toFloat()
                val py = cy + (Math.sin(angle) * ballRadius).toFloat()
                canvas.drawLine(cx, cy, px, py, patternPaint)
            }
        }
    }
}
