# react-native-raw-qrcode-scanner

Simple QRCode Reader that decodes to raw format. 
No processing. Just using own platform's best library or native capability for QR Code. 

## Installation

```sh
npm install github:airinterface/react-native-raw-qrcode-scanner#v0.0.1
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
import QRCodeScanner, { OnScannedEvent } from 'react-native-raw-qrcode-scanner';
  const onScanned = (scanned: OnScannedEvent) => {
    if (disableTimer) {
      clearTimeout(disableTimer);
      disableTimer = null;
    }
    setResult(scanned.results[0]?.text || '');
    disableTimer = setTimeout(() => {
      setResult('');
    }, 1000);
  };


  <QRCodeScanner
    cameraType={"back"}
    flashEnabled={true}
    scanEnabled={true}
    onScanned={onScanned}
    isVibrateOnScan={true}
    style={{background:"#FF0000"}}
    samplingRateInMS={300}
  />

```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
