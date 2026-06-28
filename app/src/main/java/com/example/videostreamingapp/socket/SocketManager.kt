package com.example.videostreamingapp.socket

import android.util.Log
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StreamingSocketManager {

    private var socket: Socket? = null
    private var outputStream: BufferedOutputStream? = null

    @Throws(IOException::class)
    fun connect(ip: String, port: Int, timeoutMs: Int = 5000) {
        val newSocket = Socket()
        newSocket.connect(InetSocketAddress(ip, port), timeoutMs)/* example use case:-
        connect(
            "192.168.1.20",
            5000,
            5000
        )
       Meaning:

        Connect to
        IP
        192.168.1.20
         Port
        5000
        Timeout
        5 seconds
        */
        newSocket.tcpNoDelay = true   // disable Nagle's algorithm (enabling it can cause frames to be lost as it tries to combine some data and sent it at once instead of frame by frame sending)
        socket = newSocket//Now the class stores the connected socket.
        outputStream = BufferedOutputStream(newSocket.getOutputStream())
    }

    @Throws(IOException::class)
    fun sendFrame(jpegBytes: ByteArray) {
        val out = outputStream ?: throw IOException("Not connected")
        val lengthHeader = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(jpegBytes.size).array()
        out.write(lengthHeader)
        out.write(jpegBytes)
        out.flush()
    }

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.w("StreamingSocketManager", "Error during disconnect", e)
        } finally {
            outputStream = null
            socket = null
        }
    }

    fun isConnected(): Boolean = socket?.isConnected == true && socket?.isClosed == false
}