# react-native-raw-qrcode-scanner

Simple QRCode Reader that decodes to raw foramt

## Installation

```sh
npm install react-native-raw-qrcode-scanner
```
## PERMISSIONS Requirement

 To have proper permission,

### IOS
 you need to add the "Privacy - Camera Usage Description" key to the info.plist of your project. This should be found in 'your_project/ios/your_project/Info.plist'. Add the following code:

 ```
<key>NSCameraUsageDescription</key>
<string>Your message to user when the camera is accessed for the first time</string>

```
### Android
 you need to add the "Vibration" permission to your AndroidManifest.xml of your project. This should be found in your android/app/src/main/AndroidManifest.xml Add the following:

```
    <uses-permission android:name="android.permission.CAMERA" />
    
```
  if you set isVibrateOnScan to true, make sure you have 

```
  <uses-permission android:name="android.permission.VIBRATE"/>

```
### IOS

## Usage

```js
import { QrcodeScannerView } from "react-native-raw-qrcode-scanner";

// ...

<QrcodeScannerView onScanned={onScanCalleback} cameraType={"back"} />
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
