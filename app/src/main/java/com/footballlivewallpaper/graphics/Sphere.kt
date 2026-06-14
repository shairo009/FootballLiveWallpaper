package com.footballlivewallpaper.graphics

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Sphere(val radius: Float, val numSegments: Int) {
    val vertexBuffer: FloatBuffer
    val normalBuffer: FloatBuffer
    val textureBuffer: FloatBuffer
    val numVertices: Int

    init {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val textures = mutableListOf<Float>()

        val numLatitudes = numSegments
        val numLongitudes = numSegments

        for (i in 0..numLatitudes) {
            val theta = i * Math.PI / numLatitudes
            val sinTheta = Math.sin(theta).toFloat()
            val cosTheta = Math.cos(theta).toFloat()

            for (j in 0..numLongitudes) {
                val phi = j * 2 * Math.PI / numLongitudes
                val sinPhi = Math.sin(phi).toFloat()
                val cosPhi = Math.cos(phi).toFloat()

                val x = cosPhi * sinTheta
                val y = cosTheta
                val z = sinPhi * sinTheta
                val u = 1.0f - (j.toFloat() / numLongitudes)
                val v = 1.0f - (i.toFloat() / numLatitudes)

                normals.add(x)
                normals.add(y)
                normals.add(z)
                
                textures.add(u)
                textures.add(v)

                vertices.add(radius * x)
                vertices.add(radius * y)
                vertices.add(radius * z)
            }
        }

        val indexData = mutableListOf<Short>()
        for (i in 0 until numLatitudes) {
            for (j in 0 until numLongitudes) {
                val first = (i * (numLongitudes + 1) + j).toShort()
                val second = (first + numLongitudes + 1).toShort()

                indexData.add(first)
                indexData.add(second)
                indexData.add((first + 1).toShort())

                indexData.add(second)
                indexData.add((second + 1).toShort())
                indexData.add((first + 1).toShort())
            }
        }

        numVertices = indexData.size
        
        val floatVertices = FloatArray(indexData.size * 3)
        val floatNormals = FloatArray(indexData.size * 3)
        val floatTextures = FloatArray(indexData.size * 2)
        
        for (i in indexData.indices) {
            val idx = indexData[i].toInt()
            floatVertices[i * 3] = vertices[idx * 3]
            floatVertices[i * 3 + 1] = vertices[idx * 3 + 1]
            floatVertices[i * 3 + 2] = vertices[idx * 3 + 2]
            
            floatNormals[i * 3] = normals[idx * 3]
            floatNormals[i * 3 + 1] = normals[idx * 3 + 1]
            floatNormals[i * 3 + 2] = normals[idx * 3 + 2]
            
            floatTextures[i * 2] = textures[idx * 2]
            floatTextures[i * 2 + 1] = textures[idx * 2 + 1]
        }

        vertexBuffer = ByteBuffer.allocateDirect(floatVertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(floatVertices).also { it.position(0) }
        normalBuffer = ByteBuffer.allocateDirect(floatNormals.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(floatNormals).also { it.position(0) }
        textureBuffer = ByteBuffer.allocateDirect(floatTextures.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(floatTextures).also { it.position(0) }
    }

    fun draw(positionHandle: Int, normalHandle: Int, texCoordHandle: Int) {
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numVertices)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }
}
