# QR App - Secure Parking & Calling

A comprehensive Android application for generating and scanning QR codes, featuring specific utilities for parking management and secure, masked in-app calling.

## Features

- **Secure In-App Calling (VoIP)**: Connect with QR code owners (e.g., vehicle owners) without revealing personal phone numbers using Agora RTC.
- **Parking QR Management**: 
  - Generate specialized QR codes for your vehicle.
  - Scan parking QR codes to contact owners or navigate to locations.
- **Smart Scanning**: Fast and accurate QR code scanning.
- **Location Services**: Navigation integration for finding parked vehicles.

## Installation

1. Go to the [Releases](https://github.com/FrostyFeet-coder/QR-APP/releases) page.
2. Download the latest `app-release.apk`.
3. Install the APK on your Android device (ensure "Install from Unknown Sources" is enabled).

## Usage Demo

### 1. Generating a Parking QR & Going Online
1. Open the app and select **Generate QR**.
2. Choose **Parking QR** mode.
3. Enter your vehicle details and contact preferences.
4. Tap **Generate** to create your unique secure QR code.
5. **Important**: Tap the **Go Online** button to start receiving secure calls.
   - You must be "Online" to receive calls when someone scans your QR.
   - A notification will appear indicating you are ready to receive calls.
6. You can print this QR and place it on your vehicle.

### 2. Scanning & Calling
1. Open the app and select **Scan QR**.
2. Point your camera at a Parking QR code.
3. Once scanned, you will see the vehicle info.
4. Tap the **Call** button to initiate a secure VoIP call to the owner. 
   - *Note: Your phone number remains hidden.*

### 3. Demo Video
> [!NOTE]
> A full video demonstration of the app's features will be added here soon.

[Insert Video Demo Here]

## Technical Stack
- **Language**: Kotlin
- **UI**: XML & Jetpack Compose
- **Real-time Communication**: Agora RTC (Audio) & RTM (Signaling)
- **Scanning**: ZXing & ML Kit
