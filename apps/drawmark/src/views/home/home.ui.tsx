import { RefObject } from 'react';
import { Button, Pressable, Text, View } from 'react-native';
import {
  InkEditor,
  InkEditorBrushInfo,
  InkEditorRef,
} from '../../components/InkEditor';
import { InkCanvas } from '../../components/InkCanvas';
import { PenIcon } from '../../components/Icons/pen';
import { MarkerIcon } from '../../components/Icons/marker';
import { HighlighterIcon } from '../../components/Icons/highlighter';

interface HomeUIProps {
  canvasRef: RefObject<InkEditorRef | null>;
  initialStrokes: string | undefined;
  handleStrokesChange: (strokesJson: string) => void;
  isEditing: boolean;
  setIsEditing: (val: boolean) => void;
  brushInfo: InkEditorBrushInfo;
  setBrushInfo: (info: InkEditorBrushInfo) => void;
}

export function HomeUI({
  canvasRef,
  initialStrokes,
  handleStrokesChange,
  isEditing,
  setIsEditing,
  brushInfo,
  setBrushInfo,
}: HomeUIProps) {
  const handleClear = () => {
    canvasRef.current?.clear();
  };

  const handleEditToggle = () => {
    setIsEditing(!isEditing);
  };

  return (
    <View style={{ flex: 1 }}>
      {isEditing ? (
        <InkEditor
          initialStrokes={initialStrokes}
          onStrokesChange={handleStrokesChange}
          ref={canvasRef}
          brushColor={brushInfo.color}
          brushSize={brushInfo.size}
          brushFamily={brushInfo.family}
          style={{ flex: 1 }}
        />
      ) : (
        <InkCanvas initialStrokes={initialStrokes} style={{ flex: 1 }} />
      )}
      <Button title="Clear" onPress={handleClear} />
      <View style={{ flexWrap: 'nowrap' }}>
        <Pressable
          onPress={() => setBrushInfo({ ...brushInfo, family: 'pen' })}
          style={{height: 30, width: 30}}
        >
          <PenIcon activeColor={brushInfo.color} />
          {brushInfo.family === 'pen' ? <Text>!</Text> : null}
        </Pressable>
        <Pressable
          onPress={() => setBrushInfo({ ...brushInfo, family: 'marker' })}
          style={{height: 30, width: 30}}
        >
          <MarkerIcon activeColor={brushInfo.color} />
          {brushInfo.family === 'marker' ? <Text>!</Text> : null}
        </Pressable>
        <Pressable
          onPress={() => setBrushInfo({ ...brushInfo, family: 'highlighter' })}
          style={{height: 30, width: 30}}
        >
          <HighlighterIcon activeColor={brushInfo.color} />
          {brushInfo.family === 'highlighter' ? <Text>!</Text> : null}
        </Pressable>
      </View>
      <View style={{ flexWrap: 'nowrap' }}>
        <Button
          title="Red"
          onPress={() => setBrushInfo({ ...brushInfo, color: '#FF0000' })}
        />
        <Button
          title="Blue"
          onPress={() => setBrushInfo({ ...brushInfo, color: '#0000FF' })}
        />
      </View>
      <Button
        title={isEditing ? 'Stop Editing' : 'Edit'}
        onPress={handleEditToggle}
      />
    </View>
  );
}
