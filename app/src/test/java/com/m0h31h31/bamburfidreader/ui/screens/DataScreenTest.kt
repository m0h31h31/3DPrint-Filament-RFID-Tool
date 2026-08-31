package com.m0h31h31.bamburfidreader.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataScreenTest {
    @Test
    fun totalRemainingGramsReturnsSingleSpoolWeight() {
        assertEquals(911, totalRemainingGrams(listOf(911)))
    }

    @Test
    fun totalRemainingGramsSumsMergedSpools() {
        assertEquals(911, totalRemainingGrams(listOf(300, 400, 211)))
    }

    @Test
    fun totalRemainingGramsPreservesZero() {
        assertEquals(0, totalRemainingGrams(listOf(0)))
    }

    @Test
    fun totalRemainingGramsReturnsUnknownForMissingWeights() {
        assertNull(totalRemainingGrams(listOf(500, null)))
        assertNull(totalRemainingGrams(emptyList()))
    }
}
