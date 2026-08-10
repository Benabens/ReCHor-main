# ReCHor

**Calculateur d'itinéraires pour les transports publics suisses** — application de bureau
Java / JavaFX qui calcule, pour une date, une heure et deux arrêts donnés, l'ensemble des
voyages optimaux du réseau suisse, puis les affiche et les exporte.

Projet semestriel du cours **CS-108 — Pratique de la programmation orientée objet** (EPFL),
réalisé en binôme.

> `Java 22` · `JavaFX 21` · 44 classes · horaires réels CFF (~258 Mo de données binaires)

---

## Fonctionnalités

- **Recherche d'arrêts avec autocomplétion** tolérante : insensible aux accents et à la
  casse, classement par pertinence (position dans le mot, début/fin de mot), gestion des
  alias d'arrêts et des requêtes en plusieurs mots.
- **Calcul de tous les voyages optimaux** entre deux arrêts pour une date donnée, avec
  correspondances à pied et changements de véhicule.
- **Résumé des voyages** (départ, arrivée, durée, véhicules empruntés) et **vue détaillée**
  d'un voyage : arrêts intermédiaires, voies/quais, trajets à pied.
- **Exports** : un voyage vers **iCalendar** (`.ics`, importable dans un agenda) et vers
  **GeoJSON** (tracé cartographique).
- Interface réactive : les calculs lourds tournent hors du fil graphique, avec indicateur
  de progression.

## Architecture

Le code est découpé en quatre ensembles, du plus bas niveau au plus haut :

| Package | Rôle |
|---|---|
| `ch.epfl.rechor` | Briques transverses : `Preconditions`, valeurs empaquetées (`Bits32_24_8`, `PackedRange`), formatage (`FormatterFr`), construction iCalendar (`IcalBuilder`) et JSON (`Json`), index de recherche d'arrêts (`StopIndex`). |
| `ch.epfl.rechor.timetable` | Modèle des horaires : `TimeTable`, `Stations`, `Platforms`, `Routes`, `Trips`, `Connections`, `Transfers`, `StationAliases`, plus un cache (`CachedTimeTable`). |
| `ch.epfl.rechor.timetable.mapped` | Implémentation « à plat » : lecture des horaires directement depuis des fichiers binaires mappés en mémoire, sans désérialisation objet. |
| `ch.epfl.rechor.journey` | Moteur de calcul : `Router`, `Profile`, `ParetoFront`, `PackedCriteria`, `JourneyExtractor`, puis exports (`JourneyIcalConverter`, `JourneyGeoJsonConverter`). |
| `ch.epfl.rechor.gui` | Interface JavaFX : `QueryUI` (formulaire), `StopField` (autocomplétion), `SummaryUI` (liste), `DetailUI` (détail), `Main`. |

### Le cœur algorithmique

Le calcul repose sur l'algorithme **CSA** (*Connection Scan Algorithm*) : les liaisons de la
journée sont parcourues **une seule fois, en ordre chronologique inverse**, ce qui évite de
reconstruire un graphe et rend la recherche très rapide.

L'optimisation est **multicritère** : on ne cherche pas seulement le voyage le plus rapide,
mais tous les voyages non dominés — un trajet plus lent avec moins de changements reste
pertinent. Ces optima sont conservés dans des **fronts de Pareto** (`ParetoFront`), où
chaque critère (heure d'arrivée, nombre de changements, heure de départ) est compressé dans
un `long` unique (`PackedCriteria`) pour limiter drastiquement les allocations.

### Le format binaire

Les horaires ne sont pas chargés en mémoire sous forme d'objets : ils sont lus à la volée
depuis des fichiers `.bin` mappés, via des accesseurs qui décodent des champs de bits
(`Bits32_24_8` empaquette par exemple un index sur 24 bits et une valeur sur 8 bits dans un
seul `int`). C'est ce qui permet de manipuler des centaines de milliers de liaisons sans
saturer la mémoire.

## Prérequis

- **JDK 22** ou plus récent
- **JavaFX SDK 21** (non inclus — [téléchargeable ici](https://gluonhq.com/products/javafx/))

## Lancer le projet

### Depuis IntelliJ IDEA

1. Ouvrir le dossier du projet.
2. Déclarer le JavaFX SDK 21 comme bibliothèque (*File → Project Structure → Libraries*),
   en pointant vers son dossier `lib/`.
3. Exécuter la classe `ch.epfl.rechor.gui.Main`.

### En ligne de commande

```bash
JFX=/chemin/vers/javafx-sdk-21/lib

javac -cp "$JFX/*" -d out $(find src/ch/epfl/rechor -name '*.java')
java  -cp "out:resources:$JFX/*" ch.epfl.rechor.gui.Main
```

> Si JavaFX est lancé depuis le *classpath*, la JVM affiche un avertissement
> « unnamed module » : il est sans conséquence.

## Structure du dépôt

```
src/ch/epfl/rechor/   code source (44 classes)
resources/            icônes des véhicules (PNG) et feuilles de style (CSS)
timetable/            horaires binaires — une journée par dossier (2025-05-26 → 06-01)
```

## Données

Les horaires couvrent une **période de démonstration d'une semaine** (26 mai → 1ᵉʳ juin
2025). Une recherche portant sur une date hors de cet intervalle renvoie simplement une
liste de voyages vide.

## Contexte

Projet développé dans le cadre du cours CS-108 de l'EPFL. L'énoncé, découpé en une douzaine
d'étapes hebdomadaires, imposait les signatures publiques de chaque classe ainsi que les
formats de sortie (iCalendar, GeoJSON) ; l'implémentation interne était libre.
