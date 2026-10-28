package de.cozyverse.schematictracker.fabric;

import de.cozyverse.schematictracker.SchematicTrackerClient;
import de.cozyverse.schematictracker.TrackerCommands;
import de.cozyverse.schematictracker.TrackerHud;
import de.cozyverse.schematictracker.compat.Gfx;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?}
//? if >=1.21.6 {
import de.cozyverse.schematictracker.compat.Compat;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
*///?}

/** Fabric-Einstieg: meldet Tasten, HUD, Befehle und den Tick beim gemeinsamen Kern an. */
public final class FabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		String version = FabricLoader.getInstance().getModContainer(SchematicTrackerClient.MOD_ID)
				.map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("dev");
		SchematicTrackerClient.init(FabricLoader.getInstance().getConfigDir(), version);

		for (KeyMapping key : SchematicTrackerClient.keys()) {
			//? if >=26.1 {
			KeyMappingHelper.registerKeyMapping(key);
			//?} else {
			/*KeyBindingHelper.registerKeyBinding(key);
			*///?}
		}

		//? if >=1.21.6 {
		HudElementRegistry.addLast(Compat.id("list"), (g, delta) -> TrackerHud.render(new Gfx(g)));
		//?} else {
		/*HudRenderCallback.EVENT.register((g, delta) -> TrackerHud.render(new Gfx(g)));
		*///?}

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
				TrackerCommands.register(dispatcher, FabricClient::feedback));
		ClientTickEvents.END_CLIENT_TICK.register(SchematicTrackerClient::tick);
	}

	private static TrackerCommands.Feedback feedback(FabricClientCommandSource source) {
		return new TrackerCommands.Feedback() {
			@Override
			public void ok(Component message) {
				source.sendFeedback(message);
			}

			@Override
			public void error(Component message) {
				source.sendError(message);
			}
		};
	}
}
