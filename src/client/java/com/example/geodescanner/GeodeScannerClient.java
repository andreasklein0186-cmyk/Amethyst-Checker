package com.example.geodescanner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GeodeScannerClient implements ClientModInitializer {
	public static final String MOD_ID = "geodescanner";

	/** Zuletzt gefundene "sehr groß gewachsene" Geoden; wird vom Renderer gelesen. */
	private static volatile List<GeodeAnalyzer.Geode> bigGeodes = List.of();

	/** Schon gemeldete Geoden (Position der Geodenmitte), damit der Chat nicht zugespammt wird. */
	private final Set<Long> announced = new HashSet<>();

	private ClientLevel lastLevel;
	private int tickCounter;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_LEVEL_TICK.register(this::onLevelTick);

		// Gizmos müssen in der Extraction-Phase des Level-Renderings angelegt werden.
		LevelExtractionEvents.END_EXTRACTION.register(context -> GeodeRenderer.draw(bigGeodes));
	}

	private void onLevelTick(ClientLevel level) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

		// Neue Welt/Dimension: alten Zustand verwerfen.
		if (level != lastLevel) {
			lastLevel = level;
			bigGeodes = List.of();
			announced.clear();
			tickCounter = 0;
		}

		if (++tickCounter % GeodeScannerConfig.SCAN_INTERVAL_TICKS != 0) return;

		List<GeodeAnalyzer.Geode> all = GeodeAnalyzer.scan(level, mc.player.blockPosition(),
				GeodeScannerConfig.CHUNK_RADIUS);

		List<GeodeAnalyzer.Geode> big = new ArrayList<>();
		for (GeodeAnalyzer.Geode geode : all) {
			if (geode.isBig()) {
				big.add(geode);
				announce(mc, geode);
			}
		}
		bigGeodes = List.copyOf(big);
	}

	private void announce(Minecraft mc, GeodeAnalyzer.Geode geode) {
		if (!GeodeScannerConfig.ANNOUNCE_IN_CHAT || mc.player == null) return;

		BlockPos c = geode.center();
		if (!announced.add(c.asLong())) return;

				mc.player.sendSystemMessage(
				Component.literal("Große Amethyst-Geode bei " + c.getX() + " " + c.getY() + " " + c.getZ()
						+ " (" + geode.clusters() + " ausgewachsene Cluster, "
						+ geode.largeBuds() + " große / " + geode.mediumBuds() + " mittlere / "
						+ geode.smallBuds() + " kleine Knospen)")
						.withStyle(ChatFormatting.LIGHT_PURPLE));
	}
}
