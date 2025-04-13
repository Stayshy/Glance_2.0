package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ImageClassifier(context: Context) {
    private var interpreter: Interpreter? = null
    var labels = emptyList<String>()  // было val → стало var


    init {
        try {
            val model = FileUtil.loadMappedFile(context, "best_32.tflite")
            interpreter = Interpreter(model)
            labels = context.assets.open("labels.txt").bufferedReader().readLines()
        } catch (e: Exception) {
            Log.e("ImageClassifier", "Ошибка загрузки модели: ${e.message}")
            labels = emptyList()
        }
    }

    fun detectObjects(bitmap: Bitmap): List<DetectionResult> {
        interpreter ?: return emptyList()

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 320, 320, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 10, 4), DataType.FLOAT32)
        interpreter?.run(byteBuffer, outputBuffer.buffer)

        val results = mutableListOf<DetectionResult>()
        val predictions = outputBuffer.floatArray

        for (i in 0 until 10) {
            val left = predictions[i * 4]
            val top = predictions[i * 4 + 1]
            val right = predictions[i * 4 + 2]
            val bottom = predictions[i * 4 + 3]

            val label = labels.getOrNull(i) ?: "Неизвестный"
            val score = predictions[i * 4 + 3]

            if (score > 0.5) { // Отсеиваем слабые результаты
                results.add(
                    DetectionResult(
                        RectF(left, top, right, bottom),
                        label,
                        score
                    )
                )
            }
        }
        return results
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * 320 * 320 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(320 * 320)
        bitmap.getPixels(intValues, 0, 320, 0, 0, 320, 320)

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        return byteBuffer
    }
}
