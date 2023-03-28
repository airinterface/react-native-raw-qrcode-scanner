import React, { useEffect, useRef } from 'react';
import {
  requireNativeComponent,
  View,
  Platform,
  NativeEventEmitter,
  DeviceEventEmitter,
  NativeModules,
  Vibration,
} from 'react-native';

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
  samplingRateMS?:number
  style?: any;
  isVibrateOnScan?: boolean;
};

var readCount = 0;
let defaultSampleRateMS = 300;
const QRCodeScanner = (props: QrcodeScannerProps) => {
  const {
    cameraType,
    scanEnabled,
    flashEnabled,
    onScanned,
    style,
    isVibrateOnScan,
    samplingRateInMS,
  } = props;
  let samplingRateMillSec = samplingRateInMS || defaultSampleRateMS;
  let tmpScanEnabled = true;
  let vibrateTimer: any = null;
  let isVibrate = false;
  const subscription = useRef(null);
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
    'worklet'
    if (!onScanned) {
      return;
    }
    readCount++;
    if( readCount === 1 ) {
      if (!vibrateTimer && isVibrate ) {
        Vibration.vibrate();
      }
      vibrateTimer = true;
      vibrateTimer = setTimeout(() => {
        clearTimeout(vibrateTimer);
        readCount = 0;
        vibrateTimer = null;
      }, samplingRateMillSec);
      onScanned({
        results: event.results || [],
      });
    }
  };
  /* eslint react-hooks/exhaustive-deps: off */
  useEffect(() => {
    if (subscription.current) {
      subscription.current.remove();
      subscription.current = null;
    }
    if (Platform.OS === 'ios') {
      const eventEmitter = new NativeEventEmitter(
        NativeModules.RNRawQrCodeScannerEventEmitter
      );
      subscription.current = eventEmitter.addListener('onScanned', onChange);
    } else if (Platform.OS === 'android') {
      subscription.current = DeviceEventEmitter.addListener(
        'onScanned',
        onChange
      );
    }
    return () => {
      if (subscription.current) {
        subscription.current.remove();
      }
    };
  }, []);

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
        samplingRateInMS={samplingRateMillSec}
      />
    </View>
  );
};

type RawQrcodeScannerProps = {
  flashEnabled?: boolean;
  scanEnabled?: boolean;
  cameraType?: string;
  onScanned: (event: any) => void;
  style: any;
};

const ComponentName = 'RNRawQrcodeScanner';

const RNRawQrcodeScanner =
  requireNativeComponent<RawQrcodeScannerProps>(ComponentName);

export type QRCodeScannerProps = RawQrcodeScannerProps;
export default QRCodeScanner;
