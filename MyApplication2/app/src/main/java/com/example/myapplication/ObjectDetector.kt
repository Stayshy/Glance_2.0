import ai.onnxruntime.*
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.nio.FloatBuffer

class ObjectDetector(context: Context) {
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession
    private val modelFile = "best.onnx"

    companion object {
        private const val TAG = "ObjectDetector"
    }

    init {
        loadModel(context)
    }

    private fun loadModel(context: Context) {
        try {
            val modelBytes = context.assets.open(modelFile).readBytes()
            val sessionOptions = OrtSession.SessionOptions()
            ortSession = ortEnv.createSession(modelBytes, sessionOptions)
            Log.i(TAG, "Model loaded. Input names: ${ortSession.inputNames}, Output names: ${ortSession.outputNames}")
        } catch (e: Exception) {
            Log.e(TAG, "Model loading failed", e)
            throw RuntimeException("Failed to load ONNX model", e)
        }
    }

    fun detect(bitmap: Bitmap): Array<FloatArray>? {
        return try {
            val inputs = prepareInput(bitmap)
            val results = ortSession.run(inputs)
            processOutput(results)
        } catch (e: Exception) {
            Log.e(TAG, "Detection failed", e)
            null
        } finally {
            // Освобождаем ресурсы
            inputs.values.forEach { it.close() }
        }
    }

    private fun prepareInput(bitmap: Bitmap): Map<String, OnnxTensor> {
        val inputArray = preprocessBitmapToFloatArray(bitmap)
        val inputShape = longArrayOf(1, 3, bitmap.height.toLong(), bitmap.width.toLong())

        // Создаем FloatBuffer из массива
        val floatBuffer = FloatBuffer.wrap(inputArray)

        return try {
            val inputTensor = OnnxTensor.createTensor(
                ortEnv,
                floatBuffer,
                inputShape
            )
            mapOf(ortSession.inputNames.first() to inputTensor)
        } catch (e: Exception) {
            Log.e(TAG, "Tensor creation failed", e)
            throw e
        }
    }

    private fun preprocessBitmapToFloatArray(bitmap: Bitmap): FloatArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val floatArray = FloatArray(3 * bitmap.width * bitmap.height)
        for (i in pixels.indices) {
            // Нормализация [0-255] -> [0-1] и порядок каналов RGB
            floatArray[i] = ((pixels[i] shr 16) and 0xFF) / 255.0f // R
            floatArray[i + bitmap.width * bitmap.height] = ((pixels[i] shr 8) and 0xFF) / 255.0f // G
            floatArray[i + 2 * bitmap.width * bitmap.height] = (pixels[i] and 0xFF) / 255.0f // B
        }
        return floatArray
    }

    private fun processOutput(results: OrtSession.Result): Array<FloatArray> {
        return try {
            val outputName = ortSession.outputNames.first()
            val outputValue = results.get(outputName)

            when (outputValue) {
                is Array<*> -> outputValue as Array<FloatArray>
                is OnnxTensor -> {
                    val tensorValue = outputValue.floatBuffer.array()
                    arrayOf(tensorValue) // Преобразуем в ожидаемый формат
                }
                else -> throw IllegalStateException("Unexpected output type: ${outputValue?.javaClass}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Output processing failed", e)
            throw RuntimeException("Failed to process model output", e)
        }
    }

    fun close() {
        try {
            ortSession?.close()
            ortEnv?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
        }
    }
}