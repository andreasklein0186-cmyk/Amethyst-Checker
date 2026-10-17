# Geode Scanner (Fabric, Minecraft 26.2)

Client-seitige Mod, die Amethystgeoden in den geladenen Chunks analysiert und Geoden mit
besonders vielen voll ausgewachsenen Amethystclustern **lila markiert** (Umrandung, auch durch
Wände sichtbar) und die Koordinaten im Chat meldet.

## Zielversionen

Stand 17.10.2026, laut Fabric-Beispielprojekt (Branch `26.2`):

| Komponente    | Version         |
|---------------|-----------------|
| Minecraft     | 26.2            |
| Java          | 25              |
| Fabric Loader | 0.19.5          |
| Fabric Loom   | 1.18-SNAPSHOT   |
| Fabric API    | 0.161.0+26.2    |

Minecraft 26.x ist nicht mehr obfuskiert, es gibt keine Mappings mehr (Mojang-Namen im Code).

## Bauen und starten

Der Gradle-Wrapper ist nicht enthalten. Kopiere den Ordner `gradle/` sowie `gradlew` und
`gradlew.bat` aus dem Branch `26.2` von https://github.com/FabricMC/fabric-example-mod in
dieses Projekt (oder führe mit installiertem Gradle `gradle wrapper` aus). Dann:

```
./gradlew runClient   # Minecraft mit der Mod starten
./gradlew build       # fertige Mod liegt in build/libs/
```

Für IntelliJ: Ordner öffnen, JDK 25 als Projekt-/Gradle-JDK einstellen.

## Wie die Analyse funktioniert

1. Alle 60 Ticks werden die geladenen Chunks im Umkreis von 8 Chunks gescannt. Chunk-Sektionen
   ohne Amethyst werden über die Block-Palette übersprungen.
2. Alle Amethystblöcke (Amethystblock, Budding Amethyst, kleine/mittlere/große Knospe,
   Amethystcluster) werden eingesammelt und per Flood-Fill zu Geoden gruppiert.
3. Pro Geode wird gezählt, wie viele Blöcke in welcher Wachstumsstufe sind.
4. Hat eine Geode mindestens 8 ausgewachsene Cluster, gilt sie als "sehr groß gewachsen":
   lila Box um die Geode, hellere Box um jeden einzelnen Cluster, Chatmeldung mit Koordinaten.

Alle Schwellwerte stehen in `GeodeScannerConfig.java`.

## Projektstruktur

```
src/main/resources/fabric.mod.json
src/client/java/com/example/geodescanner/
  GeodeScannerClient.java   Einstiegspunkt, Scan-Takt, Chatmeldung
  GeodeAnalyzer.java        Scannen, Gruppieren, Zählen (reine Logik)
  GeodeRenderer.java        lila Markierung (einziger Teil mit Render-API)
  GeodeScannerConfig.java   Schwellwerte und Farben
```

## Bekannte Grenzen

- Der Code wurde noch nicht kompiliert oder im Spiel getestet. Das Render-API von Minecraft
  26.x ist neu; falls `GeodeRenderer` nicht kompiliert, ist das die Stelle zum Anpassen.
- Es werden nur Chunks gescannt, die der Client geladen hat (Render-/Simulationsdistanz).
  Geoden am Rand können nur teilweise geladen sein und niedrigere Zahlen liefern.
- Eine Markierung durch Wände ist ein X-Ray-artiger Vorteil. Auf Multiplayer-Servern ist das oft
  verboten, nutze die Mod im Einzelspieler oder frage den Server-Betreiber.

## Ideen für die nächsten Schritte

- Keybind zum Ein-/Ausschalten und `/geodes`-Befehl mit Liste aller gefundenen Geoden
- Config-Datei (JSON) statt Konstanten
- Farbverlauf nach Größe (z. B. kleine Geoden grau, mittlere blau, große lila)
- Bewertungs-Score statt fester Schwelle (Cluster-Anteil an allen Knospen)
