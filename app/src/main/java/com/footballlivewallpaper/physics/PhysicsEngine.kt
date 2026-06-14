package com.footballlivewallpaper.physics

import kotlin.math.sqrt

class PhysicsEngine(val ballRadius: Float, private val ballCount: Int) {

    class Ball {
        var x = 0f
        var y = 0f
        var z = 0f

        var vx = 0f
        var vy = 0f
        var vz = 0f

        var rotX = 0f
        var rotY = 0f
        var rotZ = 0f
    }

    val balls = Array(ballCount) { Ball() }

    var gravityX = 0f
    var gravityY = -0.006f

    var boxLimitX = 1f
    var boxLimitY = 1f
    var boxLimitZ = 5f

    // FIX 1: bounceFactor < 1.0 so balls LOSE energy on bounce (was 1.02 = energy gain bug)
    private val bounceFactor = 0.82f
    private val speedLimit = 0.28f
    // Friction to slowly damp velocity so balls don't spin forever
    private val friction = 0.995f

    fun initBalls(boxLx: Float, boxLy: Float, boxLz: Float) {
        boxLimitX = boxLx
        boxLimitY = boxLy
        boxLimitZ = boxLz

        for (b in balls) {
            b.x = (Math.random().toFloat() - 0.5f) * (boxLimitX * 2 - ballRadius * 2)
            b.y = (Math.random().toFloat() - 0.5f) * (boxLimitY * 2 - ballRadius * 2)
            b.z = (Math.random().toFloat() - 0.5f) * (boxLimitZ * 2 - ballRadius * 2)

            val sp = 0.08f + Math.random().toFloat() * 0.10f
            val th = Math.random().toFloat() * Math.PI * 2
            val ph = Math.random().toFloat() * Math.PI

            b.vx = sp * Math.sin(ph.toDouble()).toFloat() * Math.cos(th.toDouble()).toFloat()
            b.vy = sp * Math.sin(ph.toDouble()).toFloat() * Math.sin(th.toDouble()).toFloat()
            b.vz = sp * Math.cos(ph.toDouble()).toFloat() * 0.4f
        }
    }

    fun update() {
        for (i in balls.indices) {
            val b = balls[i]

            // Apply gravity
            b.vx += gravityX
            b.vy += gravityY

            // Apply friction damping
            b.vx *= friction
            b.vy *= friction
            b.vz *= friction

            // Clamp to speed limit
            val speed = sqrt(b.vx * b.vx + b.vy * b.vy + b.vz * b.vz)
            if (speed > speedLimit) {
                val scale = speedLimit / speed
                b.vx *= scale
                b.vy *= scale
                b.vz *= scale
            }

            b.x += b.vx
            b.y += b.vy
            b.z += b.vz

            val lx = boxLimitX - ballRadius
            val ly = boxLimitY - ballRadius
            val lz = boxLimitZ - ballRadius

            // Wall bounces — multiply velocity by -bounceFactor (energy loss)
            if (b.x > lx) { b.x = lx; b.vx = -b.vx * bounceFactor }
            if (b.x < -lx) { b.x = -lx; b.vx = -b.vx * bounceFactor }
            if (b.y > ly) { b.y = ly; b.vy = -b.vy * bounceFactor }
            if (b.y < -ly) { b.y = -ly; b.vy = -b.vy * bounceFactor }
            if (b.z > lz) { b.z = lz; b.vz = -b.vz * bounceFactor }
            if (b.z < -lz) { b.z = -lz; b.vz = -b.vz * bounceFactor }

            // Ball-ball collision
            for (j in i + 1 until balls.size) {
                val b2 = balls[j]
                val dx = b.x - b2.x
                val dy = b.y - b2.y
                val dz = b.z - b2.z
                val dist = sqrt(dx * dx + dy * dy + dz * dz)

                if (dist < ballRadius * 2 && dist > 0.0001f) {
                    val nx = dx / dist
                    val ny = dy / dist
                    val nz = dz / dist

                    val rvx = b.vx - b2.vx
                    val rvy = b.vy - b2.vy
                    val rvz = b.vz - b2.vz

                    // dot > 0 means balls moving apart — skip
                    val dot = rvx * nx + rvy * ny + rvz * nz
                    if (dot < 0) {
                        // FIX 2: impulse uses bounceFactor correctly
                        // imp is positive; we subtract from b and add to b2
                        val imp = -dot * (1f + bounceFactor) * 0.5f

                        b.vx += nx * imp
                        b.vy += ny * imp
                        b.vz += nz * imp

                        b2.vx -= nx * imp
                        b2.vy -= ny * imp
                        b2.vz -= nz * imp

                        // Separate overlapping balls
                        val overlap = (ballRadius * 2 - dist) * 0.5f
                        b.x += nx * overlap
                        b.y += ny * overlap
                        b.z += nz * overlap

                        b2.x -= nx * overlap
                        b2.y -= ny * overlap
                        b2.z -= nz * overlap
                    }
                }
            }

            // Rotation based on velocity
            b.rotX += b.vy * 0.3f
            b.rotY += b.vx * 0.3f
            b.rotZ -= b.vx * 0.3f
        }
    }
}
