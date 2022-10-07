package com.alextern.puzzletool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import kotlin.math.abs

@Suppress("EnumEntryName")
enum class ConverterType {
    kNormal, kPuzzleDuel, kMasterPuzzle
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
    private val templates: List<Template>

    init {
        when (type) {
            ConverterType.kNormal-> {
                kXStart = 32
                kYStart = 1124
                kNumRows = 5
                kNumColumns = 7
                kCellHeight = 145
                kCellWidth = 145
                templates = createDuelTemplateList()
            }
            ConverterType.kMasterPuzzle-> {
                kXStart = 32
                kYStart = 1412
                kNumRows = 5
                kNumColumns = 7
                kCellHeight = 145
                kCellWidth = 145
                templates = createDuelTemplateList()
            }
            ConverterType.kPuzzleDuel -> {
                kXStart = 32
                kYStart = 1340
                kNumRows = 7
                kNumColumns = 7
                kCellHeight = 145
                kCellWidth = 145
                templates = createDuelTemplateList()
            }
        }

        if (imageClassifier == null) {
            val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
                .setScoreThreshold(0.5f)
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

    private fun findTemplate(x: Int, y: Int): Template? {
        var distance = 10
        while (distance <= 50) {
            for (template in templates) {
                var valid = false
                for (point in template.points) {
                    val xColor = kXStart + x * kCellWidth + (point.x * kCellWidth).toInt()
                    val yColor = kYStart + y * kCellHeight + (point.y * kCellHeight).toInt()
                    val probeColor = bitmap.getPixel(xColor, yColor)
                    valid = checkColorSame(probeColor, point, distance)
                    if (!valid)
                        break
                }

                if (valid) {
                    return template
                }
            }

            distance += 10
        }
        return null
    }

    private fun checkColorSame(probeColor: Int, point: TemplatePoint, distance: Int): Boolean {
        val probeRed = Color.red(probeColor)
        val probeGreen = Color.green(probeColor)
        val probeBlue = Color.blue(probeColor)
        val output = FloatArray(3)
        Color.RGBToHSV(probeRed, probeGreen, probeBlue, output)
        val probeHue = output[0]

        return abs(probeHue - point.hue) < distance
    }
}

data class TemplatePoint(
    val x: Float,
    val y: Float,
    var hue: Float
) {
    constructor(x: Float, y: Float, red: Int, green: Int, blue: Int): this(x, y, 0f) {
        val output = FloatArray(3)
        Color.RGBToHSV(red, green, blue, output)
        hue = output[0]
    }
}

data class Template(
    val points: List<TemplatePoint>,
    val color: PuzzleColor,
    val type: PuzzleType
)

private fun createDuelTemplateList(): List<Template> = listOf(
    // grenade red and green ???
    Template(  // grenade blue
        points = listOf(
            TemplatePoint(x = .517f, y = .172f, red = 144, green = 161, blue = 179),
            TemplatePoint(x = .628f, y = .248f, red = 64, green = 81, blue = 106),
            TemplatePoint(x = .345f, y = .49f, red = 38, green = 157, blue = 220),
            TemplatePoint(x = .365f, y = .752f, red = 33, green = 96, blue = 172),
        ),
        color = PuzzleColor.blue,
        type = PuzzleType.grenade
    ),
    Template(  // grenade violet
        points = listOf(
            TemplatePoint(x = .6f, y = .172f, red = 186, green = 186, blue = 204),
            TemplatePoint(x = .7f, y = .248f, red = 92, green = 91, blue = 127),
            TemplatePoint(x = .393f, y = .483f, red = 95, green = 53, blue = 154),
            TemplatePoint(x = .248f, y = .876f, red = 84, green = 46, blue = 144)
        ),
        color = PuzzleColor.violet,
        type = PuzzleType.grenade
    ),
    Template(  // grenade yellow
        points = listOf(
            TemplatePoint(x = .628f, y = .159f, red = 124, green = 130, blue = 148),
            TemplatePoint(x = .683f, y = .310f, red = 229, green = 205, blue = 103),
            TemplatePoint(x = .428f, y = .641f, red = 109, green = 116, blue = 132),
            TemplatePoint(x = .221f, y = .855f, red = 66, green = 49, blue = 12),
        ),
        color = PuzzleColor.yellow,
        type = PuzzleType.grenade
    ),
    Template(  // bomb violet
        points = listOf(
            TemplatePoint(x = .593f, y = .248f, hue = 200f),
            TemplatePoint(x = .468f, y = .510f, hue = 290f),
            TemplatePoint(x = .220f, y = .641f, hue = 265f),
            TemplatePoint(x = .496f, y = .875f, hue = 219f),
        ),
        color = PuzzleColor.violet,
        type = PuzzleType.bomb
    ),
    Template(  // bomb blue
        points = listOf(
            TemplatePoint(x = .614f, y = .269f, red = 128, green = 141, blue = 150),
            TemplatePoint(x = .283f, y = .483f, red = 45, green = 92, blue = 144),
            TemplatePoint(x = .620f, y = .793f, red = 40, green = 52, blue = 110),
            TemplatePoint(x = .448f, y = .565f, red = 71, green = 203, blue = 226)
        ),
        color = PuzzleColor.blue,
        type = PuzzleType.bomb
    ),
    Template(  // bomb yellow
        points = listOf(
            TemplatePoint(x = .607f, y = .234f, hue = 210f),
            TemplatePoint(x = .476f, y = .503f, hue = 55f),
            TemplatePoint(x = .227f, y = .634f, hue = 34f),
            TemplatePoint(x = .496f, y = .867f, hue = 15f),
        ),
        color = PuzzleColor.yellow,
        type = PuzzleType.bomb
    ),
    Template(  // bomb green
        points = listOf(
            TemplatePoint(x = .593f, y = .234f, hue = 197f),
            TemplatePoint(x = .476f, y = .517f, hue = 66f),
            TemplatePoint(x = .186f, y = .710f, hue = 95f),
            TemplatePoint(x = .69f, y = .648f, hue = 180f),
        ),
        color = PuzzleColor.green,
        type = PuzzleType.bomb
    ),
    Template(  // bomb red
        points = listOf(
            TemplatePoint(x = .579f, y = .241f, hue = 200f),
            TemplatePoint(x = .476f, y = .510f, hue = 26f),
            TemplatePoint(x = .179f, y = .614f, hue = 341f),
            TemplatePoint(x = .634f, y = .758f, hue = 323f),
        ),
        color = PuzzleColor.red,
        type = PuzzleType.bomb
    ),
    Template(       // normal violet
        points = listOf(
            TemplatePoint(x = .179f, y = .524f, red = 90, green = 28, blue = 135),
            TemplatePoint(x = .49f, y = .234f, red = 208, green = 158, blue = 209),
            TemplatePoint(x = .814f, y = .524f, red = 93, green = 29, blue = 151),
            TemplatePoint(x = .496f, y = .821f, red = 82, green = 51, blue = 154)
        ),
        color = PuzzleColor.violet,
        type = PuzzleType.normal
    ),
    Template(   // normal red
        points = listOf(
            TemplatePoint(x = .2f, y = .365f, red = 168, green = 56, blue = 42),
            TemplatePoint(x = .786f, y = .352f, red = 199, green = 79, blue = 62),
            TemplatePoint(x = .207f, y = .634f, red = 90, green = 20, blue = 30),
            TemplatePoint(x = .752f, y = .655f, red = 125, green = 18, blue = 46),
        ),
        color = PuzzleColor.red,
        type = PuzzleType.normal
    ),
    Template(   // normal yellow
        points = listOf(
            TemplatePoint(x = .276f, y = .283f, red = 192, green = 143, blue = 24),
            TemplatePoint(x = .731f, y = .283f, red = 193, green = 152, blue = 38),
            TemplatePoint(x = .283f, y = .752f, red = 115, green = 55, blue = 27),
            TemplatePoint(x = .766f, y = .703f, red = 134, green = 54, blue = 31),
        ),
        color = PuzzleColor.yellow,
        type = PuzzleType.normal
    ),
    Template(   // normal green
        points = listOf(
            TemplatePoint(x = .193f, y = .276f, red = 66, green = 146, blue = 47),
            TemplatePoint(x = .731f, y = .276f, red = 85, green = 154, blue = 58),
            TemplatePoint(x = .510f, y = .786f, red = 29, green = 124, blue = 58),
        ),
        color = PuzzleColor.green,
        type = PuzzleType.normal
    ),
    Template(   // normal blue
        points = listOf(
            TemplatePoint(x = .496f, y = .207f, red = 103, green = 219, blue = 218),
            TemplatePoint(x = .172f, y = .558f, red = 36, green = 76, blue = 138),
            TemplatePoint(x = .807f, y = .552f, red = 24, green = 64, blue = 123),
            TemplatePoint(x = .49f, y = .821f, red = 33, green = 83, blue = 154),
        ),
        color = PuzzleColor.blue,
        type = PuzzleType.normal
    )
)

