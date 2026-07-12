package ch.epfl.rechor;

/**
 * Classe pour empaqueter et désempaqueter deux valeurs, l'une occupant 24 bits
 * et l'autre 8 bits, dans un entier 32 bits.
 * La valeur sur 24 bits est placée dans les bits de poids fort et la valeur sur 8 bits dans les bits de poids faible.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class Bits32_24_8 {

    /**
     * Constructeur privé pour empêcher l'instanciation.
     */
    private Bits32_24_8() {
        // Rien à faire.
    }

    /**
     * Empaquète deux valeurs entières (24 bits et 8 bits) dans un entier 32 bits.
     * Vérifie que la valeur sur 24 bits tient sur 24 bits et que la valeur sur 8 bits tient sur 8 bits.
     *
     * @param bits24 la valeur sur 24 bits (poids fort)
     * @param bits8 la valeur sur 8 bits (poids faible)
     * @return l'entier 32 bits combiné
     * @throws IllegalArgumentException si bits24 ou bits8 dépasse sa taille autorisée
     */
    public static int pack(int bits24, int bits8) {
        Preconditions.checkArgument((bits24 >> 24) == 0);
        Preconditions.checkArgument((bits8 >> 8) == 0);
        return (bits24 << 8) | bits8;
    }

    /**
     * Extrait la partie sur 24 bits (bits de poids fort) d'un entier 32 bits.
     *
     * @param bits32 l'entier 32 bits initial
     * @return la valeur sur 24 bits extraite
     */
    public static int unpack24(int bits32) {
        return bits32 >>> 8;
    }

    /**
     * Extrait la partie sur 8 bits (bits de poids faible) d'un entier 32 bits.
     *
     * @param bits32 l'entier 32 bits initial
     * @return la valeur sur 8 bits extraite
     */
    public static int unpack8(int bits32) {
        return bits32 & 0xFF;
    }
}
