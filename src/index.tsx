import {
  requireNativeComponent,
  UIManager,
  Platform,
  ViewStyle,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-raw-qrcode-scanner' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

type RawQrcodeScannerProps = {
  color: string;
  style: ViewStyle;
};

const ComponentName = 'RawQrcodeScannerView';

export const RawQrcodeScannerView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<RawQrcodeScannerProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };
