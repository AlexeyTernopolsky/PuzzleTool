package com.alextern.puzzletool

import kotlin.math.max

//data class PuzzleMove(val fromX: Int, val fromY: Int, val )

private val bombFigure = PuzzleOptimizer.Figure(0)

@Suppress("EnumEntryName")
enum class Action { moveRight, moveDown, tap, notFound }
typealias PartPos = Pair<Int, Int>

class PuzzleOptimizer(private val origin: Puzzle) {
    private val variants = mutableListOf<Variant>()

    var actionX = 0
    var actionY = 0
    var actionType = Action.notFound
    var maxPoints: Int = 0

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
                if (origin[x, y].type == PuzzleType.bomb || origin[x, y].type == PuzzleType.grenade) {
                    variants.add(Variant(workPuzzle, x, y, x, y, 0))
                    workPuzzle = origin.clone()
                }
            }
        }

        variants.forEach {
            it.calculateDamage()
        }

        val variant = variants.maxByOrNull { it.damage }
        if (variant != null) {
            actionX = variant.fromX
            actionY = variant.fromY
            maxPoints = variant.damage
            actionType = when {
                variant.isTap -> Action.tap
                variant.fromX != variant.toX -> Action.moveRight
                variant.fromY != variant.toY -> Action.moveDown
                else -> Action.notFound
            }
        }
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

    data class Figure(var count: Int, var horIncluded: Boolean = false, var swapFound: Boolean= false)

    class Variant(private val puzzle: Puzzle, val fromX: Int, val fromY: Int, val toX: Int, val toY: Int, val similarCount: Int) {
        private var changed = mutableSetOf(PartPos(fromX, fromY), PartPos(toX, toY))
        private var field: Array<Array<Figure?>> = Array(puzzle.numColumns) {  Array(puzzle.numRows) { null } }
        private var figures = mutableListOf<Figure>()
        private var multiplier = 1.0
        private var _damage = 0

        val damage: Int
            get() = _damage

        val isTap: Boolean
            get() = fromX == toX && fromY == toY

        fun calculateDamage() {
            // Check on tapping
            if (isTap) {
                if (puzzle[fromX, fromY].type == PuzzleType.bomb) {
                    activateBomb(fromX, fromY)
                } else if (puzzle[fromX, fromY].type == PuzzleType.grenade) {
                    activateGrenade(fromX, fromY)
                }

                removeFigures()
            }

            while(true) {
                if (!oneTurn())
                    break
                else
                    multiplier += 0.1
            }
        }

        private fun oneTurn():Boolean {
            // clear previous
            figures.clear()
            field.forEach {
                it.fill(null)
            }

            horizontalProcessing()
            verticalProcessing()
            bombProcessing()
            removeFigures()
            return figures.isNotEmpty()
        }

        private fun horizontalProcessing() {
            (0 until puzzle.numRows).forEach { y ->
                (0 until puzzle.numColumns).forEach { x ->
                    val curColor = puzzle[x, y].color
                    if (curColor != PuzzleColor.invalid && field[x][y] == null) {
                        var same = 1
                        for (xAdd in (x + 1 until puzzle.numColumns)) {
                            if (puzzle[xAdd, y].color == curColor)
                                same++
                            else
                                break
                        }

                        if (same >= 3) {
                            val figure = Figure(if (same > 5) 5 else same)
                            figures.add(figure)
                            for (xAdd in (x until x + same)) {
                                field[xAdd][y] = figure
                            }
                        }
                    }
                }
            }
        }

        private fun verticalProcessing() {
            (0 until puzzle.numColumns).forEach { x ->
                (0 until puzzle.numRows).forEach { y ->
                    val curColor = puzzle[x, y].color
                    if (curColor != PuzzleColor.invalid && (field[x][y] == null || !field[x][y]!!.horIncluded)) {
                        var same = 1
                        for (yAdd in (y + 1 until puzzle.numRows)) {
                            if (puzzle[x, yAdd].color == curColor)
                                same++
                            else
                                break
                        }

                        if (same >= 3) {
                            val oldFigure = field[x][y]
                            if (oldFigure != null) {
                                oldFigure.count = 5
                            }
                            val figure = oldFigure ?: Figure(if (same > 5) 5 else same)
                            figure.horIncluded = true
                            if (oldFigure == null)
                                figures.add(figure)

                            for (yAdd in (y until y + same)) {
                                field[x][yAdd] = figure
                            }
                        }
                    }
                }
            }
        }

        private fun bombProcessing() {
            (0 until puzzle.numRows).forEach { y ->
                (0 until puzzle.numColumns).forEach { x ->
                    if (field[x][y] != null) {
                        if (puzzle[x, y].type == PuzzleType.bomb) {
                            activateBomb(x, y)
                        } else if (puzzle[x, y].type == PuzzleType.grenade) {
                            activateGrenade(x, y)
                        }
                    }
                }
            }
        }

        private fun activateBomb(x: Int, y: Int) {
            fun handlePuz(x: Int, y: Int) {
                if (field[x][y] == null) {
                    field[x][y] = bombFigure

                    if (puzzle[x, y].type == PuzzleType.bomb) {
                        activateBomb(x, y)
                    } else if (puzzle[x, y].type == PuzzleType.grenade) {
                        activateGrenade(x, y)
                    }
                }
            }

            if (field[x][y] == null)
                field[x][y] = bombFigure

            if (x > 0)
                handlePuz(x - 1, y)
            if (y > 0)
                handlePuz(x, y - 1)
            if (x < puzzle.numColumns - 1)
                handlePuz(x + 1, y)
            if (y < puzzle.numRows - 1)
                handlePuz(x, y + 1)
        }

        private fun activateGrenade(xP: Int, yP: Int) {
            val color = puzzle[xP, yP].color
            (0 until puzzle.numRows).forEach { y ->
                (0 until puzzle.numColumns).forEach { x ->
                    if (puzzle[x, y].color == color) {
                        field[x][y] = bombFigure
                        if (puzzle[x, y].type == PuzzleType.bomb) {
                            activateBomb(x, y)
                        }
                    }
                }
            }
        }

        private fun removeFigures() {
            if (figures.isNotEmpty())
                multiplier += 0.1 * (figures.count() - 1)
            var erasedElements = 0
            val newChanged = mutableSetOf<PartPos>()
            (0 until puzzle.numColumns).forEach { x ->
                var add = 0
                (0 until puzzle.numRows).forEach { y ->
                    val figure = field[x][y]
                    if (figure != null) {
                        erasedElements++
                        if (figure.count > 3 && !figure.swapFound) {
                            if (changed.contains(PartPos(x, y))) {
                                val newType = if (figure.count == 4) PuzzleType.bomb else PuzzleType.grenade
                                val color = puzzle[x, y].color
                                puzzle[x, y - add] = PuzzleElement(color, newType)
                                figure.swapFound = true
                            } else {
                                add++
                                newChanged.add(PartPos(x, y))
                            }
                        } else {
                            add++
                            newChanged.add(PartPos(x, y))
                        }
                    } else if (add > 0) {
                        puzzle[x, y - add] = puzzle[x, y]
                    }
                }

                if (add > 0) {
                    (puzzle.numRows - add until puzzle.numRows).forEach { y ->
                        puzzle[x, y] = PuzzleElement(PuzzleColor.invalid, PuzzleType.invalid)
                    }
                }
            }

            _damage += (erasedElements.toFloat() * 100 * multiplier).toInt()
            changed = newChanged
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

