package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Preconditions;
import java.nio.ByteBuffer;

/**
 * Permet d'accéder à un tableau d'octets structuré selon une {@link Structure}.
 * Les éléments sont rangés consécutivement, chacun occupant {@code structure.totalSize()} octets.
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class StructuredBuffer {

    private final Structure structure;
    private final ByteBuffer buffer;
    private final int elementCount;

    /**
     * Construit un buffer structuré selon la structure donnée.
     *
     * @param structure la structure utilisée pour organiser les données
     * @param buffer le tableau d'octets contenant les éléments
     * @throws IllegalArgumentException si la taille du buffer n'est pas un multiple de la taille d'un élément
     */
    public StructuredBuffer(Structure structure, ByteBuffer buffer) {
        java.util.Objects.requireNonNull(structure);
        java.util.Objects.requireNonNull(buffer);
        int totalBytes = buffer.capacity();
        int size = structure.totalSize();
        Preconditions.checkArgument(totalBytes % size == 0);
        this.structure = structure;
        this.buffer = buffer;
        this.elementCount = totalBytes / size;
    }

    /**
     * Retourne le nombre total d’éléments stockés dans le buffer.
     *
     * @return le nombre d’éléments dans le buffer
     */
    public int size() {
        return elementCount;
    }

    /**
     * Lit un champ de type U8 (entier non signé, 1 octet).
     *
     * @param fieldIndex l'index du champ à lire
     * @param elementIndex l'index de l’élément (0, 1, 2, ...)
     * @return la valeur non signée (entre 0 et 255)
     */
    public int getU8(int fieldIndex, int elementIndex) {
        byte b = buffer.get(offset(fieldIndex, elementIndex));
        return Byte.toUnsignedInt(b);
    }

    /**
     * Lit un champ de type U16 (entier non signé, 2 octets).
     *
     * @param fieldIndex l'index du champ à lire
     * @param elementIndex l'index de l’élément
     * @return la valeur non signée (entre 0 et 65535)
     */
    public int getU16(int fieldIndex, int elementIndex) {
        short s = buffer.getShort(offset(fieldIndex, elementIndex));
        return Short.toUnsignedInt(s);
    }

    /**
     * Lit un champ de type S32 (entier signé, 4 octets).
     *
     * @param fieldIndex l'index du champ à lire
     * @param elementIndex l'index de l’élément
     * @return la valeur signée
     */
    public int getS32(int fieldIndex, int elementIndex) {
        return buffer.getInt(offset(fieldIndex, elementIndex));
    }

    /**
     * Calcule la position absolue (en octets) dans le buffer
     * pour un champ et un élément donnés.
     *
     * @param fieldIndex l’index du champ
     * @param elementIndex l’index de l’élément
     * @return l’offset dans le buffer
     */
    private int offset(int fieldIndex, int elementIndex) {
        return structure.offset(fieldIndex, elementIndex);
    }
}