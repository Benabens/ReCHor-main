package ch.epfl.rechor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Représente une valeur JSON scellée :
 * JArray pour un tableau, JObject pour un objet,
 * JString pour une chaîne, JNumber pour un nombre.
 *
 * Chaque implémentation redéfinit toString() pour renvoyer
 * une représentation JSON compacte, sans espace ni retour à la ligne.
 *
 * @author Benabens (392901)
 * @author Jeremy (397366)
 */
public sealed interface Json
        permits Json.JArray, Json.JObject, Json.JString, Json.JNumber {

    /**
     * Échappe une chaîne Java en chaîne JSON entre guillemets,
     * en gérant les caractères spéciaux et de contrôle.
     *
     * @param s la chaîne à échapper (non null)
     * @return la chaîne JSON correspondante, par ex. "foo\nbar"
     * @throws NullPointerException si {@code s} est null
     */
    static String escape(String s) {
        Objects.requireNonNull(s);
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Représente un tableau JSON.
     *
     * @param values la liste des éléments JSON (non null, chaque élément non null)
     * @throws NullPointerException si {@code values} est null
     *                                  ou si un élément de {@code values} est null
     */
    public record JArray(List<Json> values) implements Json {
        public JArray {
            // copie défensive immuable (rejette aussi values null et éléments null)
            values = List.copyOf(values);
        }

        @Override
        public String toString() {
            return values.stream()
                    .map(Json::toString)
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        }
    }

    /**
     * Représente un objet JSON.
     *
     * @param members la map associant chaque clé JSON à sa valeur
     *                (non null, clés non null et non vides, valeurs non null)
     * @throws NullPointerException     si {@code members} est null
     *                                  ou contient clé ou valeur null
     * @throws IllegalArgumentException si une clé est vide
     */
    public record JObject(Map<String, Json> members) implements Json {
        public JObject {
            Objects.requireNonNull(members);
            for (var e : members.entrySet()) {
                String key = e.getKey();
                Objects.requireNonNull(key);
                ch.epfl.rechor.Preconditions.checkArgument(!key.isEmpty());
                Objects.requireNonNull(e.getValue());
            }
            // copie défensive préservant l'ordre d'insertion (important pour le GeoJSON)
            members = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(members));
        }

        @Override
        public String toString() {
            return members.entrySet().stream()
                    .map(e -> Json.escape(e.getKey()) + ":" + e.getValue())
                    .collect(java.util.stream.Collectors.joining(",", "{", "}"));
        }
    }

    /**
     * Représente une chaîne JSON.
     *
     * @param value le contenu brut de la chaîne (non null)
     * @throws NullPointerException si {@code value} est null
     */
    public record JString(String value) implements Json {
        public JString {
            Objects.requireNonNull(value);
        }

        @Override
        public String toString() {
            return Json.escape(value);
        }
    }

    /**
     * Représente un nombre JSON.
     *
     * @param value la valeur numérique
     */
    public record JNumber(double value) implements Json {
        /**
         * Retourne la représentation JSON minimale de {@code value}.
         * - rejette les valeurs non-finies (NaN, ±∞),
         * - sérialise -0.0 en "0",
         * - supprime ".0" pour les entiers.
         *
         * @return chaîne JSON correspondante
         * @throws IllegalArgumentException si {@code value} est NaN ou infini
         */
        @Override
        public String toString() {
            ch.epfl.rechor.Preconditions.checkArgument(!Double.isNaN(value) && !Double.isInfinite(value));
            if (Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(-0.0d)) {
                return "0";
            }
            if (value == (long) value) {
                return Long.toString((long) value);
            }
            return Double.toString(value);
        }
    }
}
