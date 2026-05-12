# Enerva 🏃‍♂️

**Enerva** is an Android application inspired by the Strava fitness tracking ecosystem. This project serves as an educational exploration into activity tracking, data visualization, and modern Android development practices.

# Problem Statement
Many people want a fit body and strong health so they can enjoy through daily routines with confidence. To achieve this, they turn to cardio activities like jogging, running, or brisk walking. Yet the journey often ends too soon — motivation fades, routines break, routes feel uncertain, progress is hard to measure, and without friends or a supportive circle, cardio feels more like a struggle than a habit. This matters because when people give up, they lose the chance to build lasting fitness, protect their heart health, and enjoy the quality of life that comes with consistent healthy habits.

# User Interface (UI) / Screen Roles
1. **Login Screen**: Access existing account using registered email.
2. **Home Screen**: Main dashboard displaying recent activities and daily workout recommendations.
3. **Profile Screen**: View in-depth personal statistics, achievements, and user profile information.
4. **Search Screen**: Search for running locations, friends to follow, or sports community groups.
5. **Message Screen**: Inbox hub for all conversations and social interactions.
6. **Notify Screen**: List of notifications regarding friend interactions, new challenges, and system updates.
7. **Settings Screen**: Manage account configurations, notification preferences, and privacy settings.
8. **Record Screen**: Primary interface for recording cardio activities (run/walk) in real-time with GPS.
9. **Activity Screen**: Detailed summary of a recently completed fitness activity.
10. **Groups Screen**: Manage participation in community challenges and sports clubs.
11. **Maps Screen**: Explore the map to find popular routes and plan activity paths.
12. **You Screen**: Progress dashboard showing weekly statistics and activity streaks.
13. **Chat Screen**: Active conversation interface for direct communication with other users.
14. **Edit Profile Screen**: Update personal information such as name, profile picture, and fitness goals.
15. **Signup Screen**: Create a new account for users beginning their fitness journey.
16. **Main Screen**: Onboarding screen introducing the application to new users.

## 🚀 Features
- **Activity Tracking:** Log and monitor fitness activities.
- **Data Persistence:** Local storage using **Room Database**.
- **Networking:** Integration with external APIs via **Retrofit**.
- **Camera Integration:** Using **CameraX** for capturing activity-related media.
- **Location Services:** Real-time tracking using **Google Play Services Location**.
- **Modern UI:** Built entirely with **Jetpack Compose** and **Material 3**.

## 🛠 Tech Stack
- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Navigation:** [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **Local Database:** [Room](https://developer.android.com/training/data-storage/room)
- **Asynchronous Programming:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
- **Network:** [Retrofit](https://square.github.io/retrofit/) & [Moshi](https://github.com/square/moshi)
- **Hardware APIs:** CameraX, Location API, DataStore (Preferences)

## 📂 Project Structure
- `view/`: Contains Compose screens and UI components.
- `viewmodel/`: Business logic and UI state management.
- `model/`: Data entities and Room database configuration.
- `network/`: API service definitions and Retrofit setup.

---
*Note: This project is for educational purposes and is not a commercial product.*
