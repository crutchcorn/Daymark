CREATE TABLE `toolbar_state` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`canvas_id` text NOT NULL,
	`state_json` text DEFAULT '{}' NOT NULL,
	`updated_at` integer NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX `toolbar_state_canvas_id_unique` ON `toolbar_state` (`canvas_id`);