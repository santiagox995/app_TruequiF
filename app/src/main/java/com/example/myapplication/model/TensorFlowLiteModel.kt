package com.example.myapplication.model

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.io.IOException

class TensorFlowLiteModel(context: Context) {

    private var interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context))
    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("siamese_trueques.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(embeddingA: FloatArray, embeddingB: FloatArray): Float {
        val inputA = arrayOf(embeddingA)
        val inputB = arrayOf(embeddingB)
        val output = Array(1) { FloatArray(1) }
        interpreter.run(arrayOf(inputA, inputB), output)
        return output[0][0]
    }
}
