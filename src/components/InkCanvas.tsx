import React, {forwardRef, useImperativeHandle, useRef} from 'react';
import {
  requireNativeComponent,
  UIManager,
  Platform,
  findNodeHandle,
  StyleSheet,
  ViewStyle,
  StyleProp,
} from 'react-native';

const COMPONENT_NAME = 'InkCanvasView';

interface InkCanvasNativeProps {
  style?: StyleProp<ViewStyle>;
  brushColor?: string;
  brushSize?: number;
  brushFamily?: 'pen' | 'marker' | 'highlighter';
}

export interface InkCanvasRef {
  clear: () => void;
}

export interface InkCanvasProps {
  style?: StyleProp<ViewStyle>;
  /**
   * The color of the brush stroke in hex format (e.g., "#000000").
   * @default "#000000"
   */
  brushColor?: string;
  /**
   * The size of the brush stroke in pixels.
   * @default 5
   */
  brushSize?: number;
  /**
   * The type of brush family to use.
   * - "pen": Pressure-sensitive pen (default)
   * - "marker": Solid marker
   * - "highlighter": Semi-transparent highlighter
   * @default "pen"
   */
  brushFamily?: 'pen' | 'marker' | 'highlighter';
}

const NativeInkCanvas =
  Platform.OS === 'android'
    ? requireNativeComponent<InkCanvasNativeProps>(COMPONENT_NAME)
    : null;

/**
 * InkCanvas is a React Native component that wraps the Android Ink API
 * for low-latency stylus/touch drawing.
 *
 * Note: This component is only available on Android.
 */
export const InkCanvas = forwardRef<InkCanvasRef, InkCanvasProps>(
  ({style, brushColor = '#000000', brushSize = 5, brushFamily = 'pen'}, ref) => {
    const nativeRef = useRef(null);

    useImperativeHandle(ref, () => ({
      clear: () => {
        if (Platform.OS === 'android' && nativeRef.current) {
          const commands = UIManager.getViewManagerConfig(COMPONENT_NAME)?.Commands;
          if (commands?.clear !== undefined) {
            UIManager.dispatchViewManagerCommand(
              findNodeHandle(nativeRef.current),
              commands.clear,
              [],
            );
          }
        }
      },
    }));

    if (Platform.OS !== 'android' || !NativeInkCanvas) {
      // Return null or a placeholder for non-Android platforms
      return null;
    }

    return (
      <NativeInkCanvas
        ref={nativeRef}
        style={[styles.container, style]}
        brushColor={brushColor}
        brushSize={brushSize}
        brushFamily={brushFamily}
      />
    );
  },
);

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
