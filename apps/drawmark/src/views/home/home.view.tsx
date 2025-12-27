import { ActivityIndicator } from 'react-native';
import { HomeUI } from './home.ui';
import { useInkCanvasPersistence } from '../../hooks/useInkCanvasPersistence';
import { useToolbarStatePersistence } from '../../hooks/useToolbarStatePersistence';

export function HomeView() {
  const { canvasRef, strokes, textFields, isLoading: isCanvasLoading, handleStrokesChange, handleTextFieldsChange } =
    useInkCanvasPersistence('main-canvas');

  const {
    toolbarState,
    activeBrushInfo,
    isLoading: isToolbarLoading,
    setActiveFamily,
    setEditingMode,
    setBrushSettings,
  } = useToolbarStatePersistence('main-canvas');

  if (isCanvasLoading || isToolbarLoading || !activeBrushInfo) {
    return <ActivityIndicator />;
  }

  return (
    <HomeUI
      canvasRef={canvasRef}
      strokes={strokes}
      textFields={textFields}
      handleStrokesChange={handleStrokesChange}
      handleTextFieldsChange={handleTextFieldsChange}
      toolbarState={toolbarState}
      activeBrushInfo={activeBrushInfo}
      setActiveFamily={setActiveFamily}
      setEditingMode={setEditingMode}
      setBrushSettings={setBrushSettings}
    />
  );
}
