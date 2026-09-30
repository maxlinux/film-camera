package dev.codex.filmcamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FilmCameraApp() }
    }
}

private enum class CaptureMode { PHOTO, VIDEO }

@Composable
private fun FilmCameraApp() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var cameraAllowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var microphoneAllowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        cameraAllowed = result[Manifest.permission.CAMERA] == true
        microphoneAllowed = result[Manifest.permission.RECORD_AUDIO] == true
    }

    if (!cameraAllowed) {
        PermissionScreen { permissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }
    } else {
        CameraScreen(microphoneAllowed = microphoneAllowed, requestAudio = { permissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO)) }, owner = owner)
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF10100F)), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FILM CAMERA", color = Color(0xFFF8F2E9), fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Spacer(Modifier.height(18.dp))
            Text("Camera access is needed to compose and capture. Microphone access enables sound in video recordings.", color = Color(0xFFD5CEC3), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text("ALLOW CAMERA", color = Color(0xFF10100F), fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xFFE6C592)).clickable(onClick = onRequest).padding(horizontal = 24.dp, vertical = 14.dp))
        }
    }
}

@Composable
private fun CameraScreen(microphoneAllowed: Boolean, requestAudio: () -> Unit, owner: androidx.lifecycle.LifecycleOwner) {
    val context = LocalContext.current
    val controller = remember(context, owner) { CameraController(context, owner) }
    var look by remember { mutableStateOf(FilmLook.ORIGINAL) }
    var mode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var recording by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("Ready") }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx).also { preview -> controller.bind(preview) } },
            modifier = Modifier.fillMaxSize(),
        )
        // This translucent treatment updates instantly over the CameraX preview. Capture processing is intentionally
        // separate so the UI remains responsive on entry-level devices.
        FilmOverlay(look)

        Column(Modifier.fillMaxSize().padding(top = 52.dp, bottom = 30.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("FILM CAMERA", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text(if (recording) "● REC" else notice.uppercase(), color = if (recording) Color(0xFFFF6464) else Color.White.copy(alpha = .82f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Column(Modifier.fillMaxWidth()) {
                LookStrip(selected = look, onSelected = { look = it })
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    ModePill("PHOTO", mode == CaptureMode.PHOTO) { mode = CaptureMode.PHOTO }
                    Spacer(Modifier.width(10.dp))
                    ModePill("VIDEO", mode == CaptureMode.VIDEO) { mode = CaptureMode.VIDEO }
                }
                Text(
                    if (mode == CaptureMode.PHOTO) "LIVE LOOK + JPEG LOOK" else "LIVE LOOK PREVIEW · VIDEO SAVES ORIGINAL",
                    color = Color.White.copy(alpha = .78f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(look.label, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(82.dp), textAlign = TextAlign.Center)
                    Shutter(recording = recording, isVideo = mode == CaptureMode.VIDEO) {
                        if (mode == CaptureMode.PHOTO) {
                            controller.takePhoto(look, onSaved = { notice = "Saved" }, onError = { notice = it })
                        } else if (recording) {
                            controller.stopVideo()
                            notice = "Saving video"
                        } else {
                            if (!microphoneAllowed) requestAudio()
                            controller.startVideo(withAudio = microphoneAllowed, onStarted = { recording = true; notice = "Recording" }, onFinished = { recording = false; notice = "Saved" }, onError = { recording = false; notice = it })
                        }
                    }
                    Text(if (mode == CaptureMode.PHOTO) "JPG" else if (microphoneAllowed) "MP4 + AUD" else "MP4", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(82.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun FilmOverlay(look: FilmLook) {
    if (look == FilmLook.ORIGINAL) return
    Box(Modifier.fillMaxSize().background(look.wash.copy(alpha = look.washAlpha)))
    if (look == FilmLook.WARM_GRAIN || look == FilmLook.DOCUMENTARY) Canvas(Modifier.fillMaxSize().alpha(.16f)) {
        val step = 5.dp.toPx()
        var x = 0f
        while (x < size.width) { var y = (x % 4) * step; while (y < size.height) { drawCircle(Color(0xFFFFE5B5), radius = .65.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y)); y += step * 2 }; x += step * 2 }
    }
}

@Composable
private fun LookStrip(selected: FilmLook, onSelected: (FilmLook) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilmLook.entries.forEach { look ->
            Column(Modifier.width(88.dp).clickable { onSelected(look) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().aspectRatio(1.18f).clip(RoundedCornerShape(12.dp)).background(if (look == FilmLook.ORIGINAL) Color(0xFF5D6670) else look.wash).then(if (selected == look) Modifier.border(2.dp, Color(0xFFE6C592), RoundedCornerShape(12.dp)) else Modifier))
                Spacer(Modifier.height(6.dp))
                Text(look.label, color = Color.White, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
                Text(look.subtitle, color = Color.White.copy(.65f), fontSize = 9.sp, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ModePill(label: String, active: Boolean, onClick: () -> Unit) = Text(label, color = if (active) Color(0xFF171512) else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(if (active) Color(0xFFE6C592) else Color.Black.copy(.38f)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 9.dp))

@Composable
private fun Shutter(recording: Boolean, isVideo: Boolean, onClick: () -> Unit) = Box(Modifier.size(78.dp).border(3.dp, Color.White, CircleShape).padding(6.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
    Box(Modifier.size(if (recording) 34.dp else 58.dp).clip(if (recording) RoundedCornerShape(8.dp) else CircleShape).background(if (isVideo) Color(0xFFFF4F4F) else Color.White))
}
