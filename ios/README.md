# FortuneTeller iOS App

SwiftUI app mirroring RunForm's structure.

## Generate Xcode project

```powershell
brew install xcodegen  # on macOS
cd ios
xcodegen generate
open FortuneTeller.xcodeproj
```

If you do not use XcodeGen, open Xcode → Create a new iOS App → drag the `FortuneTeller/` source folder in.

## Files

- `FortuneTellerApp.swift` — app entry
- `ContentView.swift` — home screen with scope selector + camera/upload actions
- `PalmCameraView.swift` — AVFoundation live camera capture
- `PalmPhotoPicker.swift` — PhotoKit photo upload
- `ReadingResultView.swift` — fortune reading detail view
- `APIClient.swift` — multipart upload to `/analyze-palm`
- `PalmModels.swift` — Codable response models

## Configuration

`BACKEND_BASE_URL` in `Info.plist` controls API target. Defaults to `http://localhost:8000` for Debug. For device testing on a LAN, set it to your dev machine IP (and run the backend on `0.0.0.0`).
