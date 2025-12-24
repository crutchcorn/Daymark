import { InkEditor, InkEditorRef } from '../../components/InkEditor';
import { Button, View } from 'react-native';
import { InkCanvas } from '../../components/InkCanvas';
import { useState } from 'react';

interface HomeUIProps {
  canvasRef: React.RefObject<InkEditorRef | null>;
  initialStrokes: string | undefined;
  handleStrokesChange: (strokesJson: string) => void;
  isEditing: boolean;
  setIsEditing: (val: boolean) => void;
}

export function HomeUI({
  canvasRef,
  initialStrokes,
  handleStrokesChange,
  isEditing,
  setIsEditing,
}: HomeUIProps) {
  const handleClear = () => {
    canvasRef.current?.clear();
  };

  const handleEditToggle = () => {
    setIsEditing(!isEditing);
  };

  const [brushColor, setBrushColor] = useState('#0000FF');

  return (
    <View style={{ flex: 1 }}>
      {isEditing ? <InkEditor
        initialStrokes={initialStrokes}
        onStrokesChange={handleStrokesChange}
        ref={canvasRef}
        brushColor={brushColor}
        brushSize={8}
        brushFamily="pen"
        style={{ flex: 1 }}
      /> : <InkCanvas initialStrokes={initialStrokes} style={{ flex: 1 }} />}
      <Button title="Clear" onPress={handleClear} />
      <Button title="Red" onPress={() => setBrushColor('#FF0000')} />
      <Button title="Blue" onPress={() => setBrushColor('#0000FF')} />
      <Button title={isEditing ? "Stop Editing" : "Edit"} onPress={handleEditToggle} />
    </View>
  );
}
