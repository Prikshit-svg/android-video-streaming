# Performance Results

## Test Environment

| Parameter                  | Value                                      |
|----------------------------|--------------------------------------------|
| Android Device             | Redmi Note 10T 5g                          |
| Android Version            | Android 13                                 |
| Desktop OS                 | Windows 11                                 |
| Network                    | Local WiFi (2.4GHz / 5GHz)                 |
| Camera Resolution          | 640 × 480                                  |
| Distance (phone to router) | same room                                  |
 
## Streaming Performance

| Metric              | Value                                           |
|---------------------|-------------------------------------------------|
| Average FPS         | 10 fps                                          |
| Video Bitrate       | 2 Mbps (configured)                             |
| Encoding            | H.264 hardware (MediaCodec)                     |
| Approximate Latency | ~200–400ms (visual estimate)                    |
| Dropped Frames      | Minimal — DROP_OLDEST strategy keeps stream live|



## CPU Usage

| Component                    | CPU Impact                                   |
|------------------------------|----------------------------------------------|
| H.264 encoding               | Very low — handled by hardware chip          |
| Frame conversion (NV21→NV12) | Low — runs on IO dispatcher                  |
| TCP sending                  | Low — single coroutine on IO dispatcher      |
| UI rendering (Compose)       | Low — only updates once per second for stats |



## Bandwidth Usage

| Scenario                       | Data Rate                              |
|--------------------------------|----------------------------------------|
| Static scene (little movement) | ~200–400 Kbps (P-frames are tiny)      |
| Active scene (lots of movement)| ~1.5–2 Mbps (approaching I-frame size) |
| Peak (keyframe every 1 second) | ~2 Mbps                                |

H.264 inter-frame compression means bandwidth varies with scene content — far more efficient than JPEG which would use 2–4 Mbps constantly regardless of movement.



## Comparison: JPEG vs H.264

| Metric                  | JPEG (Stage 3)     | H.264 (Current)            |
|-------------------------|--------------------|----------------------------|
| Encoding                | Software (CPU)     | Hardware (MediaCodec chip) |
| CPU usage               | High               | Very low                   |
| Bandwidth (static scene)| ~2–4 Mbps constant | ~200–400 Kbps              |
| Bandwidth (active scene)| ~2–4 Mbps constant | ~1.5–2 Mbps                |
| Inter-frame compression | None               | Yes (P-frames)             |
| Quality at same bitrate | Lower              | Higher                     |



## Limitations

- Single client only — one desktop receiver at a time
- No adaptive bitrate — fixed 2 Mbps target regardless of network conditions
- No RTSP — uses custom TCP protocol, not viewable in VLC/ffplay directly
- Latency increases if WiFi signal is weak
- First frame takes 1–2 seconds to appear (H.264 decoder pipeline initialization)