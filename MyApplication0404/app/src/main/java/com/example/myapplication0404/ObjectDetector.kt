package com.example.myapplication0404

import android.content.Context
import android.graphics.RectF
import android.util.Log
import ai.onnxruntime.*
import android.graphics.Bitmap
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.FloatBuffer

data class DetectionResult(val classIndex: Int, val score: Float, val box: RectF)

class ObjectDetector(context: Context) {
    private val ortEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val labels: List<String>
    private val inputName: String

    init {
        // Загрузка модели
        val modelBytes = context.assets.open("best.onnx").readBytes()
        session = ortEnvironment.createSession(modelBytes)

        // Получение имени входного тензора
        inputName = session.inputNames.iterator().next()

        // Загрузка labels.txt из assets
        labels = context.assets.open("labels.txt").bufferedReader().use(BufferedReader::readLines)
    }
    fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
        val inputSize = 640
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val floatArray = FloatArray(3 * inputSize * inputSize)

        var pixelIndex = 0
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = resized.getPixel(x, y)
                val r = (pixel shr 16 and 0xFF) / 255.0f
                val g = (pixel shr 8 and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                floatArray[pixelIndex] = r
                floatArray[pixelIndex + inputSize * inputSize] = g
                floatArray[pixelIndex + 2 * inputSize * inputSize] = b
                pixelIndex++
            }
        }

        return floatArray
    }

    fun detect(inputTensor: FloatArray): List<DetectionResult> {
        val inputSize = 640
        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        val floatBuffer = FloatBuffer.wrap(inputTensor)

        val tensor = OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape)
        val results = session.run(mapOf(inputName to tensor))

        // Получаем [1, 14, 8400] → Array<Array<FloatArray>>
        @Suppress("UNCHECKED_CAST")
        val output = results[0].value as Array<Array<FloatArray>>
        val outputArray = output[0] // [14][8400]

        val detections = mutableListOf<DetectionResult>()

        for (i in 0 until 8400) {
            val x = outputArray[0][i]
            val y = outputArray[1][i]
            val w = outputArray[2][i]
            val h = outputArray[3][i]
            val objConf = outputArray[4][i]

            if (objConf < 0.6f) continue // фильтр слабых объектов

            // Найдем класс с максимальным score
            var maxScore = 0f
            var classIdx = -1
            for (j in 5 until 14) {
                val classScore = outputArray[j][i]
                if (classScore > maxScore) {
                    maxScore = classScore
                    classIdx = j - 5
                }
            }

            // Итоговая уверенность: objConf * classScore (по правилам YOLO)
            val finalScore = objConf * maxScore
            if (finalScore > 0.65f && classIdx in labels.indices){
                val left = (x - w / 2).coerceIn(0f, inputSize.toFloat())
                val top = (y - h / 2).coerceIn(0f, inputSize.toFloat())
                val right = (x + w / 2).coerceIn(0f, inputSize.toFloat())
                val bottom = (y + h / 2).coerceIn(0f, inputSize.toFloat())
                val boxWidth = right - left
                val boxHeight = bottom - top
                if (boxWidth < 30f || boxHeight < 30f) continue

                detections.add(
                    DetectionResult(
                        classIndex = classIdx,
                        score = finalScore,
                        box = RectF(left, top, right, bottom)
                    )
                )
            }
        }

        return applyNMS(detections, iouThreshold = 0.6f)
    }

    private fun applyNMS(detections: List<DetectionResult>, iouThreshold: Float = 0.5f): List<DetectionResult> {
        val sorted = detections.sortedByDescending { it.score }.toMutableList()
        val result = mutableListOf<DetectionResult>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)

            val it = sorted.iterator()
            while (it.hasNext()) {
                val other = it.next()
                if (iou(best.box, other.box) > iouThreshold) {
                    it.remove()
                }
            }
        }
        return result
    }

    private fun iou(a: RectF, b: RectF): Float {
        val left = maxOf(a.left, b.left)
        val top = maxOf(a.top, b.top)
        val right = minOf(a.right, b.right)
        val bottom = minOf(a.bottom, b.bottom)

        val intersection = maxOf(0f, right - left) * maxOf(0f, bottom - top)
        val union = a.width() * a.height() + b.width() * b.height() - intersection
        return if (union == 0f) 0f else intersection / union
    }


    fun getLabel(index: Int): String {
        return labels.getOrNull(index) ?: "Unknown"
    }
}