package ch.epfl.rechor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de {@link StopIndex} (classe qui n'était pas couverte).
 *
 * Le test central vérifie la sémantique ET exigée par l'énoncé : chaque sous-requête
 * doit correspondre au nom pour que l'arrêt soit retenu (une sous-requête sans
 * correspondance écarte l'arrêt) ; le score total est alors la SOMME des scores des
 * sous-requêtes.
 */
class StopIndexTest {

    private static StopIndex index() {
        return new StopIndex(
                List.of("Lausanne", "Lausanne-Flon", "Genève", "Genève-Aéroport", "Renens VD", "Nyon"),
                Map.of());
    }

    @Test
    void multiWordRequiresEverySubqueryToMatch() {
        // Aucun arrêt ne contient à la fois « lausanne » ET « geneve » :
        // chaque sous-requête devant correspondre, tous les arrêts sont écartés.
        List<String> r = index().stopsMatching("lausanne geneve", 10);
        assertTrue(r.isEmpty(), "attendu vide, obtenu : " + r);
    }

    @Test
    void multiWordKeepsNamesMatchingAllSubqueries() {
        List<String> r = index().stopsMatching("geneve aeroport", 10);
        assertTrue(r.contains("Genève-Aéroport"), "obtenu : " + r);
        assertFalse(r.contains("Genève"), "Genève ne contient pas « aeroport »");
    }

    @Test
    void singleWordStillMatchesAllContainingIt() {
        List<String> r = index().stopsMatching("lausanne", 10);
        assertTrue(r.contains("Lausanne"));
        assertTrue(r.contains("Lausanne-Flon"));
        assertFalse(r.contains("Nyon"));
    }

    @Test
    void isAccentAndCaseInsensitiveByDefault() {
        List<String> r = index().stopsMatching("geneve", 10);
        assertTrue(r.contains("Genève"), "obtenu : " + r);
        assertTrue(r.contains("Genève-Aéroport"), "obtenu : " + r);
    }

    @Test
    void respectsMaxSize() {
        assertEquals(1, index().stopsMatching("", 1).size());
        assertTrue(index().stopsMatching("e", 2).size() <= 2);
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(NullPointerException.class, () -> index().stopsMatching(null, 5));
        assertThrows(IllegalArgumentException.class, () -> index().stopsMatching("a", -1));
    }
}
