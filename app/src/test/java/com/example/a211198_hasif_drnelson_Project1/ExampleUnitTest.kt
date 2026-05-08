// Package declaration — must match the folder path under src/test/java.
package com.example.a211198_hasif_drnelson_Project1

// JUnit's @Test annotation — marks a method as a unit test the runner should execute.
import org.junit.Test

// Pulls in JUnit's assertion helpers (assertEquals, assertTrue, assertNotNull, etc.)
// so we can verify expected vs actual results inside our tests.
import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * "Local" means this test runs on your computer's JVM (no Android device or emulator
 * needed), so it's fast — perfect for testing pure Kotlin logic such as ViewModels
 * or data classes that don't rely on the Android framework.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    // @Test tells JUnit "run this method as a test".
    // The method name describes what we're checking — here, that addition is correct.
    @Test
    fun addition_isCorrect() {
        // assertEquals(expected, actual): fails the test if 2 + 2 doesn't equal 4.
        // First argument is what we expect, second is the value we're testing.
        assertEquals(4, 2 + 2)
    }
}