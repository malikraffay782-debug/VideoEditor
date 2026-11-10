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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videoeditor.app.ui.EditorScreen
import com.videoeditor.core.PerformanceDetector
import com.videoeditor.core.model.Project
import com.videoeditor.coreui.theme.VideoEditorTheme
import com.videoeditor.project.ProjectRepository

class MainActivity : ComponentActivity() {
    private val projectRepository = ProjectRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoEditorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D0D0D)
                ) {
                    AppRoot(projectRepository)
                }
            }
        }
    }
}

@Composable
fun AppRoot(repository: ProjectRepository) {
    var currentProject by remember { mutableStateOf<Project?>(null) }

    if (currentProject != null) {
        EditorScreen(
            project = currentProject!!,
            onBack = { currentProject = null }
        )
    } else {
        HomeScreen(
            onNewProject = {
                val p = repository.createProject("Untitled Project")
                currentProject = p
            }
        )
    }
}

@Composable
fun HomeScreen(onNewProject: () -> Unit) {
    val context = LocalContext.current
    val capabilities = remember { PerformanceDetector.detect(context) }

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
            text = "Phase 2 — Media & Timeline",
            color = Color(0xFFB0B0B0),
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Mode: ${capabilities.recommendedMode}",
            color = Color(0xFF80CBC4),
            fontSize = 14.sp
        )
        Text(
            text = "RAM: ${capabilities.totalRamMb} MB · Preview: ${capabilities.maxPreviewHeight}p",
            color = Color(0xFF888888),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onNewProject,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7))
        ) {
            Text("New Project", color = Color.Black, fontWeight = FontWeight.SemiBold)
        }
    }
}
