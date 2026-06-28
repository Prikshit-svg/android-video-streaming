import socket
import struct
import av
import cv2

HOST = '0.0.0.0'
PORT = 8080

def recv_exact(conn, n):
    buf = bytearray()
    while len(buf) < n:
        chunk = conn.recv(n - len(buf))
        if not chunk:
            raise ConnectionError("Socket closed")
        buf.extend(chunk)
    return bytes(buf)

def main():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen(1)
        print(f"Listening on {HOST}:{PORT}...")

        conn, addr = s.accept()
        print(f"Connected: {addr}")

        codec = av.CodecContext.create('h264', 'r')

        with conn:
            while True:
                length_bytes = recv_exact(conn, 4)
                length = struct.unpack('>I', length_bytes)[0]
                nal_data = recv_exact(conn, length)

                packets = codec.parse(nal_data)
                for packet in packets:
                    frames = codec.decode(packet)
                    for frame in frames:
                        img = frame.to_ndarray(format='bgr24')
                        cv2.imshow('H264 Stream', img)
                        if cv2.waitKey(1) & 0xFF == ord('q'):
                            return

        cv2.destroyAllWindows()

if __name__ == '__main__':
    main()