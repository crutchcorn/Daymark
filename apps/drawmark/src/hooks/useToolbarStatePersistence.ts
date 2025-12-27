import { useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { db } from '../constants/db';
import { getToolbarState, saveToolbarState } from '../services/toolbarState';
import {
  DEFAULT_TOOLBAR_STATE,
  type ToolbarState,
  type BrushSettings,
} from '../constants/toolbar';
import type { InkEditorBrushFamily, InkEditorMode } from '../components/InkEditor';

const QUERY_KEY_PREFIX = 'toolbarState';

export function useToolbarStatePersistence(canvasId: string) {
  const queryClient = useQueryClient();

  // Query to load initial state
  const {
    data: toolbarState,
    isLoading,
    error,
  } = useQuery({
    queryKey: [QUERY_KEY_PREFIX, canvasId],
    queryFn: async () => {
      return getToolbarState(db, canvasId);
    },
    staleTime: Infinity,
  });

  // Mutation to save toolbar state
  const saveStateMutation = useMutation({
    mutationFn: async (state: ToolbarState) => {
      await saveToolbarState(db, canvasId, state);
    },
    onSuccess: (_data, state) => {
      queryClient.setQueryData([QUERY_KEY_PREFIX, canvasId], state);
    },
  });

  const persistState = useCallback(
    (newState: ToolbarState) => {
      // Update cache immediately for responsive UI
      queryClient.setQueryData([QUERY_KEY_PREFIX, canvasId], newState);
      // Save immediately
      saveStateMutation.mutate(newState);
    },
    [canvasId, queryClient, saveStateMutation],
  );

  // Helper to update active brush family
  const setActiveFamily = useCallback(
    (family: InkEditorBrushFamily) => {
      if (!toolbarState) return;
      persistState({ ...toolbarState, activeFamily: family });
    },
    [toolbarState, persistState],
  );

  // Helper to update editing mode
  const setEditingMode = useCallback(
    (mode: InkEditorMode) => {
      if (!toolbarState) return;
      persistState({ ...toolbarState, editingMode: mode });
    },
    [toolbarState, persistState],
  );

  // Helper to update settings for a specific brush family
  const setBrushSettings = useCallback(
    (family: InkEditorBrushFamily, settings: Partial<BrushSettings>) => {
      if (!toolbarState) return;
      persistState({
        ...toolbarState,
        brushes: {
          ...toolbarState.brushes,
          [family]: { ...toolbarState.brushes[family], ...settings },
        },
      });
    },
    [toolbarState, persistState],
  );

  // Helper to update color for the currently active brush
  const setActiveBrushColor = useCallback(
    (color: string) => {
      if (!toolbarState) return;
      setBrushSettings(toolbarState.activeFamily, { color });
    },
    [toolbarState, setBrushSettings],
  );

  // Get current active brush info (for the InkEditor component)
  const activeBrushInfo = toolbarState
    ? {
        family: toolbarState.activeFamily,
        color: toolbarState.brushes[toolbarState.activeFamily].color,
        size: toolbarState.brushes[toolbarState.activeFamily].size,
      }
    : null;

  return {
    toolbarState: toolbarState ?? DEFAULT_TOOLBAR_STATE,
    activeBrushInfo,
    isLoading,
    error,
    isSaving: saveStateMutation.isPending,
    setActiveFamily,
    setEditingMode,
    setBrushSettings,
    setActiveBrushColor,
  };
}
