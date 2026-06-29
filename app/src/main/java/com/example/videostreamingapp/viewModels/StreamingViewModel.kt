package com.example.videostreamingapp.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videostreamingapp.ConnectionState
import com.example.videostreamingapp.FrameData
import com.example.videostreamingapp.FrameQueue
import com.example.videostreamingapp.encoder.H264Encoder
import com.example.videostreamingapp.socket.StreamingSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class StreamingViewModel : ViewModel() {
    private var frameQueue = FrameQueue(capacity = 2)
    private val socketManager = StreamingSocketManager()
    private var encoder: H264Encoder? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var streamingJob: Job? = null
    private var framesSentCount = 0L
    private var fpsWindowStart = System.currentTimeMillis()
    private var fpsWindowCount = 0

    fun onFrameCaptured(nv21: ByteArray, width: Int, height: Int) {
        if (_connectionState.value !is ConnectionState.Streaming) return
        viewModelScope.launch {
            frameQueue.send(FrameData(nv21, width, height, System.currentTimeMillis()))
        }
    }

    fun connect(ip: String, port: Int) {
        if (_connectionState.value is ConnectionState.Connecting) return
        frameQueue = FrameQueue(capacity = 2)
        _connectionState.value = ConnectionState.Connecting

        streamingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                socketManager.connect(ip, port)
                withContext(Dispatchers.Main) {
                    _connectionState.value = ConnectionState.Streaming(0, 0f)
                }
                streamLoop()
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
                }
            }
        }
    }

    private suspend fun streamLoop() {
        frameQueue.asFlow().collect { frame ->
            try { if (encoder != null && (encoder!!.width != frame.width || encoder!!.height != frame.height)) {
                encoder?.close()
                encoder = null
            }

                val enc = encoder ?: H264Encoder(frame.width, frame.height).also { encoder = it }
                val nalUnits = enc.encode(frame.nv21)
                for (nal in nalUnits) {
                    socketManager.sendFrame(nal)
                    updateStats()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    _connectionState.value = ConnectionState.Error(e.message ?: "Connection lost")
                }
                disconnect()
            }
        }
    }

    private suspend fun updateStats() {
        framesSentCount++
        fpsWindowCount++
        val now = System.currentTimeMillis()
        val elapsed = now - fpsWindowStart
        if (elapsed >= 1000) {
            val fps = fpsWindowCount * 1000f / elapsed
            withContext(Dispatchers.Main) {
                _connectionState.value = ConnectionState.Streaming(framesSentCount, fps)
            }
            fpsWindowCount = 0
            fpsWindowStart = now
        }
    }

    fun disconnect() {
        streamingJob?.cancel()
        socketManager.disconnect()
        frameQueue.close()
        encoder?.close()
        encoder = null
        _connectionState.value = ConnectionState.Idle
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}
