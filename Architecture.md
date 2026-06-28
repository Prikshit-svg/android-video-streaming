# Architecture Documentation

## System Overview

The system consists of two components:

1. **Android App** — captures camera frames, encodes them in H.264, and transmits over TCP
2. **Python Desktop Receiver** — receives the TCP stream, decodes H.264, and displays live video

Both devices must be connected to the same local WiFi network.

## Android App Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture using Jetpack Compose for UI and Kotlin Coroutines for async operations.


                        UI Layer                         
         Jetpack Compose (StreamingControls.kt)          
   IP field │ Port field  │ Connect button │ Status text
                          │
                          ▼ 
                   observes StateFlow
                          │
                          ▼ 
                   ViewModel Layer                        
              StreamingViewModel.kt                       
                         │
                         ▼                                
  ConnectionState: Idle → Connecting → Streaming → Error 
  FrameQueue: Channel(capacity=2, DROP_OLDEST)           
  streamLoop: collects frames → encodes → sends          


│   Camera Layer  │             │     Network Layer       │
│                 │             │                         │
│ CameraX         │             │ StreamingSocketManager  │
│ ImageAnalysis   │             │ TCP Socket              │
│ YUV→NV21        │             │ 4-byte length framing   │
│ H264Encoder     │             │                         │
└──────────────             └────────────────────────┘

## Component Breakdown

### CameraX + Frame Capture (CameraPreviewScreen.kt, ToNv21ByteArray.kt)

- Uses CameraX Preview use case to display live viewfinder
- Uses CameraX ImageAnalysis use case to receive raw frames
- STRATEGY_KEEP_ONLY_LATEST backpressure — drops frames if the encoder is busy, prevents memory buildup
- Each frame is in YUV_420_888 format — converted to NV21 with stride-aware per-row copying to handle hardware memory padding

### Frame Queue (FrameQueue.kt)

- A Kotlin Channel with capacity 2 and DROP_OLDEST overflow strategy
- Sits between the camera callback (produces frames) and the stream loop (consumes frames)
- Decouples camera thread from the network/encoding thread
- If the network is slow, old frames are automatically dropped — keeping the stream live rather than building a backlog

### H.264 Encoder (H264Encoder.kt)

- Wraps Android MediaCodec API in synchronous mode
- Configured with COLOR_FormatYUV420SemiPlanar (NV12) input format
- NV21 → NV12 conversion: Y plane copied as-is, chroma VU pairs swapped to UV
- Outputs Annex-B H.264 NAL units (prepends 00 00 00 01 start code if not present)
- Created lazily on the first frame so dimensions are known
- Released in disconnect() to free the hardware chip

**Encoder settings:**
| Parameter        | Value                   |
|------------------|-------------------------|
| Codec            | H.264 (AVC)             |
| Input format     | NV12 (YUV420SemiPlanar) |
| Bitrate          | 2 Mbps                  |
| Frame rate       | 30 fps                  |
| I-frame interval | 1 second                |

### ViewModel (StreamingViewModel.kt)

- Owns the FrameQueue,H264Encoder, and StreamingSocketManager
- `connect() opens TCP socket and launches streamLoop() on Dispatchers.IO
- `streamLoop() collects frames from the queue, encodes each frame, sends all resulting NAL units
- `disconnect() cancels the coroutine job, closes the socket, closes the encoder
- `ConnectionState is a StateFlow — UI reacts automatically to state changes

### TCP Socket (StreamingSocketManager.kt)

- Simple client TCP socket connecting to desktop IP + port
- Each NAL unit is sent with a **4-byte big-endian length prefix** followed by the NAL data
- tcpNoDelay enabled — disables Nagle's algorithm to reduce latency
- No reconnection logic — on error, state moves to Error and user taps Connect again

