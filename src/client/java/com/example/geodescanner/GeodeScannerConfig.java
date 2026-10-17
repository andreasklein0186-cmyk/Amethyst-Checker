package com.example.geodescanner;

/**
 * Alle einstellbaren Werte an einer Stelle. Später lässt sich das leicht durch eine
 * JSON-Config oder ein Cloth-Config-Menü ersetzen.
 */
public final class GeodeScannerConfig {
	private GeodeScannerConfig() {}

	/** Wie viele Chunks um den Spieler gescannt werden (nur bereits geladene Chunks). */
	public static final int CHUNK_RADIUS = 8;

	/** Alle wie viele Ticks neu gescannt wird (20 Ticks = 1 Sekunde). */
	public static final int SCAN_INTERVAL_TICKS = 60;

	/**
	 * Zwei Amethyst-Blöcke gehören zur selben Geode, wenn sie höchstens so viele Blöcke
	 * voneinander entfernt sind (in jeder Achse). Die Calcit-/Basalt-Schichten dazwischen sind
	 * 1-2 Blöcke dick, die Amethystschicht liegt aber meist zusammenhängend.
	 */
	public static final int LINK_DISTANCE = 2;

	/** Mindestanzahl an Amethyst-/Budding-Blöcken, damit etwas überhaupt als Geode zählt. */
	public static final int MIN_GEODE_CORE_BLOCKS = 15;

	/**
	 * Ab dieser Anzahl voll ausgewachsener Amethystcluster (AMETHYST_CLUSTER) gilt eine Geode
	 * als "sehr groß gewachsen" und wird lila markiert.
	 */
	public static final int BIG_CLUSTER_THRESHOLD = 8;

	/** Lila Farbe (ARGB) für die Umrandung der ganzen Geode. */
	public static final int COLOR_GEODE = 0xFFB026FF;

	/** Hellere Farbe (ARGB) für jeden einzelnen ausgewachsenen Cluster. */
	public static final int COLOR_CLUSTER = 0xFFE0A8FF;

	/** Ob jeder einzelne ausgewachsene Cluster zusätzlich umrandet wird. */
	public static final boolean OUTLINE_EACH_CLUSTER = true;

	/** Ob beim ersten Fund einer großen Geode eine Chatnachricht mit Koordinaten erscheint. */
	public static final boolean ANNOUNCE_IN_CHAT = true;
}
