package com.footballlivewallpaper

import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

/**
 * A base class for OpenGL live wallpapers.
 * Provides a managed GL context and rendering loop within a WallpaperService.
 */
abstract class GLWallpaperService : WallpaperService() {

    abstract inner class GLEngine : Engine() {
        private var glSurfaceView: WallpaperGLSurfaceView? = null
        private var rendererSet = false

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            
            // --- FIX 5: Manual GLSurfaceView Initialization ---
            // In a WallpaperService, GLSurfaceView needs manual life cycle calls
            // because it's not part of a standard Activity view hierarchy.
            glSurfaceView = WallpaperGLSurfaceView()
        }

        override fun onDestroy() {
            glSurfaceView?.run {
                onPause()
                glSurfaceDestroyed()
                onDetachedFromWindow()
            }
            glSurfaceView = null
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (rendererSet) {
                if (visible) {
                    glSurfaceView?.onResume()
                } else {
                    glSurfaceView?.onPause()
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            glSurfaceView?.glSurfaceCreated(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            glSurfaceView?.glSurfaceChanged(holder, format, width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            glSurfaceView?.glSurfaceDestroyed()
            super.onSurfaceDestroyed(holder)
        }

        protected fun setRenderer(renderer: GLSurfaceView.Renderer) {
            glSurfaceView?.setRenderer(renderer)
            rendererSet = true
        }

        protected fun setEGLContextClientVersion(version: Int) {
            glSurfaceView?.setEGLContextClientVersion(version)
        }

        protected fun setPreserveEGLContextOnPause(preserve: Boolean) {
            glSurfaceView?.setPreserveEGLContextOnPause(preserve)
        }

        /**
         * A custom GLSurfaceView that works inside a WallpaperService.
         */
        inner class WallpaperGLSurfaceView : GLSurfaceView(this@GLWallpaperService) {
            
            init {
                // Must call onAttachedToWindow to start the GLThread
                onAttachedToWindow()
            }

            override fun getHolder(): SurfaceHolder {
                // Return the WallpaperEngine's surface holder
                return this@GLEngine.surfaceHolder
            }

            fun glSurfaceCreated(holder: SurfaceHolder) {
                super.surfaceCreated(holder)
            }

            fun glSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                super.surfaceChanged(holder, format, width, height)
            }

            fun glSurfaceDestroyed() {
                super.surfaceDestroyed(this@GLEngine.surfaceHolder)
            }

            // Expose protected methods as public for the Engine to call
            public override fun onAttachedToWindow() {
                super.onAttachedToWindow()
            }

            public override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
            }
        }
    }
}
