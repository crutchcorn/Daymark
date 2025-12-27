import { eq } from 'drizzle-orm';
import * as v from 'valibot';
import { DB } from '../constants/db';
import { toolbarStateTable } from '../db/schema';
import {
  ToolbarStateSchema,
  DEFAULT_TOOLBAR_STATE,
  type ToolbarState,
} from '../constants/toolbar';

export interface ToolbarStateRecord {
  id: number;
  canvasId: string;
  stateJson: string;
  updatedAt: Date;
}

/**
 * Retrieves the toolbar state for a given canvas ID.
 * Uses valibot to safely parse and provide defaults for missing/invalid fields.
 */
export async function getToolbarState(
  db: DB,
  canvasId: string,
): Promise<ToolbarState> {
  const results = await db
    .select()
    .from(toolbarStateTable)
    .where(eq(toolbarStateTable.canvasId, canvasId))
    .limit(1);

  if (results.length === 0) {
    return DEFAULT_TOOLBAR_STATE;
  }

  try {
    const parsed = JSON.parse(results[0].stateJson);
    const result = v.safeParse(ToolbarStateSchema, parsed);
    if (result.success) {
      return result.output;
    }
    return DEFAULT_TOOLBAR_STATE;
  } catch {
    return DEFAULT_TOOLBAR_STATE;
  }
}

/**
 * Saves the toolbar state for a given canvas ID.
 * Creates a new record if it doesn't exist, or updates the existing one.
 */
export async function saveToolbarState(
  db: DB,
  canvasId: string,
  state: ToolbarState,
): Promise<void> {
  const stateJson = JSON.stringify(state);
  const existing = await db
    .select()
    .from(toolbarStateTable)
    .where(eq(toolbarStateTable.canvasId, canvasId))
    .limit(1);

  if (existing.length > 0) {
    await db
      .update(toolbarStateTable)
      .set({
        stateJson,
        updatedAt: new Date(),
      })
      .where(eq(toolbarStateTable.canvasId, canvasId));
  } else {
    await db.insert(toolbarStateTable).values({
      canvasId,
      stateJson,
      updatedAt: new Date(),
    });
  }
}
