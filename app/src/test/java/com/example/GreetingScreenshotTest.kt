package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.Book
import com.example.ui.HomeDashboardView
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleBooks = listOf(
        Book(
            id = 1,
            title = "Sapiens: A Brief History",
            author = "Yuval Noah Harari",
            description = "Syllabus drafted by AI to master Sapiens.",
            category = "History & Science",
            progressPercent = 35,
            isCustom = false
        )
    )
    composeTestRule.setContent {
      MyApplicationTheme {
        HomeDashboardView(
            books = sampleBooks,
            nickname = "Scholar",
            studyMinutes = 15,
            studyGoal = 30,
            onLogMinute = {},
            onBookSelect = {},
            onSyllabusBuilderClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
