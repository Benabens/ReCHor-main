# ReCHor

Calculateur d'itinéraires de transports publics (réseau suisse), en **Java + JavaFX**.
Projet semestriel du cours **EPFL CS-108** (Pratique de la programmation orientée objet).

## Architecture

- **`timetable/` (+ `timetable/mapped/`)** — lecture des horaires depuis des fichiers
  binaires plats mappés en mémoire (« valeurs empaquetées »), avec cache.
- **`journey/`** — moteur de recherche : *Connection Scan Algorithm* + **front de Pareto**,
  extraction des voyages optimaux et exports **iCalendar** / **GeoJSON**.
- **`gui/`** — interface **JavaFX** : recherche d'arrêts (autocomplétion), résumé des
  voyages et vue détaillée.

## Lancer

Ouvrir le projet dans IntelliJ (JavaFX 21 requis), puis exécuter
`ch.epfl.rechor.gui.Main`. Les données de `timetable/` couvrent une période de démonstration.
