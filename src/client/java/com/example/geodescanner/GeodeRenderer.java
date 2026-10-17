package com.example.geodescanner;

import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Zeichnet die lila Markierungen. Das ist die einzige Klasse, die das (in Minecraft 26.x
 * stark umgebaute) Render-API benutzt. Falls sich dort etwas ändert, muss nur diese Datei
 * angepasst werden.
 *
 * Es werden Gizmos verwendet, das sind die Linien-/Box-Overlays, die auch die Debug-Anzeigen
 * von Minecraft nutzen. "AlwaysOnTop" sorgt dafür, dass man die Markierung auch durch Wände sieht.
 */
public final class GeodeRenderer {
	private GeodeRenderer() {}

	/** Muss während der Level-Extraction aufgerufen werden (siehe GeodeScannerClient). */
	public static void draw(List<GeodeAnalyzer.Geode> bigGeodes) {
		for (GeodeAnalyzer.Geode geode : bigGeodes) {
			// Große lila Box um die ganze Geode.
			Gizmos.cuboid(geode.bounds().inflate(0.5), GizmoStyle.stroke(GeodeScannerConfig.COLOR_GEODE))
					.setAlwaysOnTop();

			if (GeodeScannerConfig.OUTLINE_EACH_CLUSTER) {
				for (BlockPos pos : geode.clusterPositions()) {
					Gizmos.cuboid(new AABB(pos), GizmoStyle.stroke(GeodeScannerConfig.COLOR_CLUSTER))
							.setAlwaysOnTop();
				}
			}
		}
	}
}