### Connection State Machine

        ┌─────────────────────────────────┐
        │                                 │
   ┌────▼────┐    connect()    ┌──────────┴──┐
   │  Idle   ├────────────────►│ Connecting  │
   └────▲────┘                 └──────┬──────┘
        │                               │ socket open
        │                      ┌──────▼──────┐
        │    disconnect()      │  Streaming  │
        │◄─────────────────────┤  (fps, count)│
        │                      └──────┬──────┘
        │                               │ IOException
        │                      ┌──────▼──────┐
        └──────────────────────┤    Error    │
             user taps Connect └─────────────┘


## Desktop Receiver Architecture

TCP Socket (port 8080)
    │
    ▼
recv_exact(4 bytes) → length
    │
    ▼
recv_exact(length bytes) → NAL unit data
    │
    ▼
av.CodecContext.parse(data) → packets
    │
    ▼
av.CodecContext.decode(packet) → VideoFrame
    │
    ▼
frame.to_ndarray(format='bgr24') → NumPy array
    │
    ▼
cv2.imshow() → display


- recv_exact() loops until exactly N bytes are received — handles TCP's stream-based fragmentation
- av.CodecContext (pyav/FFmpeg) maintains decoder state across NAL units — required for P-frame reconstruction
- SPS and PPS NAL units are processed silently (no frame output) — they initialize the decoder
- First decoded frame appears after 1–2 seconds while the decoder pipeline fills



## Data Flow — One Frame End to End

1. CameraX delivers YUV_420_888 ImageProxy at ~30fps
2. toNv21ByteArray() copies Y plane row-by-row (respecting rowStride)
   then interleaves V,U chroma pairs → NV21 ByteArray
3. onFrameCaptured() sends FrameData into the FrameQueue channel
4. streamLoop() receives FrameData from channel (on Dispatchers.IO)
5. H264Encoder.feedInput() swaps VU→UV and queues to MediaCodec input buffer
6. MediaCodec hardware chip encodes the frame
7. H264Encoder.drainOutput() retrieves NAL unit(s) from MediaCodec output buffer
8. Each NAL unit gets Annex-B start code prepended (00 00 00 01)
9. StreamingSocketManager.sendFrame() writes [4-byte length][NAL data] to TCP socket
10. Python recv_exact() reads exactly that many bytes
11. pyav parses and decodes the NAL unit
12. Decoded frame converted to BGR NumPy array
13. OpenCV renders the frame in a window


## TCP Framing Protocol


┌─────────────────┬──────────────────────────────────────┐
│  Length (4 bytes    │         NAL Unit Data (N bytes)              │
│  big-endian u32     │  [00 00 00 01] [H.264 NAL payload]           │
└─────────────────┴──────────────────────────────────────┘


Simple, stateless, and self-delimiting. The receiver always knows exactly how many bytes to read next.



## Design Decisions

**Why CameraX over Camera2?**
CameraX provides a lifecycle-aware, higher-level API. ImageAnalysis with STRATEGY_KEEP_ONLY_LATEST gives automatic frame dropping with no extra code.

**Why MVVM?**
Separates UI from streaming logic. The ViewModel survives screen rotations — streaming continues uninterrupted if the user rotates the phone.

**Why a bounded FrameQueue with DROP_OLDEST?**
The encoder and network are slower than the camera. Without dropping, frames would queue indefinitely, causing increasing delay. DROP_OLDEST keeps the stream live — always showing the most recent frame.

**Why MediaCodec over software JPEG?**
Hardware H.264 encoding uses dedicated silicon — negligible CPU cost. Inter-frame compression (P-frames) reduces bandwidth by 5-10x compared to JPEG at the same quality.

**Why TCP over UDP?**
TCP is simpler and reliable on a local WiFi network. For a LAN streaming use case, the latency difference is negligible. UDP would be preferable for higher-latency or lossy networks (e.g. internet streaming).

**Why length-prefixed framing over raw stream?**
TCP is a byte stream with no built-in message boundaries. Without length prefixes, the receiver cannot tell where one NAL unit ends and the next begins. 4-byte length prefix is the simplest reliable solution.
