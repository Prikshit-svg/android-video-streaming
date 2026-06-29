#Demo video:- https://drive.google.com/file/d/1OJxfYEDDJyM0mqFR5RKXXKvg9EYXUXki/view?usp=drivesdk

# Android Video Streaming App

A real-time video streaming system that captures live camera video on Android and transmits it to a desktop receiver over a local WiFi network using H.264 hardware encoding.



## Architecture Overview


Android Device                          Desktop (Python)
─────────────────────────────────────   ──────────────────────────
CameraX (YUV_420_888)                        
    │                                        
    ▼                                        
YUV → NV21 conversion                        
    │                                        
    ▼                                        
NV21 → NV12 conversion                       
    │                                        
    ▼                                        
MediaCodec H.264 encoder (hardware)          
    │                                        
    ▼                                        
TCP socket (length-prefixed NAL units) ──► pyav H.264 decoder
                                             │
                                             ▼
                                        OpenCV display window

## Requirements

### Android
- Android 8.0 (API 26) or higher
- Camera permission
- WiFi connection on the same local network as the desktop

### Desktop
- Python 3.8+
- Libraries: av, opencv-python

## Build Instructions

### Android App

1. Open VideoStreamingApp/ in Android Studio
2. Let Gradle sync complete
3. Connect your Android device via USB with Developer Mode enabled
4. Click **Run** (Shift+F10)

### Desktop Receiver

Install dependencies:
pip install av opencv-python

## Running the App

### Step 1 — Find your desktop's IP address

**Windows:**
ipconfig

Look for **IPv4 Address** under your WiFi adapter. Example: 192.168.1.5

### Step 2 — Start the desktop receiver

python receiver.py
You will see:
Listening on 0.0.0.0:8080...

### Step 3 — Start the Android app

1. Launch the app on your phone
2. Grant camera permission when prompted
3. Enter your desktop's IP address and port `8080`
4. Tap **Connect & Stream**

The desktop window will open and display the live video feed within 1–2 seconds.

### Step 4 — Stop streaming

Tap **Stop** in the Android app, or press `Q` in the desktop window.

## Project Structure

VideoStreamingApp/
├── app/src/main/java/com/example/videostreamingapp/
│   ├── camera/
│   │   ├── CameraPreviewScreen.kt     # CameraX preview + frame capture
│   │   └── ToNv21ByteArray.kt         # YUV_420_888 → NV21 conversion
│   ├── encoder/
│   │   └── H264Encoder.kt             # MediaCodec H.264 hardware encoder
│   ├── socket/
│   │   └── StreamingSocketManager.kt  # TCP socket connection + framing
│   ├── viewModels/
│   │   └── StreamingViewModel.kt      # MVVM core — connects all components
│   ├── permissions/
│   │   └── CameraPermission.kt        # Runtime camera permission handling
│   ├── ConnectionState.kt             # Sealed class: Idle/Connecting/Streaming/Error
│   ├── FrameQueue.kt                  # Bounded coroutine channel between camera and encoder
│   ├── StreamingControls.kt           # Jetpack Compose UI
│   └── MainActivity.kt                # Entry point
│
receiver.py                            # Python desktop receiver
README.md

## Limitations and Assumptions

- Both devices must be on the same local WiFi network
- Only one desktop receiver can connect at a time
- The RTSP bonus task is not implemented — streaming uses TCP with custom length-framed NAL units
- Port 8080 must be open and not blocked by firewall on the desktop
- Tested on Android 13, Python 3.12, Windows 11




