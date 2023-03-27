import * as React from 'react';
import { useEffect, useState } from 'react';
import { StyleSheet, View, Text } from 'react-native';
import QRCodeScanner, { OnScannedEvent } from 'react-native-raw-qrcode-scanner';

export default function App() {
  const [result, setResult] = useState('');
  const [isFront, setIsFront] = useState(false);
  const [enableScan, setEnableScan] = useState(true);
  const [enableFlash, setEnableFlash] = useState(false);
  const [facingText, setFacingText] = useState('back');

  const toggleCamera = () => {
    setIsFront(!isFront);
  };
  var disableTimer: any = null;
  const toggleScan = () => {
    setEnableScan(!enableScan);
  };

  const toggleFlash = () => {
    setEnableFlash(!enableFlash);
  };
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

  useEffect(() => {
    if (isFront) {
      setFacingText('front');
    } else {
      setFacingText('back');
    }
  }, [isFront]);

  return (
    <View style={styles.container}>
      <QRCodeScanner
        cameraType={facingText}
        flashEnabled={enableFlash}
        scanEnabled={enableScan}
        onScanned={onScanned}
        isVibrateOnScan={true}
        samplingRateInMS={350}
        style={styles.cameraContainer}
      />
      <View style={styles.bottomContainer}>
        <View style={styles.row}>
          <Text>Result: {result}</Text>
        </View>
        <View style={styles.row}>
          <Text onPress={toggleCamera} style={styles.button}>
            {facingText}
          </Text>
          <Text
            onPress={toggleScan}
            style={[
              styles.button,
              { backgroundColor: enableScan ? enableColor : disableColor },
            ]}
          >
            Scan
          </Text>
          <Text
            onPress={toggleFlash}
            style={[
              styles.button,
              { backgroundColor: enableFlash ? enableColor : disableColor },
            ]}
          >
            Flash
          </Text>
        </View>
      </View>
    </View>
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
    backgroundColor: '#FF0000',
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
