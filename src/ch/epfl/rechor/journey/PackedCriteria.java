package ch.epfl.rechor.journey;

import ch.epfl.rechor.Preconditions;
import static java.lang.Integer.toUnsignedLong;

/**
 * Classe pour manipuler des critères d'optimisation (heures de départ et d'arrivée,
 * nombre de changements, etc.) empaquetés en 64 bits.
 *
 * Ces critères peuvent inclure ou non une heure de départ : si elle est absente,
 * son champ (12 bits) vaut 0. Sont toujours stockés l'heure d'arrivée, le nombre
 * de changements, et une charge utile (payload) de 32 bits.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class PackedCriteria {

    private static final int DEP_BITS = 12;
    private static final int ARR_BITS = 12;
    private static final int CHG_BITS = 7;

    private static final int MAX_DEP = (1 << DEP_BITS) - 1;
    private static final int MAX_ARR = (1 << ARR_BITS) - 1;
    private static final int MAX_CHG = (1 << CHG_BITS) - 1;

    // Masques et décalages nommés
    private static final int DEP_SHIFT = ARR_BITS + CHG_BITS;
    private static final int ARR_SHIFT = CHG_BITS;

    private static final int DEP_MASK = (1 << DEP_BITS) - 1;
    private static final int ARR_MASK = (1 << ARR_BITS) - 1;
    private static final int CHG_MASK = (1 << CHG_BITS) - 1;

    private PackedCriteria() {}

    public static long pack(int depMins, int arrMins, int changes, int payload) {
        Preconditions.checkArgument(depMins >= -240 && depMins < 2880);
        Preconditions.checkArgument(arrMins >= -240 && arrMins < 2880);
        Preconditions.checkArgument(changes >= 0 && changes <= MAX_CHG);

        int depValue = encodeDep(depMins);
        int arrValue = encodeArr(arrMins);

        long criteria = (((long) depValue) << DEP_SHIFT)
                | (((long) arrValue) << ARR_SHIFT)
                | (long) changes;
        return (criteria << 32) | toUnsignedLong(payload);
    }

    public static long pack(int arrMins, int changes, int payload) {
        Preconditions.checkArgument(arrMins >= -240 && arrMins < 2880);
        Preconditions.checkArgument(changes >= 0 && changes <= MAX_CHG);

        int arrValue = encodeArr(arrMins);

        long criteria = (((long) 0) << DEP_SHIFT)
                | (((long) arrValue) << ARR_SHIFT)
                | (long) changes;
        return (criteria << 32) | toUnsignedLong(payload);
    }

    public static boolean hasDepMins(long criteria) {
        long crit = criteria >>> 32;
        int dep = (int) (crit >>> DEP_SHIFT);
        return dep != 0;
    }

    public static int depMins(long criteria) {
        Preconditions.checkArgument(hasDepMins(criteria));
        long crit = criteria >>> 32;
        int depValue = (int) (crit >>> DEP_SHIFT);
        return decodeDep(depValue);
    }

    public static int arrMins(long criteria) {
        long crit = criteria >>> 32;
        int arrValue = (int) ((crit >>> ARR_SHIFT) & ARR_MASK);
        return decodeArr(arrValue);
    }

    public static int changes(long criteria) {
        long crit = criteria >>> 32;
        return (int) (crit & CHG_MASK);
    }

    public static int payload(long criteria) {
        return (int) (criteria & 0xFFFFFFFFL);
    }

    public static boolean dominatesOrIsEqual(long criteria1, long criteria2) {
        boolean hasDep1 = hasDepMins(criteria1);
        boolean hasDep2 = hasDepMins(criteria2);
        Preconditions.checkArgument(hasDep1 == hasDep2);

        if (hasDep1) {
            int dep1 = depMins(criteria1);
            int dep2 = depMins(criteria2);
            int arr1 = arrMins(criteria1);
            int arr2 = arrMins(criteria2);
            int chg1 = changes(criteria1);
            int chg2 = changes(criteria2);
            return (dep1 >= dep2) && (arr1 <= arr2) && (chg1 <= chg2);
        } else {
            int arr1 = arrMins(criteria1);
            int arr2 = arrMins(criteria2);
            int chg1 = changes(criteria1);
            int chg2 = changes(criteria2);
            return (arr1 <= arr2) && (chg1 <= chg2);
        }
    }

    public static long withoutDepMins(long criteria) {
        long payload = criteria & 0xFFFFFFFFL;
        long crit = criteria >>> 32;
        long newCrit = crit & ~(DEP_MASK << DEP_SHIFT);
        return (newCrit << 32) | payload;
    }

    public static long withDepMins(long criteria, int depMins) {
        Preconditions.checkArgument(depMins >= -240 && depMins < 2880);

        long payload = criteria & 0xFFFFFFFFL;
        long crit = criteria >>> 32;

        int arrValue = (int) ((crit >>> ARR_SHIFT) & ARR_MASK);
        int changes = (int) (crit & CHG_MASK);
        int newDepValue = encodeDep(depMins);

        long newCrit = (((long) newDepValue) << DEP_SHIFT)
                | (((long) arrValue) << ARR_SHIFT)
                | (long) changes;

        return (newCrit << 32) | payload;
    }

    public static long withAdditionalChange(long criteria) {
        long payload = criteria & 0xFFFFFFFFL;
        long crit = criteria >>> 32;
        int chg = (int) (crit & CHG_MASK);
        Preconditions.checkArgument(chg < MAX_CHG);
        chg++;
        long newCrit = (crit & ~CHG_MASK) | chg;
        return (newCrit << 32) | payload;
    }

    public static long withPayload(long criteria, int payload1) {
        return (criteria & 0xFFFFFFFF00000000L) | toUnsignedLong(payload1);
    }

    // Méthodes utilitaires pour conversion minutes <-> champ codé
    private static int encodeDep(int depMins) {
        return MAX_DEP - (depMins + 240);
    }

    private static int decodeDep(int depValue) {
        return (MAX_DEP - depValue) - 240;
    }

    private static int encodeArr(int arrMins) {
        return arrMins + 240;
    }

    private static int decodeArr(int arrValue) {
        return arrValue - 240;
    }
}
