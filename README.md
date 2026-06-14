# Football Live Wallpaper

An interactive, high-performance 3D bouncing footballs live wallpaper for Android. Built with native Android and Canvas rendering (supports 60 FPS simulations), this wallpaper responds to screen touches and simulates realistic gravity, bounce physics, and movement.

## Features

- **Interactive 3D Simulation**: Bouncing footballs that react to collisions and device boundaries.
- **High Performance**: Optimized rendering cycle utilizing standard Canvas callbacks at ~60 FPS.
- **Minimal Battery Consumption**: Renders only when visible and pauses automatically when the device is locked or another app is open.
- **Privacy First**: Fully offline, requests zero dangerous permissions, collects no data.

## Getting Started

### Prerequisites

- Android SDK (API 24+)
- Android Studio or Gradle to build the project.

### Building & Installation

To build and run the app locally:

1. Clone this repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/FootballLiveWallpaper.git
   ```
2. Open the project in Android Studio.
3. Build the project or run it directly on your device.

To build the release version for Google Play Store:
```bash
./gradlew bundleRelease
```

## Privacy Policy

The privacy policy for this application is available at:
[Privacy Policy](privacy-policy.html) (or hosted via GitHub Pages).

## License

This project is licensed under the MIT License - see the LICENSE file for details.
