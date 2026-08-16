# BG Stats

BG Stats ist eine mobile-first Spring-Boot-Webanwendung für persönliche BoardGameGeek-Statistiken. Sie lädt alle paginierten Plays eines konfigurierten BGG-Accounts mit dem [bggClient](https://github.com/jensGiehl/bggClient), behält nur Plays mit dem Account als Spieler, ergänzt die Spiele um BGG-Details und stellt die Ergebnisse mit Thymeleaf und Bootstrap dar.

## Funktionen

- Spielübersicht als dezente Cover-Kacheln mit gut lesbaren Titeln, Partien, Gesamtzeit, erster und letzter Partie, verständlichem Zeitraum sowie Siegen
- vollständige Kennzahlen über alle geladenen Plays, unabhängig vom Schwellenwert der Spieleliste
- Spieldetails aus BGG: Erscheinungsjahr, Spielerzahl, Spieldauer, Mindestalter, Wertung, Komplexität, Rang, Kategorien und Mechaniken
- konfigurierbarer Schwellenwert; standardmäßig erscheinen nur Spiele mit mehr als 8 Partien
- Sortierung der Spiele nach Gesamtzeit absteigend
- Jahresansicht mit Spielzeit, aktiven Tagen, Orten, Siegen, stärkstem Monat, Favorit und einer Tabelle aller Spiele
- Personensuche über BGG-Benutzernamen und Anzeigenamen aus den geladenen Plays, inklusive Siegquote, Lieblingsspiel und häufigsten Mitspielenden
- Ortsstatistik mit Rangliste, Spielzeit, Personen, Spieltagen und den Spielen des gewählten Orts
- frei kombinierbare Auswertung nach Jahr, Kategorie, Mitspieler, Ort, Spiel und Mindestwertung
- WhatsApp-formatierten Export je enthaltenem Spiel mit Partien, Spieltagen, Gesamt-, Maximal- und Durchschnittszeit sowie erstem, letztem und dazwischenliegendem Datum per Zwischenablage
- BoardGameGeek-formatierten Export derselben Statistik mit BGG-Spielverlinkung und ohne Emojis
- responsive Canvas-Diagramme auf allen Statistikseiten, die jeweils als PNG gespeichert werden können
- Personenstatistik mit einem Kreisdiagramm der zehn meistgespielten Spiele, BGG-Covern als Segmentfüllung und einer Sammelkategorie für alle übrigen Spiele
- asynchrones Laden des vollständigen Datenbestands beim Anwendungsstart mit einer automatisch verschwindenden Warteansicht
- zeitbasierter In-Memory-Cache und manuelle Aktualisierung in der Navigation
- JSON-Snapshot aller vollständig geladenen Daten für die lokale Entwicklung ohne BGG-API-Aufrufe
- verständliche deutsche Zeitangaben wie `1 Tag, 5 Stunden und 4 Minuten`

Bei einem Play mit `quantity > 1` werden Anzahl und Dauer entsprechend vervielfacht. Als Sieg zählt ein Play für eine Person nur, wenn BGG sie als Gewinner markiert und der Eintrag weder unvollständig noch von der Siegstatistik ausgeschlossen ist.

## Voraussetzungen

- Java 25
- Maven 3.9 oder neuer
- ein BoardGameGeek-API-Token für den API-Betrieb

Den Token legt man nach Anmeldung auf der [BGG-Seite für Anwendungen](https://boardgamegeek.com/applications) an.

## Konfiguration

Alle Anwendungseinstellungen befinden sich in [`src/main/resources/application.yml`](src/main/resources/application.yml). Werte können über die dort angegebenen Umgebungsvariablen überschrieben werden.

Für den normalen API-Betrieb sind erforderlich:

```bash
BGG_API_KEY=dein-api-token
BGG_USERNAME=dein-bgg-benutzername
```

Wichtige optionale Einstellungen:

| Variable | Standard | Bedeutung |
|---|---:|---|
| `SERVER_PORT` | `8080` | HTTP-Port der Anwendung |
| `BGG_INPUT_FILE` | leer | JSON-Datei als alleinige Datenquelle; API-Key und Benutzername sind dann nicht erforderlich |
| `BGG_SNAPSHOT_FILE` | `bg-stats-data.json` | Zieldatei für den Snapshot nach einem vollständigen API-Ladevorgang |
| `BGG_PLAY_THRESHOLD` | `8` | Angezeigt werden Spiele mit mehr Partien als dieser Wert |
| `BGG_CACHE_TTL` | `15m` | Lebensdauer der geladenen BGG-Daten |
| `BGG_COVER_BATCH_SIZE` | `20` | Anzahl Spiel-IDs pro Abfrage von Cover und Spieldetails |
| `BGG_REQUEST_TIMEOUT` | `60s` | Timeout einer BGG-Anfrage |
| `BGG_MAX_RETRIES` | `3` | Wiederholungen bei temporären BGG-Fehlern |
| `BGG_RETRY_BACKOFF` | `20s` | Pause zwischen Wiederholungen |

## Lokal starten

PowerShell:

```powershell
$env:BGG_API_KEY = "dein-api-token"
$env:BGG_USERNAME = "dein-bgg-benutzername"
mvn spring-boot:run
```

Danach ist die Anwendung unter [http://localhost:8080](http://localhost:8080) erreichbar.

Der vollständige Datenbestand wird nach dem Start asynchron geladen. Währenddessen ist die Anwendung bereits erreichbar und zeigt auf der Startseite eine Warteanimation. Sobald alle Partien und Spieldetails bereitstehen, öffnet sich die Statistikübersicht automatisch. Ein Ladefehler wird auf derselben Seite angezeigt und kann dort erneut angestoßen werden.

Beim ersten Laden der Daten wird der vollständige Katalog zusätzlich in `bg-stats-data.json` geschrieben. Diese generierte Datei ist in `.gitignore` eingetragen. Ein anderer Zielpfad kann mit `BGG_SNAPSHOT_FILE` oder `--bgg.snapshot-file=...` gesetzt werden.

### Mit lokalem JSON-Snapshot starten

Eine vorhandene Snapshot-Datei kann beim Start als Datenquelle angegeben werden. In diesem Modus erfolgen keine BGG-API-Aufrufe und es werden weder `BGG_API_KEY` noch `BGG_USERNAME` benötigt:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--bgg.input-file=bg-stats-data.json"
```

Alternativ mit dem gebauten JAR:

```powershell
java -jar target/bg-stats-1.0.0-SNAPSHOT.jar --bgg.input-file=bg-stats-data.json
```

Die Schaltfläche zum Aktualisieren liest im JSON-Modus die Datei erneut ein. Der Pfad kann alternativ über `BGG_INPUT_FILE` gesetzt werden.

Ältere Snapshots ohne Spieldetails bleiben lesbar. In diesem Fall sind Kategorien, Wertungen und davon abhängige Filter leer, bis die Daten einmal im API-Modus aktualisiert und neu gespeichert wurden.

## Tests und Build

```bash
mvn clean verify
java -jar target/bg-stats-1.0.0-SNAPSHOT.jar
```

Die Tests verwenden ausschließlich lokale Beispieldaten und rufen BoardGameGeek nicht auf.

## Docker

Image lokal bauen:

```bash
docker build -t bg-stats:latest .
```

### Veröffentlichtes Image

Die GitHub Action [`docker-image.yml`](.github/workflows/docker-image.yml) führt zuerst alle Maven-Tests aus und baut danach ein Image für `linux/amd64` und `linux/arm64`. Pull Requests werden nur gebaut. Bei einem Push auf `master` wird das Image in der GitHub Container Registry als `master`, `latest` und mit einem Commit-Tag wie `sha-a1b2c3d` veröffentlicht.

Ein Git-Tag wie `v1.2.3` erzeugt zusätzlich die Image-Tags `1.2.3` und `1.2`. Der Workflow kann außerdem im Reiter **Actions** manuell gestartet werden. Zur Anmeldung an der Registry verwendet er automatisch `GITHUB_TOKEN`; es muss kein zusätzliches Secret angelegt werden.

Das veröffentlichte Image kann beispielsweise so ersetzt und gestartet werden:

```bash
docker rm -f bg-stats 2>/dev/null

docker run -d \
  --name bg-stats \
  --pull=always \
  -p 8089:8080 \
  -v "$(pwd)/data:/data" \
  -e BGG_API_KEY="dein-api-token" \
  -e BGG_USERNAME="dein-bgg-benutzername" \
  ghcr.io/jensgiehl/my-boardgame-stats:latest
```

`8089` ist der Port auf dem Host; im Container lauscht die Anwendung auf Port `8080`. Der Ordner `data` im aktuellen Host-Verzeichnis wird nach `/data` eingebunden. Dort schreibt das Image standardmäßig `/data/bg-stats-data.json`.

Für den Betrieb ohne API wird eine vorhandene `bg-stats-data.json` in den Host-Ordner `data` gelegt und zusätzlich `-e BGG_INPUT_FILE="/data/bg-stats-data.json"` angegeben. Für das lokal gebaute Image `bg-stats:latest` sollte `--pull=always` durch `--pull=never` ersetzt werden.

Ist das Package in GitHub noch privat, muss es unter **Packages → Package settings → Change visibility** einmalig auf `Public` gestellt werden, damit das Image ohne Anmeldung heruntergeladen werden kann.

## Technischer Aufbau

- Java 25 und Spring Boot 4
- Maven
- Thymeleaf
- Bootstrap als WebJar
- Chart.js als WebJar
- bggClient `v1.0.0-1` über JitPack

Der API-Client ist in einer Gateway-Schicht gekapselt. Die Statistiklogik arbeitet auf eigenen unveränderlichen Domänenobjekten und ist dadurch unabhängig von HTTP und Templates testbar.
