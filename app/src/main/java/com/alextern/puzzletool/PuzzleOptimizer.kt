package com.alextern.puzzletool

import kotlin.math.max

//data class PuzzleMove(val fromX: Int, val fromY: Int, val )

class PuzzleOptimizer(private val origin: Puzzle) {
    private val variants = mutableListOf<Variant>()

    fun optimize() {
        val numRows = origin.numRows
        val numColumns = origin.numColumns
        var workPuzzle = origin.clone()
        (0 until numRows).forEach { y ->
            (0 until numColumns).forEach { x ->
                if (x < numColumns - 1) {
                    workPuzzle = checkVariant(workPuzzle, x, y, x + 1, y)
                }
                if (y < numRows - 1) {
                    workPuzzle = checkVariant(workPuzzle, x, y, x, y + 1)
                }
            }
        }

        variants.forEach {
            if (it.similarCount < 4)
                it.calculateDamage()
        }

        variants.sortBy { it.damage }
    }

    private fun checkVariant(workPuzzle: Puzzle, fromX: Int, fromY: Int, toX: Int, toY: Int): Puzzle {
        swap(workPuzzle, fromX, fromY, toX, toY)
        val count = max(similarCount(workPuzzle, fromX, fromY), similarCount(workPuzzle, toX, toY))
        if (count >= 2) {
            variants.add(Variant(workPuzzle, fromX, fromY, toX, toY, count))
            return origin.clone()
        } else {
            swap(workPuzzle, toX, toY, fromX, fromY)
        }

        return workPuzzle
    }

    data class PartPos(val x: Int, val y: Int)

    class Variant(val puzzle: Puzzle, val fromX: Int, val fromY: Int, val toX: Int, val toY: Int, val similarCount: Int) {
        private var changed = mutableSetOf(PartPos(fromX, fromY), PartPos(toX, toY))
        private var _cost = 0
        private var _damage = 0

        val cost: Int
            get() = _cost

        val damage: Int
            get() = _damage

        fun calculateDamage() {
            var same = 1
            (0 until puzzle.numRows).forEach { y ->
                var currentType = puzzle[0, y].type
                (1 until puzzle.numColumns).forEach { x ->
                    //if (puzzle[])
                }
            }
        }
    }
}

fun similarCount(puzzle: Puzzle, x: Int, y: Int): Int {
    val originColor = puzzle[x, y].color
    var result = 0
    var count = 0
    var distance = 1
    var same = true
    while (distance < 3 && same) {
        if (puzzle[x + distance, y].color == originColor) count++ else same = false
        distance++
    }
    distance = 1
    same = true
    while (distance < 3 && same) {
        if (puzzle[x - distance, y].color == originColor) count++ else same = false
        distance++
    }
    if (count >= 2)
        result = count
    count = 0
    distance = 1
    same = true
    while (distance < 3 && same) {
        if (puzzle[x, y + distance].color == originColor) count++ else same = false
        distance++
    }
    distance = 1
    same = true
    while (distance < 3 && same) {
        if (puzzle[x, y - distance].color == originColor) count++ else same = false
        distance++
    }
    if (count >= 2)
        result += count
    return result
}

private fun swap(puzzle: Puzzle, fromX: Int, fromY: Int, toX: Int, toY: Int) {
    val prev1 = puzzle[fromX, fromY]
    val prev2 = puzzle[toX, toY]
    puzzle[fromX, fromY] = prev2
    puzzle[toX, toY] = prev1
}

