package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Preconditions;
import java.util.Objects;

/**
 * Décrit la structure (liste de champs) d'une table de données aplaties.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class Structure {

    /**
     * Représente les trois types de champs possibles : U8, U16, S32.
     */
    public enum FieldType {
        U8,   // 1 octet (non signé)
        U16,  // 2 octets (non signés)
        S32   // 4 octets (signés)
    }

    /**
     * Représente un champ, composé de :
     *   - index : position (0, 1, 2, ...)
     *   - type : U8, U16, S32.
     * Le constructeur lève une NullPointerException si type est null.
     *
     * @author Benjamin (392901)
     * @author Jeremy (397366)
     */
    public static record Field(int index, FieldType type) {
        public Field {
            Objects.requireNonNull(type);
        }
    }

    /**
     * Méthode statique pour créer un Field plus concis.
     *
     * @param index index du champ (0, 1, 2, ...)
     * @param type le type du champ (U8, U16, S32)
     * @return un Field correspondant
     */
    public static Field field(int index, FieldType type) {
        return new Field(index, type);
    }

    // Tableau des champs
    private final Field[] fields;
    // offsets[i] = position en octets du champ i dans la structure
    private final int[] offsets;
    // taille totale (en octets) de la structure
    private final int totalSize;

    /**
     * Construit une Structure à partir d'un tableau de champs.
     *
     * @param fields liste des champs
     * @throws IllegalArgumentException si fields est vide ou si les champs ne sont pas ordonnés correctement
     */
    public Structure(Field... fields) {
        Preconditions.checkArgument(fields.length != 0);
        for (int i = 0; i < fields.length; i++) {
            Preconditions.checkArgument(fields[i].index() == i);
        }
        this.fields = fields.clone(); // copie pour l'immutabilité

        offsets = new int[fields.length];
        int offset = 0;
        for (int i = 0; i < fields.length; i++) {
            offsets[i] = offset;
            offset += sizeOf(fields[i].type());
        }
        totalSize = offset;
    }

    /**
     * Retourne la taille totale (en octets) de la structure.
     *
     * @return la taille totale en octets
     */
    public int totalSize() {
        return totalSize;
    }

    /**
     * Retourne l'offset (en octets) du champ d'index fieldIndex pour l'élément d'index elementIndex.
     * La formule utilisée est : offsets[fieldIndex] + elementIndex * totalSize.
     *
     * @param fieldIndex l'index du champ
     * @param elementIndex l'index de l'élément (p. ex. la ième gare)
     * @return la position (en octets) dans le tableau de bytes
     * @throws IndexOutOfBoundsException si elementIndex est négatif ou si fieldIndex est hors borne
     */
    public int offset(int fieldIndex, int elementIndex) {
        if (elementIndex < 0) {
            throw new IndexOutOfBoundsException();
        }
        return offsets[fieldIndex] + elementIndex * totalSize;
    }

    // Méthode utilitaire pour connaître la taille (en octets) d'un type
    private static int sizeOf(FieldType type) {
        return switch (type) {
            case U8 -> 1;
            case U16 -> 2;
            case S32 -> 4;
        };
    }
}
