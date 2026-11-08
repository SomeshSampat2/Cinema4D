# Cinema4D — project notes

## Build & verify
- Build: `./gradlew :app:assembleDebug` (AGP 9.1.0, compileSdk 37, JDK 21 — required by Compose BOM 2026.03.01 + Coil 3.6.3)
- Install: `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk`
- Launch flow for manual check: tap movie card → tap showtime → tap seat on map → 3D seat view

## 3D seat view (hall.glb + Seat3DView.kt)
- `tools/build_hall.py` regenerates `app/src/main/assets/models/hall.glb` + `app/src/main/assets/seat_map.json` (run with Blender: `/Applications/Blender.app/Contents/MacOS/Blender --background --python tools/build_hall.py -- <glb> <json>`)
- Seat backs project into the bottom ~20% of frame — the camera lookAt aims ~2.6m below screen center so they clear the bottom sheet.
- SceneView loads a default neutral IBL (`environments/neutral/neutral_ibl.ktx`) — keep it; removing it makes the hall pitch black.
- `ImageNode` must NOT be rotated — `normal = Direction(0,0,-1)` already faces the audience; an extra `rotation = Rotation(y=180)` double-flips it invisible.
- Seat visibility comes from: SeatFabric material patch (baseColor ~[0.4,0.05,0.07] + emissiveFactor [0.14,0.011,0.018] @ strength 0.22), warm ceiling point lights (8×, intensity ~4000), ISO ~380 exposure.
- The screen poster is center-cropped to screen aspect and brightness-boosted ×3.2 in `Seat3DView` — don't add a point light at the screen, it blows out the poster.
- Mini seat-map overlay in 3D mode is 104dp — taller covers the seat backs.

## Known gaps
- Default Android launcher icon.
- Camera glides via `lookAt(smooth=true)`; no seat-tap haptics/sound.
