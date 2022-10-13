package com.alextern.puzzletool

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

@Suppress("EnumEntryName")
enum class ConverterType {
    kNormal, kMoonPuzzle, kPuzzleDuel, kMasterPuzzle,
}

private var imageClassifier: ImageClassifier? = null
private var imageProcessor = ImageProcessor.Builder().build()

class BitmapToPuzzleConverter(
    private val bitmap: Bitmap,
    context: Context,
    type: ConverterType = ConverterType.kNormal
) {
    private val kXStart: Int
    private val kYStart: Int
    private val kNumRows: Int
    private val kNumColumns: Int
    private val kCellHeight: Int
    private val kCellWidth: Int

    init {
        when (type) {
            ConverterType.kNormal-> {
                kXStart = 32
                kYStart = 1124
                kNumRows = 5
                kNumColumns = 7
                kCellHeight = 145
                kCellWidth = 145
            }
            ConverterType.kMasterPuzzle-> {
                kXStart = 32
                kYStart = 1412
                kNumRows = 5
                kNumColumns = 7
                kCellHeight = 145
                kCellWidth = 145
            }
            ConverterType.kPuzzleDuel -> {
                kXStart = 32
                kYStart = 1340
                kNumRows = 7
                kNumColumns = 7
                kCellHeight = 145
                kCellWidth = 145
            }
            ConverterType.kMoonPuzzle -> {
                kXStart = 17  // 1064
                kYStart = 1195 // 1942
                kNumRows = 5
                kNumColumns = 7
                kCellHeight = 149
                kCellWidth = 149
            }
        }

        if (imageClassifier == null) {
            val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
                .setScoreThreshold(0.6f)
                .setMaxResults(1)

            val baseOptionsBuilder = BaseOptions.builder().setNumThreads(1)
            optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

            val modelName = "puzzles.tflite"

            imageClassifier =
                ImageClassifier.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        }
     }

    fun analyze(): Puzzle {
        val buffer = StringBuilder()
        val unknownTiles = arrayListOf<Bitmap>()
        (0 until kNumRows).forEach { y ->
            (0 until kNumColumns).forEach { x ->
                val bmp = Bitmap.createBitmap(bitmap, kXStart + x * kCellWidth, kYStart + y * kCellHeight, kCellWidth, kCellHeight)

                // Preprocess the image and convert it into a TensorImage for classification.
                val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bmp))
                val imageCp = imageClassifier
                if (imageCp != null) {
                    val results = imageCp.classify(tensorImage)
                    if (results.isEmpty() || results.first().categories.isEmpty()) {
                        unknownTiles.add(bmp)
                    } else {
                        val classification = results.first().categories.first().label
                        buffer.append(classification)
                        buffer.append(' ')
                    }
                }
            }
            buffer.append('\n')
        }

        return if (unknownTiles.isEmpty()) {
            Puzzle(
                numColumns = kNumColumns,
                numRows = kNumRows,
                stringPuzzle = buffer.toString()
            )
        } else {
            val puzzle = Puzzle(kNumColumns, kNumRows)
            puzzle.unknownTiles = unknownTiles
            puzzle
        }
    }

    fun cellCoordinate(x: Int, y: Int) : Pair<Int, Int> {
        return Pair(
            kXStart + x * kCellWidth,
            kYStart + y * kCellHeight
        )
    }
}