package com.example.videostreamingapp

sealed class ConnectionState() {
    data object Idle : ConnectionState()
    data object Connecting : ConnectionState()
    data class Streaming(val framesSent: Long, val fps: Float) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}