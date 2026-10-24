package de.cozyverse.schematictracker.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.cozyverse.schematictracker.screen.TrackerConfigScreen;

/** Einstellungen über Mod Menu (optional; ohne Mod Menu wird diese Klasse nie geladen). */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return TrackerConfigScreen::new;
	}
}
