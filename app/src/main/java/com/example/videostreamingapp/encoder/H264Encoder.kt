package com.example.videostreamingapp.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.io.Closeable

class H264Encoder(
    private val width: Int,// frame dimensions. MediaCodec must know these upfront to allocate its internal buffers
    private val height: Int,// frame dimensions. MediaCodec must know these upfront to allocate its internal buffers
    bitRate:Int=2_000_000,//sets quality of 2mbps
    frameRate:Int=30,//it is fps and is not hard set
    iFrameInterval:Int=1//decides the interval after which an I frame is sent

): Closeable{//We use that to release the hardware encoder when done.

    private val codec: MediaCodec= MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)// AVC (Advanced Video Coding) is the formal name for H.264
    private val bufferInfo = MediaCodec.BufferInfo()//When you ask MediaCodec "is there any encoded output ready?", it fills this object with metadata about the output: how many bytes, what timestamp, and flags (like "this is a keyframe" or "this is SPS/PPS config data").
    private var presentationUs=0L//presentationUs is a timestamp we stamp on each input frame, in microseconds. It starts at 0 and increases by frameStepUs each frame
    private var frameStepUs=1_000_000L//frameStepUs = 1,000,000 µs ÷ 30 = 33,333 µs per frame. That's 33.3ms, which is the duration of one frame at 30fps.MediaCodec uses these timestamps to maintain correct frame ordering. If you send frames out of order (rare, but possible), it uses these to sort them.

    init {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
    }

    fun encode(nv21: ByteArray): List<ByteArray> {
        feedInput(nv21)
        return drainOutput()
    }
    private fun feedInput(nv21: ByteArray) {
        val idx = codec.dequeueInputBuffer(10_000L)
        if (idx < 0) return
        val buf = codec.getInputBuffer(idx) ?: return
        buf.clear()
        val frameSize = width * height
        // Y plane is identical in NV21 and NV12
        buf.put(nv21, 0, frameSize)
        // NV21 chroma is VU-interleaved; COLOR_FormatYUV420SemiPlanar (NV12) needs UV
        var i = frameSize
        while (i < nv21.size - 1) {
            buf.put(nv21[i + 1]) // U
            buf.put(nv21[i])     // V
            i += 2
        }
        codec.queueInputBuffer(idx, 0, buf.position(), presentationUs, 0)
        presentationUs += frameStepUs
    }

    private fun drainOutput(): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        while (true) {
            val idx = codec.dequeueOutputBuffer(bufferInfo, 0)
            if (idx < 0) break
            val buf = codec.getOutputBuffer(idx) ?: run {
                codec.releaseOutputBuffer(idx, false)
                continue
            }
            val raw = ByteArray(bufferInfo.size)
            buf.get(raw)
            codec.releaseOutputBuffer(idx, false)
            if (raw.isNotEmpty()) result.add(withStartCode(raw))
        }
        return result
    }

    private fun withStartCode(data: ByteArray): ByteArray {
        if (data.size >= 4 &&
            data[0] == 0x00.toByte() && data[1] == 0x00.toByte() &&
            data[2] == 0x00.toByte() && data[3] == 0x01.toByte()) return data
        return byteArrayOf(0x00, 0x00, 0x00, 0x01) + data
    }

    override fun close() {
        runCatching { codec.stop() }
        runCatching { codec.release() }
    }

}
