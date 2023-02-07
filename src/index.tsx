import React, { PropTypes } from 'react';

import {
  requireNativeComponent,
  View,
  UIManager,
  Platform,
  NativeEventEmitter,
  DeviceEventEmitter,
  NativeModules,
  Vibration,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-raw-qrcode-scanner' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

export type BarcodeItemType = {
  text: string | null;
  url: string | null;
  type: string | null;
  rawBytes: number[];
};

export interface OnScannedEvent {
  results: BarcodeItemType[];
}

type QrcodeScannerProps = {
  flashEnabled?: boolean;
  scanEnabled?: boolean;
  cameraType?: string;
  onScanned?: (barcodes: OnScannedEvent) => void;
  style?: PropTypes.any;
  isVibrateOnScan?: boolean;
};

var readCount = 0;
const QRCodeScanner = (props: QrcodeScannerProps) => {
  const {
    cameraType,
    scanEnabled,
    flashEnabled,
    onScanned,
    style,
    isVibrateOnScan,
  } = props;
  let tmpScanEnabled = true;
  let vibrateTimer: any = null;
  let isVibrate = false;
  if (typeof isVibrateOnScan === 'boolean') {
    isVibrate = isVibrateOnScan;
  }

  if (typeof scanEnabled === 'boolean') {
    tmpScanEnabled = scanEnabled;
  }

  if (typeof scanEnabled === 'boolean') {
    tmpScanEnabled = scanEnabled;
  }
  const onChange = (event: any) => {
    if (!onScanned) {
      return;
    }
    readCount++;
    if (!vibrateTimer && isVibrate && readCount === 1) {
      vibrateTimer = true;
      Vibration.vibrate();
      vibrateTimer = setTimeout(() => {
        clearTimeout(vibrateTimer);
        readCount = 0;
        vibrateTimer = null;
      }, 1000);
    }
    onScanned({
      results: event.results || [],
    });
  };
  const eventEmitter = new NativeEventEmitter(
    NativeModules.RNRawQrCodeScannerEventEmitter
  );
  eventEmitter.addListener('onScanned', onChange);

  DeviceEventEmitter.addListener('onScanned', onChange);
  const defaultStyle = {
    positon: 'absolute',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
  };
  return (
    <View style={[style, defaultStyle]}>
      <RNRawQrcodeScanner
        style={style}
        cameraType={cameraType || 'back'}
        onScanned={onChange}
        flashEnabled={flashEnabled || false}
        scanEnabled={tmpScanEnabled}
      />
    </View>
  );
};

type RawQrcodeScannerProps = {
  flashEnabled?: boolean;
  scanEnabled?: boolean;
  cameraType?: string;
  onScanned: PropTypes.func;
};

const ComponentName = 'RNRawQrcodeScanner';

const RNRawQrcodeScanner =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<RawQrcodeScannerProps>(ComponentName)
    : () => {
        console.log(LINKING_ERROR);
      };

export type QRCodeScannerProps = RawQrcodeScannerProps;
export default QRCodeScanner;
