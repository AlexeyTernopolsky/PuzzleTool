@file:Suppress("EnumEntryName")

package com.alextern.puzzletool

enum class PuzzleColor {
    invalid, violet, red, yellow, green, blue
}

enum class PuzzleType {
    invalid, normal, bomb, grenade
}

data class PuzzleElement(val color: PuzzleColor, val type: PuzzleType)

class Puzzle(val numColumns: Int, val numRows: Int, copyArray: Array<PuzzleElement>? = null) {
    constructor(numColumns: Int, numRows: Int, stringPuzzle: String) : this(numColumns, numRows) {
        var pos = 0
        (0 until numRows).forEach { y ->
            (0 until numColumns).forEach { x ->
                var element = stringPuzzle.get(pos)
                val puzzleColor = when (element) {
                    'V' -> PuzzleColor.violet
                    'R' -> PuzzleColor.red
                    'B' -> PuzzleColor.blue
                    'G' -> PuzzleColor.green
                    'Y' -> PuzzleColor.yellow
                    else -> PuzzleColor.invalid
                }

                pos++
                element = stringPuzzle.get(pos)
                val puzzleType = when (element) {
                    'n' -> PuzzleType.normal
                    'b' -> PuzzleType.bomb
                    'g' -> PuzzleType.grenade
                    else -> PuzzleType.invalid
                }

                updateElement(x, y, puzzleColor, puzzleType)
                pos += 2
              }

            pos++
        }
    }

    private val elements = copyArray?.clone()
        ?: Array(numRows * numColumns) {
            PuzzleElement(
                color = PuzzleColor.invalid,
                type = PuzzleType.invalid
            )
        }

    fun updateElement(x: Int, y: Int, color: PuzzleColor, type: PuzzleType) {
        elements[y * numColumns + x] = PuzzleElement(color, type)
    }

    override fun toString(): String {
        val buffer = StringBuilder()
        (0 until numRows).forEach { y ->
            (0 until numColumns).forEach { x ->
                val element = elements[y * numColumns + x]
                when (element.color) {
                    PuzzleColor.invalid -> buffer.append('I')
                    PuzzleColor.violet -> buffer.append('V')
                    PuzzleColor.red -> buffer.append('R')
                    PuzzleColor.blue -> buffer.append('B')
                    PuzzleColor.green -> buffer.append('G')
                    PuzzleColor.yellow -> buffer.append('Y')
                }

                when (element.type) {
                    PuzzleType.normal -> buffer.append('n')
                    PuzzleType.invalid -> buffer.append('i')
                    PuzzleType.bomb -> buffer.append('b')
                    PuzzleType.grenade -> buffer.append('g')
                }

                buffer.append(' ')
            }
            buffer.append("\n")
        }

        return buffer.toString()
    }

    fun clone() = Puzzle(numColumns, numRows, elements)

    operator fun get(x: Int, y: Int) = if (x >= 0 && y>=0 && x < numColumns && y < numRows)
            elements[y * numColumns + x]
        else
            PuzzleElement(PuzzleColor.invalid, PuzzleType.invalid)

    operator fun set(x: Int, y: Int, element: PuzzleElement) {
        elements[y * numColumns + x] = element
    }

    fun isValid():Boolean {
        for (element in elements) {
            if (element.type == PuzzleType.invalid)
                return false
        }

        return true
    }
}

