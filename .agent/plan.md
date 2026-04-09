# Project Plan

A jogging app homepage based on the provided image. The app should have features like instant workouts, weekend run planning with routes, and a bottom navigation bar with Home, Maps, Record, Groups, and You sections. The design should be modern, following Material Design 3 guidelines with a dark theme as shown in the image.

## Project Brief

# Project Brief: RunTrack MVP

## Features
1.  **Instant Workouts Carousel**: A high-visibility horizontal carousel on the dashboard allowing users to quickly start pre-defined activities (e.g., Brisk Walk) with duration indicators.
2.  **Weekend Run Discovery**: A dedicated "Plan Your Weekend Run" section featuring local route suggestions with high-quality images, distance stats, and estimated times.
3.  **M3 Bottom Navigation**: A standard Material Design 3 navigation bar providing quick access to Home, Maps, Record, Groups, and the user's Profile ("You").
4.  **Activity Quick-Action**: A prominent Floating Action Button (FAB) or centralized "Record" button for immediate GPS tracking of jogging sessions.

## High-Level Technical Stack
-   **Kotlin**: The primary language for modern Android development.
-   **Jetpack Compose**: Used for building the Material Design 3 UI components and the dark-themed dashboard.
-   **Navigation Compose**: Handles transitions between the five main sections of the app.
-   **Coil**: Efficiently loads and caches images for the route discovery cards and user profiles.
-   **Kotlin Symbol Processing (KSP)**: Optimized code generation for UI and dependency management.

## UI Design Image
![UI Design](C:/Users/hasif/AndroidStudioProjects/a211198_Hasif_DrNelson_Lab12/input_images/image_0.jpeg)

## Implementation Steps
**Total Duration:** 31m 42s

### Task_1_Theme_Navigation: Set up the Material 3 dark theme, Edge-to-Edge display, and the primary navigation structure including the Bottom Navigation Bar with five destinations (Home, Maps, Record, Groups, You).
- **Status:** COMPLETED
- **Updates:** - Material 3 Dark Theme: Implemented a custom `RunTrackTheme` in `Theme.kt` using a dark color palette with vibrant orange (#FF5722) as the primary color.
- **Acceptance Criteria:**
  - M3 Dark Theme with vibrant colors implemented
  - Edge-to-Edge display enabled
  - Navigation between all 5 tabs is functional
  - Bottom Navigation Bar matches Material Design 3 guidelines
- **Duration:** 7m 53s

### Task_2_Home_TopBar_Workouts: Implement the Home screen's top section including the Top App Bar (with profile, chat, search, and notification icons) and the 'Instant Workouts' horizontal carousel.
- **Status:** COMPLETED
- **Updates:** - Top App Bar: Implemented a custom top bar with the "Home" title, a centrally positioned profile picture (using Coil's `AsyncImage`), and action icons for Chat, Search, and Notifications on the right.
- **Acceptance Criteria:**
  - Top App Bar matches the design in image_0.jpeg
  - 'Instant Workouts' section features a horizontal carousel with workout cards
  - The implemented UI must match the design provided in C:/Users/hasif/AndroidStudioProjects/a211198_Hasif_DrNelson_Lab12/input_images/image_0.jpeg
- **Duration:** 6m 41s

### Task_3_Home_WeekendRun_FAB: Implement the 'Plan Your Weekend Run' discovery section with detailed route cards and the prominent orange Floating Action Button for recording activities.
- **Status:** COMPLETED
- **Updates:** - Duplicate FABs Resolved: Removed the duplicate Floating Action Button from `MainActivity.kt`. Only one orange '+' FAB exists, correctly positioned at the `BottomEnd` (bottom-right) of the `HomeScreen`.
- **Acceptance Criteria:**
  - 'Plan Your Weekend Run' section matches the layout in image_0.jpeg
  - Route cards display images (using Coil), distance, and time stats
  - Orange '+' FAB is correctly positioned
  - The implemented UI must match the design provided in C:/Users/hasif/AndroidStudioProjects/a211198_Hasif_DrNelson_Lab12/input_images/image_0.jpeg
- **Duration:** 3m 43s

### Task_4_Final_Polish_Verify: Create an adaptive app icon, perform a final review of the UI against the design image, and verify the application's stability.
- **Status:** IN_PROGRESS
- **Updates:** Reopening Task_4_Final_Polish_Verify to perform the final verification after the fixes. 
The critic agent reported two FABs: one at the bottom right (BottomEnd) and one at the bottom center (BottomCenter), with the latter partially clipped by the navigation bar. 
Action: Remove the duplicate FAB in `MainActivity.kt` and ensure only the correct FAB in `HomeScreen` remains, positioned at `BottomEnd` as shown in `image_0.jpeg`. Also, ensure it doesn't create any visual artifacts in the navigation bar. 
Verify the final app icon and overall UI alignment with `image_0.jpeg`.
- **Acceptance Criteria:**
  - Adaptive app icon matching the jogging theme is created
  - App builds and runs successfully without crashes
  - UI alignment with image_0.jpeg is confirmed
  - All existing tests pass
- **Duration:** 13m 25s

