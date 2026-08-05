package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Platforms;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * BufferedPlatforms permet d'accéder à une table de voies ou quais représentée de manière aplatie.
 *
 * Le format de chaque enregistrement est le suivant :
 * - Champ NAME_FIELD (U16) : index de chaîne du nom de la voie/quai dans la table des chaînes.
 * - Champ STATION_FIELD (U16) : index de la gare parente (dans la table des gares).
 *
 * Chaque enregistrement occupe 4 octets (2 octets pour le nom et 2 octets pour l'index de la gare).
 *
 * @author Benjamin (392901)
 * @author Jeremy (397366)
 */
public final class BufferedPlatforms implements Platforms {

    private static final int NAME_FIELD = 0;
    private static final int STATION_FIELD = 1;

    private static final Structure PLATFORM_STRUCTURE = new Structure(
            Structure.field(NAME_FIELD, Structure.FieldType.U16),
            Structure.field(STATION_FIELD, Structure.FieldType.U16)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    /**
     * Construit une instance de BufferedPlatforms à partir d'une table de chaînes et d'un ByteBuffer.
     * Le ByteBuffer doit avoir une capacité multiple de la taille de la structure (4 octets par enregistrement).
     *
     * @param stringTable la table des chaînes
     * @param buffer le ByteBuffer contenant les données aplaties des voies/quais
     * @throws NullPointerException si stringTable ou buffer est null
     * @throws IllegalArgumentException si la capacité du buffer n'est pas un multiple de la taille de la structure
     */
    public BufferedPlatforms(List<String> stringTable, ByteBuffer buffer) {
        Objects.requireNonNull(stringTable);
        Objects.requireNonNull(buffer);
        this.stringTable = stringTable;
        this.buffer = new StructuredBuffer(PLATFORM_STRUCTURE, buffer);
    }

    /**
     * Retourne le nombre d'enregistrements (voies/quais) stockés.
     *
     * @return le nombre d'éléments
     */
    @Override
    public int size() {
        return buffer.size();
    }

    /**
     * Retourne le nom de la voie/quai d'index donné.
     * Le nom est obtenu en lisant le champ NAME_FIELD (U16) et en recherchant la chaîne correspondante dans la table des chaînes.
     *
     * @param id l'index de l'enregistrement (0 .. size()-1)
     * @return le nom de la voie/quai, ou une chaîne vide si aucun nom n'est spécifié
     * @throws IndexOutOfBoundsException si id est invalide
     */
    @Override
    public String name(int id) {
        int nameId = buffer.getU16(NAME_FIELD, id);
        return stringTable.get(nameId);
    }

    /**
     * Retourne l'index de la gare parente de la voie/quai d'index donné.
     * Ce champ est stocké en tant qu'entier non signé (U16).
     *
     * @param id l'index de l'enregistrement (0 .. size()-1)
     * @return l'index de la gare parente
     * @throws IndexOutOfBoundsException si id est invalide
     */
    @Override
    public int stationId(int id) {
        return buffer.getU16(STATION_FIELD, id);
    }
}
