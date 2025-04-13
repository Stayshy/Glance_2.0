package com.example.myapplication0404

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
        }

        setContent {
            CameraPreview()
        }
    }
}

@Composable
fun CameraPreview() {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val detector = remember { ObjectDetector(context) }
    var detectionResults by remember { mutableStateOf(emptyList<DetectionResult>()) }
    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }

    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                imageWidth = imageProxy.width
                                imageHeight = imageProxy.height
                                processImageProxy(imageProxy, detector) { results ->
                                    detectionResults = results
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            context as ComponentActivity,
                            cameraSelector,
                            preview,
                            imageCapture,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Ошибка камеры", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        BoundingBoxOverlay(
            results = detectionResults,
            previewWidth = imageWidth,
            previewHeight = imageHeight,
            labels = { detector.getLabel(it) },
            modifier = Modifier.fillMaxSize()
        )

        // Кнопка снимка
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            FloatingActionButton(
                onClick = {
                    val photoFile = File(
                        context.cacheDir,
                        "temp_photo.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exc: ImageCaptureException) {
                                Log.e("Camera", "Capture failed: ${exc.message}", exc)
                            }

                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                saveImageWithOverlay(context, bitmap, detectionResults) {
                                    detector.getLabel(it)
                                }
                            }
                        }
                    )
                },
                containerColor = Color.White,
                modifier = Modifier.size(72.dp)
            ) {}
        }
    }
}
@Composable
fun BoundingBoxOverlay(
    modifier: Modifier = Modifier,
    results: List<DetectionResult>,
    previewWidth: Int,
    previewHeight: Int,
    labels: (Int) -> String
) {
    Canvas(modifier = modifier) {
        val modelInputSize = 640f
        val scaleX = size.width / modelInputSize
        val scaleY = size.height / modelInputSize

        results.forEach { result ->
            val rect = result.box
            val left = rect.left * scaleX
            val top = rect.top * scaleY
            val right = rect.right * scaleX
            val bottom = rect.bottom * scaleY

            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4f)
            )

            drawContext.canvas.nativeCanvas.drawText(
                "${labels(result.classIndex)} %.2f".format(result.score),
                left,
                top - 10,
                Paint().apply {
                    color = android.graphics.Color.RED
                    textSize = 36f
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
            )
        }
    }
}

// ImageProxy → Bitmap
fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
    val inputSize = 640
    val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
    val input = FloatArray(3 * inputSize * inputSize)

    for (y in 0 until inputSize) {
        for (x in 0 until inputSize) {
            val px = resized.getPixel(x, y)
            val r = (px shr 16 and 0xFF).toFloat()
            val g = (px shr 8 and 0xFF).toFloat()
            val b = (px and 0xFF).toFloat()

            val offset = y * inputSize + x
            input[offset] = b // B
            input[inputSize * inputSize + offset] = g // G
            input[2 * inputSize * inputSize + offset] = r // R
        }
    }

    return input
}

// Основная обработка кадра
fun processImageProxy(
    image: ImageProxy,
    detector: ObjectDetector,
    onResult: (List<DetectionResult>) -> Unit
) {
    try {
        val bitmap = image.toBitmap()
        Log.d("Detection", "Картинка ${bitmap.width}x${bitmap.height}")

        val inputTensor = bitmapToFloatArray(bitmap)
        val results = detector.detect(inputTensor)
        Log.d("Detection", "Найдено объектов: ${results.size}")

        onResult(results)
    } catch (e: Exception) {
        Log.e("Detection", "Ошибка обработки: ${e.message}", e)
    } finally {
        image.close()
    }
}
        fun saveImageWithOverlay(
            context: Context,
            bitmap: Bitmap,
            detectionResults: List<DetectionResult>,
            labels: (Int) -> String
        ) {
            val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(outputBitmap)
            val paint = Paint().apply {
                color = android.graphics.Color.RED
                strokeWidth = 4f
                style = Paint.Style.STROKE
            }
            val textPaint = Paint().apply {
                color = android.graphics.Color.RED
                textSize = 36f
                isAntiAlias = true
            }

            val scaleX = bitmap.width / 640f
            val scaleY = bitmap.height / 640f

            detectionResults.forEach { result ->
                val rect = result.box
                val left = rect.left * scaleX
                val top = rect.top * scaleY
                val right = rect.right * scaleX
                val bottom = rect.bottom * scaleY
                canvas.drawRect(left, top, right, bottom, paint)
                canvas.drawText("${labels(result.classIndex)} %.2f".format(result.score), left, top - 10, textPaint)
            }

            val filename = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Camera")
            val file = File(picturesDir, filename)

            FileOutputStream(file).use {
                outputBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }

            // Добавим в медиатеку
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/jpeg"),
                null
            )}

