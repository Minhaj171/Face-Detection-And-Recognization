# Face Detection & Recognition (Android)

A native Android application that detects faces in real time and compares/matches a
captured face against a reference image, built as a foundation for identity-verification
style features (similar to the face-matching used in my AI Interview Platform project).

## Key Features

- Real-time face detection using the camera (CameraX + ML Kit Face Detection)
- Face comparison/matching between a live capture and a reference image
  (`FaceMatcher`, `CompareImageActivity`)
- Person detection flow (`DetectPersonActivity`)
- Result reporting via a dialog UI (`ResultDialogFragment`)

## Tech Stack

- **Language:** Java
- **Platform:** Native Android (CameraX)
- **ML/Vision:** Google ML Kit Face Detection, Firebase ML Vision, Google Play Services Vision
- **On-device ML:** TensorFlow Lite (+ GPU delegate)
- **Backend services:** Firebase (Analytics, ML)

## Running the Project

1. Open the project in Android Studio.
2. Add your own `google-services.json` (Firebase project config) to the `app/` module.
3. Let Gradle sync and download dependencies.
4. Run on a physical device with a camera (recommended for real-time detection).

## Potential Use Cases

Identity verification, attendance systems, and any workflow needing on-device face
detection and matching without sending images to a remote server.
