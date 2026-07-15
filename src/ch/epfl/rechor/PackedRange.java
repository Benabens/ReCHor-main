package ch.epfl.rechor;

/**
 * Classe permettant d'empaqueter un intervalle d'entiers dans un entier 32 bits,
 * en utilisant 24 bits pour la borne inférieure et 8 bits pour la longueur.
 * L'intervalle va de startInclusive (inclus) à endExclusive (exclu).
 * Les conditions imposent que startInclusive tienne sur 24 bits et que la longueur
 * (endExclusive - startInclusive) tienne sur 8 bits.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class PackedRange {

    /**
     * Constructeur privé, pas d'instanciation.
     */
    private PackedRange() {
        // Rien
    }

    /**
     * Empaquète la borne inférieure de l'intervalle (24 bits) et sa longueur (8 bits) dans un entier 32 bits.
     *
     * @param startInclusive borne inférieure (>= 0 et < 2^24)
     * @param endExclusive borne supérieure (>= startInclusive)
     * @return un entier 32 bits représentant l'intervalle
     * @throws IllegalArgumentException si startInclusive ou la longueur ne respectent pas leurs bornes
     */
    public static int pack(int startInclusive, int endExclusive) {
        Preconditions.checkArgument(startInclusive >= 0 && startInclusive < (1 << 24));
        int length = endExclusive - startInclusive;
        Preconditions.checkArgument(length >= 0 && length < 256);
        return Bits32_24_8.pack(startInclusive, length);
    }

    /**
     * Retourne la longueur de l'intervalle (endExclusive - startInclusive) à partir de l'entier empaqueté.
     *
     * @param interval l'entier 32 bits empaqueté
     * @return la longueur sur 8 bits extraite
     */
    public static int length(int interval) {
        return Bits32_24_8.unpack8(interval);
    }

    /**
     * Retourne la borne inférieure de l'intervalle à partir de l'entier empaqueté.
     *
     * @param interval l'entier 32 bits empaqueté
     * @return la valeur sur 24 bits extraite représentant startInclusive
     */
    public static int startInclusive(int interval) {
        return Bits32_24_8.unpack24(interval);
    }

    /**
     * Calcule la borne supérieure (endExclusive) en ajoutant la longueur à la borne inférieure.
     *
     * @param interval l'entier 32 bits empaqueté
     * @return la borne supérieure (exclue) de l'intervalle
     */
    public static int endExclusive(int interval) {
        return startInclusive(interval) + length(interval);
    }
}
