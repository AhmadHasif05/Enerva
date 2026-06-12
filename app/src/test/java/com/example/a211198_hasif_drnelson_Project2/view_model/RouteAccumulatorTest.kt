package com.example.a211198_hasif_drnelson_Project2.view_model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteAccumulatorTest {

    @Test
    fun `first fix is always kept and adds no distance`() {
        val acc = RouteAccumulator()
        val kept = acc.addFix(1.0, 103.0, accuracyM = 5f, timeMs = 0L)
        assertTrue(kept)
        assertEquals(1, acc.points.size)
        assertEquals(0.0, acc.distanceKm, 1e-9)
    }

    @Test
    fun `first fix is kept even when accuracy is worse than the gate`() {
        val acc = RouteAccumulator(minAccuracyM = 25f)
        val kept = acc.addFix(1.0, 103.0, accuracyM = 40f, timeMs = 0L)
        assertTrue(kept)
        assertEquals(1, acc.points.size)
    }

    @Test
    fun `a later fix worse than the accuracy gate is rejected`() {
        val acc = RouteAccumulator(minAccuracyM = 25f)
        acc.addFix(1.0, 103.0, accuracyM = 5f, timeMs = 0L)
        val kept = acc.addFix(1.001, 103.0, accuracyM = 40f, timeMs = 1000L)
        assertFalse(kept)
        assertEquals(1, acc.points.size)
    }

    @Test
    fun `a sub-threshold move is rejected as jitter`() {
        val acc = RouteAccumulator(minMoveM = 4.0)
        acc.addFix(1.0, 103.0, accuracyM = 5f, timeMs = 0L)
        val kept = acc.addFix(1.000001, 103.0, accuracyM = 5f, timeMs = 1000L)
        assertFalse(kept)
        assertEquals(1, acc.points.size)
        assertEquals(0.0, acc.distanceKm, 1e-9)
    }

    @Test
    fun `a real move is kept and accrues distance`() {
        val acc = RouteAccumulator()
        acc.addFix(1.0, 103.0, accuracyM = 5f, timeMs = 0L)
        val kept = acc.addFix(1.001, 103.0, accuracyM = 5f, timeMs = 1000L)
        assertTrue(kept)
        assertEquals(2, acc.points.size)
        assertEquals(0.111, acc.distanceKm, 0.002)
    }

    @Test
    fun `null accuracy passes the accuracy gate`() {
        val acc = RouteAccumulator()
        val kept = acc.addFix(1.0, 103.0, accuracyM = null, timeMs = 0L)
        assertTrue(kept)
    }

    @Test
    fun `null accuracy still hits the jitter gate on a later fix`() {
        val acc = RouteAccumulator()
        acc.addFix(1.0, 103.0, accuracyM = null, timeMs = 0L)
        // Second fix has null accuracy but barely moves — jitter gate must reject it.
        val kept = acc.addFix(1.000001, 103.0, accuracyM = null, timeMs = 1000L)
        assertFalse(kept)
        assertEquals(1, acc.points.size)
        assertEquals(0.0, acc.distanceKm, 1e-9)
    }

    @Test
    fun `reset clears points and distance`() {
        val acc = RouteAccumulator()
        acc.addFix(1.0, 103.0, accuracyM = 5f, timeMs = 0L)
        acc.addFix(1.001, 103.0, accuracyM = 5f, timeMs = 1000L)
        acc.reset()
        assertEquals(0, acc.points.size)
        assertEquals(0.0, acc.distanceKm, 1e-9)
    }
}
