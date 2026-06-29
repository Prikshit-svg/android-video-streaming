package com.example.videostreamingapp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videostreamingapp.viewModels.CamViewModel
import com.example.videostreamingapp.viewModels.StreamingViewModel


@Composable
fun StreamingScreen(viewModel: StreamingViewModel,camViewModel : CamViewModel) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    var ip by remember { mutableStateOf("192.168.1.") }
    var port by remember { mutableStateOf("8080") }
StreamingControls( state = state,
    ip = ip,
    port = port,
    onIpChange = { ip = it },
    onPortChange = { port = it },
    onConnectClick = { viewModel.connect(ip, port.toIntOrNull() ?: 8080) },
    onDisconnectClick = {
        viewModel.disconnect() },
    {camViewModel.changeCam()}

    )
}
@Composable
fun StreamingControls(state: ConnectionState,
                      ip: String,
                      port: String,
                      onIpChange: (String) -> Unit,
                      onPortChange: (String) -> Unit,
                      onConnectClick: () -> Unit,
                      onDisconnectClick: () -> Unit,
                      onCamSwitchClick: () -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = ip,
            onValueChange = onIpChange,
            label = { Text("Desktop IP") }
        )
        OutlinedTextField(
            value = port,
            onValueChange = onPortChange,
            label = { Text("Port") }
        )

        when (state) {
            is ConnectionState.Idle, is ConnectionState.Error -> {
                Button(onClick = onConnectClick) {
                    Row(
                        Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Start
                    ) {
                    Text("Connect & Stream")
                        Button(onClick = onCamSwitchClick) {Text("Switch") }
                    }
                }
                if (state is ConnectionState.Error) {
                    Text(state.message, color = Color.Red)
                }
            }
            is ConnectionState.Connecting -> {
                CircularProgressIndicator()
                Text("Connecting...")
            }
            is ConnectionState.Streaming -> {
                val s = state as ConnectionState.Streaming
                Text("Streaming — ${s.framesSent} frames, ${"%.1f".format(s.fps)} fps")

                    Button(onClick = onDisconnectClick) { Text("Stop") }
                    Spacer(Modifier.padding(20.dp))


            }
        }
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewStreamingControl() {
    // We use a Column to see how it looks in different states
    Column {
        // 1. Idle State Preview
        Text("Idle State:", Modifier.padding(8.dp))
        StreamingControls(
            state = ConnectionState.Idle,
            ip = "192.168.1.10",
            port = "8080",
            onIpChange = {},
            onPortChange = {},
            onConnectClick = {},
            onDisconnectClick = {},{}
        )

        // 2. Streaming State Preview
        Text("Streaming State:", Modifier.padding(8.dp))
        StreamingControls(
            state = ConnectionState.Streaming(framesSent = 1240, fps = 30.5f),
            ip = "192.168.1.10",
            port = "8080",
            onIpChange = {},
            onPortChange = {},
            onConnectClick = {},
            onDisconnectClick = {},
            onCamSwitchClick = {}
        )
    }
}

