import React from 'react';
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
  style?: any;
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

  if( Platform.OS === 'ios' ) {
    const eventEmitter = new NativeEventEmitter(
      NativeModules.RNRawQrCodeScannerEventEmitter
    );
    eventEmitter.addListener('onScanned', onChange);

  } else if ( Platform.OS === 'android' ) {
    DeviceEventEmitter.addListener('onScanned', onChange);
  } 

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
  onScanned: (event:any)=>void;
  style: any;
};

const ComponentName = 'RNRawQrcodeScanner';


const RNRawQrcodeScanner = requireNativeComponent<RawQrcodeScannerProps>(ComponentName)

export type QRCodeScannerProps = RawQrcodeScannerProps;
export default QRCodeScanner;
