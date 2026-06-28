package com.example.videostreamingapp

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class FrameQueue(capacity: Int = 2) {
    private val channel = Channel<FrameData>(
        capacity = capacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    suspend fun send(frame: FrameData) {
        channel.trySend(frame) // non-blocking; respects DROP OLDEST automatically
    }

    fun asFlow(): Flow<FrameData> = channel.receiveAsFlow()

    fun close() = channel.close()
}

data class FrameData(val nv21: ByteArray, val width: Int, val height: Int, val timestamp: Long)