import React, { forwardRef } from 'react';
import {
  requireNativeComponent,
  Platform,
  StyleSheet,
  ViewStyle,
  StyleProp,
} from 'react-native';

const COMPONENT_NAME = 'InkCanvasView';

interface InkCanvasNativeProps {
  style?: StyleProp<ViewStyle>;
  strokes?: string;
  textFields?: string;
}

export interface InkCanvasRef {
  // Currently no imperative methods needed for read-only canvas
}

export interface InkCanvasProps {
  style?: StyleProp<ViewStyle>;
  /**
   * The strokes to display on the canvas.
   * This should be a JSON string.
   */
  strokes?: string;
  /**
   * The text fields to display on the canvas.
   * This should be a JSON string.
   */
  textFields?: string;
}

const NativeInkCanvas =
  Platform.OS === 'android'
    ? requireNativeComponent<InkCanvasNativeProps>(COMPONENT_NAME)
    : null;

/**
 * InkCanvas is a React Native component that displays ink strokes (read-only).
 * For editing capabilities, use InkEditor instead.
 *
 * Note: This component is only available on Android.
 */
export const InkCanvas = forwardRef<InkCanvasRef, InkCanvasProps>(
  ({ style, strokes, textFields }, ref) => {
    if (Platform.OS !== 'android' || !NativeInkCanvas) {
      // Return null or a placeholder for non-Android platforms
      return null;
    }

    return (
      <NativeInkCanvas
        style={[styles.container, style]}
        strokes={strokes}
        textFields={textFields}
      />
    );
  },
);

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
