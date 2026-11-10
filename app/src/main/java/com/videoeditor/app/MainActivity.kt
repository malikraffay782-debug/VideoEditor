package com.videoeditor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videoeditor.core.PerformanceDetector
import com.videoeditor.coreui.theme.VideoEditorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoEditorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D0D0D)
                ) {
                    Phase1HomeScreen()
                }
            }
        }
    }
}

@Composable
fun Phase1HomeScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val capabilities = PerformanceDetector.detect(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Video Editor",
            color = Color(0xFF4FC3F7),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Phase 1 Foundation",
            color = Color(0xFFB0B0B0),
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Device Profile",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "RAM: ${capabilities.totalRamMb} MB (avail ${capabilities.availableRamMb} MB)",
            color = Color(0xFFB0B0B0),
            fontSize = 14.sp
        )
        Text(
            text = "CPU cores: ${capabilities.cpuCores}",
            color = Color(0xFFB0B0B0),
            fontSize = 14.sp
        )
        Text(
            text = "Low-RAM device: ${capabilities.isLowRamDevice}",
            color = Color(0xFFB0B0B0),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Mode: ${capabilities.recommendedMode}",
            color = Color(0xFF4FC3F7),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Max preview: ${capabilities.maxPreviewHeight}p",
            color = Color(0xFFB0B0B0),
            fontSize = 14.sp
        )
        Text(
            text = "3D allowed: ${capabilities.allow3d}",
            color = Color(0xFFB0B0B0),
            fontSize = 14.sp
        )
    }
}
