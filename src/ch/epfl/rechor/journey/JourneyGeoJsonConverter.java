package ch.epfl.rechor.journey;

import ch.epfl.rechor.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Convertit un {@link Journey} en GeoJSON minimal représentant son tracé.
 *
 * Le GeoJSON produit est un objet de type LineString dont la liste de
 * coordonnées est :
 * - le point de départ de la première étape,
 * - tous les arrêts intermédiaires de chaque étape,
 * - le point d’arrivée de chaque étape.
 *
 * Les coordonnées (longitude, latitude) sont arrondies à 5 décimales,
 * et les points consécutifs identiques sont ignorés. Le document retourné
 * est compact, sans espaces ni sauts de ligne.
 *
 * @author Benjamin (392901)
 * @author Jeremy  (397366)
 */
public final class JourneyGeoJsonConverter {

    /**
     * Constructeur privé pour empêcher l’instanciation de cette classe utilitaire.
     */
    private JourneyGeoJsonConverter() {
        // ne doit pas être instancié
    }

    /**
     * Convertit le voyage donné en une chaîne GeoJSON compacte.
     *
     * @param journey le voyage à convertir (non null)
     * @return une chaîne GeoJSON de la forme
     *         {"type":"LineString","coordinates":[[lon,lat],...]}
     * @throws NullPointerException si {@code journey} est null
     */
    public static String toGeoJson(Journey journey) {
        Objects.requireNonNull(journey);

        List<double[]> points = new ArrayList<>();
        double prevLon = Double.NaN;
        double prevLat = Double.NaN;

        // point de départ de la première étape
        Journey.Leg first = journey.legs().get(0);
        addIfNew(points, first.depStop(), prevLon, prevLat);
        prevLon = round(first.depStop().longitude());
        prevLat = round(first.depStop().latitude());

        // points intermédiaires et arrêts d’arrivée
        for (Journey.Leg leg : journey.legs()) {
            for (Journey.Leg.IntermediateStop stop : leg.intermediateStops()) {
                addIfNew(points, stop.stop(), prevLon, prevLat);
                prevLon = round(stop.stop().longitude());
                prevLat = round(stop.stop().latitude());
            }
            addIfNew(points, leg.arrStop(), prevLon, prevLat);
            prevLon = round(leg.arrStop().longitude());
            prevLat = round(leg.arrStop().latitude());
        }

        // construction de l’objet JSON
        List<Json> coords = new ArrayList<>(points.size());
        for (double[] p : points) {
            coords.add(new Json.JArray(List.of(
                    new Json.JNumber(p[0]),
                    new Json.JNumber(p[1])
            )));
        }
        Map<String, Json> obj = new LinkedHashMap<>();
        obj.put("type",        new Json.JString("LineString"));
        obj.put("coordinates", new Json.JArray(coords));

        return new Json.JObject(obj).toString();
    }

    /**
     * Ajoute le point de l’arrêt donné à la liste s’il est différent
     * du précédent.
     *
     * @param pts      liste de tableaux [lon, lat] à enrichir
     * @param stop     arrêt dont on extrait les coordonnées
     * @param prevLon  longitude du dernier point ajouté (ou NaN)
     * @param prevLat  latitude du dernier point ajouté (ou NaN)
     */
    private static void addIfNew(List<double[]> pts, Stop stop,
                                 double prevLon, double prevLat) {
        double lon = round(stop.longitude());
        double lat = round(stop.latitude());
        if (Double.isNaN(prevLon) || lon != prevLon || lat != prevLat) {
            pts.add(new double[]{lon, lat});
        }
    }

    /**
     * Arrondit une valeur à 5 décimales.
     *
     * @param value valeur double à arrondir
     * @return valeur arrondie à 5 décimales
     */
    private static double round(double value) {
        return Math.round(value * 1e5) / 1e5d;
    }
}
