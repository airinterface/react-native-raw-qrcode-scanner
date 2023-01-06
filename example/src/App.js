import * as React from 'react';
import { useEffect, useState } from 'react';
import { StyleSheet, View, Text } from 'react-native';
import QRCodeScanner from 'react-native-raw-qrcode-scanner';
export default function App() {
  const [result, setResult] = useState('');
  const [isFront, setIsFront] = useState(false);
  const [enableScan, setEnableScan] = useState(true);
  const [enableFlash, setEnableFlash] = useState(false);
  const [facingText, setFacingText] = useState('back');
  const toggleCamera = () => {
    setIsFront(!isFront);
  };
  var disableTimer = null;
  const toggleScan = () => {
    setEnableScan(!enableScan);
  };
  const toggleFlash = () => {
    setEnableFlash(!enableFlash);
  };
  const onScanned = (scanned) => {
    if (disableTimer) {
      clearTimeout(disableTimer);
      disableTimer = null;
    }
    setResult(scanned.results[0]?.text || '');
    disableTimer = setTimeout(() => {
      setResult('');
    }, 1000);
  };
  useEffect(() => {
    if (isFront) {
      setFacingText('front');
    } else {
      setFacingText('back');
    }
  }, [isFront]);
  return React.createElement(
    View,
    { style: styles.container },
    React.createElement(QRCodeScanner, {
      cameraType: facingText,
      flashEnabled: enableFlash,
      scanEnabled: enableScan,
      onScanned: onScanned,
      isVibrateOnScan: true,
      style: styles.cameraContainer,
    }),
    React.createElement(
      View,
      { style: styles.bottomContainer },
      React.createElement(
        View,
        { style: styles.row },
        React.createElement(Text, null, 'Result: ', result)
      ),
      React.createElement(
        View,
        { style: styles.row },
        React.createElement(
          Text,
          { onPress: toggleCamera, style: styles.button },
          facingText
        ),
        React.createElement(
          Text,
          {
            onPress: toggleScan,
            style: [
              styles.button,
              { backgroundColor: enableScan ? enableColor : disableColor },
            ],
          },
          'Scan'
        ),
        React.createElement(
          Text,
          {
            onPress: toggleFlash,
            style: [
              styles.button,
              { backgroundColor: enableFlash ? enableColor : disableColor },
            ],
          },
          'Flash'
        )
      )
    )
  );
}
const enableColor = '#22a6b3';
const disableColor = '#333333';
const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cameraContainer: {
    position: 'absolute',
    zIndex: 1,
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
    backgroundColor: '#0000FF',
  },
  bottomContainer: {
    position: 'absolute',
    zIndex: 2,
    backgroundColor: '#DDDDDD',
    display: 'flex',
    flexDirection: 'column',
    bottom: 0,
    left: 0,
    width: '100%',
    height: 100,
  },
  row: {
    padding: 10,
    display: 'flex',
    flexDirection: 'row',
    width: '100%',
  },
  button: {
    borderRadius: 10,
    padding: 10,
    fontSize: 20,
    backgroundColor: enableColor,
    margin: 10,
    flex: 1,
  },
  box: {
    width: '100%',
    height: '100%',
  },
});
