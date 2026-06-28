package com.example.videostreamingapp.camera

import androidx.camera.core.ImageProxy
import androidx.compose.runtime.Composable

//YUV_420_888 from CameraX gives you three separate Plane objects (Y, U, V), and critically,
// each plane can have a row stride larger than its logical width — the hardware may pad rows
// for memory alignment. Naively concatenating plane buffers without respecting stride produces
// a corrupted/skewed image, a very common silent bug.


fun ImageProxy.toNv21ByteArray(): ByteArray{
    val yPlane=planes[0]//Y plane contains brightness information.there is only 1 value of Y per pixel
    val uPlane=planes[1]//U plane contains Blue color information.there is only 1 value of U per 2x2 pixel block
    val vPlane=planes[2]//V plane contains Red color information.there is only 1 value of V per 2x2 pixel block
/*
A YUV_420_888 image has three planes:
Plane 0 → Y (Brightness)
Plane 1 → U (Blue Chrominance)
Plane 2 → V (Red Chrominance)
*/
    val ySize=yPlane.buffer.remaining()
    val uSize=uPlane.buffer.remaining()
    val vSize=vPlane.buffer.remaining()
// remaining() tells how many bytes are left in each buffer.

    val nv21 = ByteArray(width * height * 3 / 2) // YUV420: 1 Y byte/pixel + 0.5 byte/pixel chroma
    var pos = 0

    //Y plane: copy row by row, respecting rowStride
    val yBuffer = yPlane.buffer
    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride // normally 1

    for (row in 0 until height) {
        val rowStart = row * yRowStride
        if (yPixelStride == 1) {
            // Tightly packed within the row — fast path, one bulk copy per row
            yBuffer.position(rowStart)
            yBuffer.get(nv21, pos, width)
            pos += width
        } else {
            // Rare, but handle it: per-pixel stride within the row
            for (col in 0 until width) {
                nv21[pos++] = yBuffer.get(rowStart + col * yPixelStride)
            }
        }
    }

    //  V and U planes interleaved as NV21 (V, U, V, U, ...)
    // NV21 chroma plane is half-width, half-height (4:2:0 subsampling)
    val chromaHeight = height / 2
    val chromaWidth = width / 2

    val vBuffer = vPlane.buffer
    val uBuffer = uPlane.buffer
    val vRowStride = vPlane.rowStride
    val uRowStride = uPlane.rowStride
    val vPixelStride = vPlane.pixelStride // often 2
    val uPixelStride = uPlane.pixelStride // often 2

    for (row in 0 until chromaHeight) {
        val vRowStart = row * vRowStride
        val uRowStart = row * uRowStride
        for (col in 0 until chromaWidth) {
            nv21[pos++] = vBuffer.get(vRowStart + col * vPixelStride)
            nv21[pos++] = uBuffer.get(uRowStart + col * uPixelStride)
        }
    }

    return nv21
}