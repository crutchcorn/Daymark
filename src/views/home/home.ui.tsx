import { useRef } from 'react';
import {InkCanvas, InkCanvasRef} from '../../components/InkCanvas';
import { Button, View } from 'react-native';

export function HomeUI() {
  const canvasRef = useRef<InkCanvasRef>(null);

  const handleClear = () => {
    canvasRef.current?.clear();
  };

  return (
    <View style={{flex: 1}}>
      <InkCanvas
        ref={canvasRef}
        brushColor="#0000FF"
        brushSize={8}
        brushFamily="pen"
        style={{flex: 1}}
      />
      <Button title="Clear" onPress={handleClear} />
    </View>
  );
}