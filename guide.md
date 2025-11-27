# HumanityTree nutzen

## Voraussetzungen
- JDK 17 (wird über den Gradle-Wrapper genutzt)
- Gradle Wrapper vorhanden (`./gradlew`)
- Kotlin-Multiplatform-Projekt; zentrale API: `src/commonMain/kotlin/at/crowdware/humanitytree/core/HumanityTree.kt`

## Projekt bauen und testen
```bash
./gradlew test   # führt die JVM-Tests aus
```
Beispieltests: `src/commonTest/kotlin/…/HumanityTreeTest.kt`, `src/jvmTest/kotlin/…/FileStoreTest.kt`.

## Zentrale Konzepte
- **Tag**: Suchschlüssel, z. B. `#Guitarist`, nach dem gespeichert und gesucht wird.
- **profileCid**: Verweis auf ein Profil/Objekt (z. B. IPFS-CID).
- **Store**: Ablage der Baumknoten. `InMemoryStore` (flüchtig) und `FileStore` (persistente Dateien unter `nodes/` und `ipns/`).
- **Root/IPNS**: Aktuelles Root-CID wird unter einem Namen (Standard `humanitytree-root`) abgelegt; darüber findet jede Suche den Einstieg.

## Schnellstart (InMemory, flüchtig)
```kotlin
import at.crowdware.humanitytree.core.HumanityTree
import at.crowdware.humanitytree.core.InMemoryStore

val store = InMemoryStore()
val tree = HumanityTree(store, defaultLeafCapacity = 128)

// Einträge hinzufügen
tree.insert("#Guitarist", "cid1")
tree.insert("#Guitarist", "cid2")

// Nach Profilen zu einem Tag suchen
val profiles = tree.lookup("#Guitarist")  // -> ["cid1", "cid2"]
```

## Persistente Nutzung (Dateisystem)
```kotlin
import at.crowdware.humanitytree.core.FileStore
import at.crowdware.humanitytree.core.HumanityTree
import java.nio.file.Paths

val dataDir = Paths.get("data/humanitytree")
val store = FileStore(dataDir)
val tree = HumanityTree(store)  // ipnsName default: "humanitytree-root"

// Daten schreiben
tree.insert("#VeganFood", "bafybeigd...")

// Später (auch in neuer App-Instanz) lesen
val results = tree.lookup("#VeganFood")
```
Der `FileStore` speichert pro CID eine `.sml`-Datei unter `nodes/` und legt die aktuelle Root-CID als Textdatei unter `ipns/humanitytree-root.txt` ab.

## Verhalten beim Einfügen
- Mehrere `profileCid`s pro Tag möglich; Duplikate werden vermieden.
- Blätter haben ein Kapazitätslimit (`defaultLeafCapacity`, Standard 128). Wird es überschritten, splittet der Baum automatisch und hebt einen `InnerNode` an die Wurzel.
- `insert` liefert ein `InsertOutcome` mit neuem Root-CID, Ziel-Leaf-CID und Flag, ob ein Profil hinzugefügt wurde.

## Einbindung in eigene Apps
1. Gradle-Dependency (lokal veröffentlichen oder via `mavenLocal()`): siehe `build.gradle.kts`.
2. Store wählen (`InMemoryStore` für Tests, `FileStore` für einfache Persistenz; eigene `HumanityTreeStore`-Implementierungen sind möglich, z. B. für IPFS).
3. Baum mit konstantem `ipnsName` erzeugen, damit mehrere Instanzen denselben Root teilen.
4. `insert(tag, profileCid)` und `lookup(tag)` in Services/Handlern nutzen (z. B. HTTP-API oder CLI nach Bedarf selbst ergänzen).

## Nützliche Dateien/Referenzen
- Kernlogik: `src/commonMain/kotlin/at/crowdware/humanitytree/core/HumanityTree.kt`
- Datei-Store: `src/jvmMain/kotlin/at/crowdware/humanitytree/core/FileStore.kt`
- SML-Kodierung der Knoten: `src/commonMain/kotlin/at/crowdware/humanitytree/core/NodeCodec.kt`
