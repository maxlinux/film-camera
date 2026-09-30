package dev.codex.filmcamera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

class CameraController(private val context: Context, private val owner: LifecycleOwner) {
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    private val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
    private val videoCapture = VideoCapture.withOutput(recorder)
    private var provider: ProcessCameraProvider? = null
    private var recording: Recording? = null

    fun bind(previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            provider = future.get().also { cameraProvider ->
                cameraProvider.unbindAll()
                val preview = androidx.camera.core.Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, videoCapture)
            }
        }, executor)
    }

    fun takePhoto(look: FilmLook, onSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, stampedName("FILM", "jpg"))
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Film Camera")
        }
        val options = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
        imageCapture.takePicture(options, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                val uri = result.savedUri ?: return onError("Camera did not return a photo URI")
                if (look != FilmLook.ORIGINAL) applyLookToSavedPhoto(uri, look)
                onSaved(uri)
            }
            override fun onError(exception: ImageCaptureException) = onError(exception.message ?: "Could not save photo")
        })
    }

    fun startVideo(withAudio: Boolean, onStarted: () -> Unit, onFinished: (Uri?) -> Unit, onError: (String) -> Unit) {
        val file = File(context.cacheDir, stampedName("FILM", "mp4"))
        var pending: PendingRecording = videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file).build())
        if (withAudio) pending = pending.withAudioEnabled()
        recording = pending.start(executor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> onStarted()
                is VideoRecordEvent.Finalize -> {
                    recording = null
                    if (event.hasError()) onError(event.cause?.message ?: "Could not record video")
                    else onFinished(copyVideoToGallery(file))
                }
            }
        }
    }

    fun stopVideo() { recording?.stop() }

    private fun applyLookToSavedPhoto(uri: Uri, look: FilmLook) {
        runCatching {
            val bitmap = context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream) ?: return
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                bitmap.withLook(look).compress(Bitmap.CompressFormat.JPEG, 95, output)
            }
        }
    }

    private fun copyVideoToGallery(file: File): Uri? = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, stampedName("FILM", "mp4"))
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Film Camera")
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        file.delete()
        uri
    }.getOrNull()

    private fun stampedName(prefix: String, extension: String) = "$prefix-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.$extension"
}

/** Applies a selected original film look to a bitmap when an app later chooses in-memory capture processing. */
fun Bitmap.withLook(look: FilmLook): Bitmap {
    if (look == FilmLook.ORIGINAL) return this
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { output ->
        Canvas(output).drawBitmap(this, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(look.matrix))
        })
    }
}
