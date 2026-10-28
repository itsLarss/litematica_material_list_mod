package de.cozyverse.schematictracker.neoforge;

import de.cozyverse.schematictracker.SchematicTrackerClient;
import de.cozyverse.schematictracker.TrackerCommands;
import de.cozyverse.schematictracker.TrackerHud;
import de.cozyverse.schematictracker.compat.Compat;
import de.cozyverse.schematictracker.compat.Gfx;
import de.cozyverse.schematictracker.screen.TrackerConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge-Einstieg: meldet Tasten, HUD, Befehle, Tick und den Einstellungs-Bildschirm (Mod-Liste) beim
 * gemeinsamen Kern an. NeoForge erlaubt in Mod-IDs keinen Bindestrich, daher "schematic_tracker".
 */
@Mod(value = NeoForgeClient.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClient {
	public static final String MOD_ID = "schematic_tracker";

	public NeoForgeClient(IEventBus modBus, ModContainer container) {
		SchematicTrackerClient.init(FMLPaths.CONFIGDIR.get(), container.getModInfo().getVersion().toString());
		container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new TrackerConfigScreen(parent));

		modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
			//? if >=1.21.9 {
			event.registerCategory(Compat.keyCategory());
			//?}
			for (KeyMapping key : SchematicTrackerClient.keys()) event.register(key);
		});
		modBus.addListener(RegisterGuiLayersEvent.class, event ->
				event.registerAboveAll(Compat.id("list"), (g, delta) -> TrackerHud.render(new Gfx(g))));

		NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event ->
				TrackerCommands.register(event.getDispatcher(), NeoForgeClient::feedback));
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> SchematicTrackerClient.tick(Minecraft.getInstance()));
	}

	private static TrackerCommands.Feedback feedback(CommandSourceStack source) {
		return new TrackerCommands.Feedback() {
			@Override
			public void ok(Component message) {
				source.sendSuccess(() -> message, false);
			}

			@Override
			public void error(Component message) {
				source.sendFailure(message);
			}
		};
	}
}
