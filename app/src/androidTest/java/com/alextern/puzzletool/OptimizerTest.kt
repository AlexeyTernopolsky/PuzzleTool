package com.alextern.puzzletool

import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test

@SmallTest
class OptimizerTest {
    @Test
    fun test1() {
        val puzzle1 = Puzzle(7, 7, stringPuzzle =
                "Vn Rn Vb Vn Bn Bn Rn \n" +
                "Rn Gn Vn Bn Gn Vn Rn \n" +
                "Bn Rn Bn Gn Bn Vn Bn \n" +
                "Bn Rn Rb Bn Gn Bn Vn \n" +
                "Yn Bn Vn Yn Bn Gn Bn \n" +
                "Rn Gn Rn Vn Bn Rn Vn \n" +
                "Vn Bn Vn Bn Yn Vn Rn \n")

        val optimizer = PuzzleOptimizer(puzzle1)
        optimizer.optimize()
        assertEquals("", "")
    }
}