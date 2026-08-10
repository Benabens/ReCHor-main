package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.CachedTimeTable;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests d'intégration ciblant les horaires des étapes de marche (Foot), qui n'étaient
 * pas couverts. Ils reconstruisent de vrais voyages via {@link Router} +
 * {@link JourneyExtractor} sur les données réelles du dossier {@code timetable/}.
 *
 * Ils gardent notamment contre la régression corrigée où la marche initiale utilisait
 * l'heure 0 (minuit la veille) au lieu de l'heure de départ de la première liaison.
 *
 * Le test se contente de s'auto‑désactiver ({@code assumeTrue}) si les données de la
 * date choisie ne sont pas présentes, afin de ne jamais échouer faute de données.
 */
class WalkLegTimingTest {

    private static final Path TT_DIR = Path.of("timetable");
    private static final LocalDate DATE = LocalDate.of(2025, 5, 28);

    private TimeTable tt;
    private Router router;
    private Map<String, Integer> nameToId;

    @BeforeEach
    void setUp() throws IOException {
        assumeTrue(Files.isDirectory(TT_DIR.resolve(DATE.toString())),
                "Données horaires pour " + DATE + " absentes : test ignoré.");
        tt = new CachedTimeTable(FileTimeTable.in(TT_DIR));
        router = new Router(tt);
        nameToId = new HashMap<>();
        for (int i = 0; i < tt.stations().size(); i++) {
            nameToId.put(tt.stations().name(i), i);
        }
    }

    private int id(String name) {
        Integer i = nameToId.get(name);
        assumeTrue(i != null, "Gare '" + name + "' absente des données : test ignoré.");
        return i;
    }

    /** Toute étape de marche doit être bien formée et située dans l'intervalle du voyage. */
    @Test
    void footLegsAreWellFormedAndOnTheJourneyDay() {
        int dep = id("Lausanne");
        int arr = id("Zermatt");
        List<Journey> journeys = JourneyExtractor.journeys(router.profile(DATE, arr), dep);

        long withFoot = journeys.stream()
                .filter(j -> j.legs().stream().anyMatch(l -> l instanceof Journey.Leg.Foot))
                .count();
        assertTrue(withFoot > 0,
                "Attendu au moins un voyage Lausanne→Zermatt avec une étape à pied/changement.");

        for (Journey j : journeys) {
            for (Journey.Leg leg : j.legs()) {
                if (leg instanceof Journey.Leg.Foot foot) {
                    // départ ≤ arrivée
                    assertFalse(foot.depTime().isAfter(foot.arrTime()),
                            "Étape à pied avec départ après arrivée.");
                    // strictement dans la fenêtre du voyage
                    assertFalse(foot.depTime().isBefore(j.depTime()),
                            "Étape à pied démarrant avant le départ du voyage.");
                    assertFalse(foot.arrTime().isAfter(j.arrTime()),
                            "Étape à pied finissant après l'arrivée du voyage.");
                    // régression : jamais datée la veille (marche initiale à minuit veille)
                    assertFalse(foot.depTime().toLocalDate().isBefore(DATE),
                            "Étape à pied datée avant le jour du voyage (régression marche initiale).");
                }
            }
        }
    }

    /** Les étapes consécutives sont contiguës dans le temps : arrivée ≤ départ suivant. */
    @Test
    void consecutiveLegsAreTimeContiguous() {
        int dep = id("Lausanne");
        int arr = id("Zermatt");
        List<Journey> journeys = JourneyExtractor.journeys(router.profile(DATE, arr), dep);
        assertFalse(journeys.isEmpty(), "Aucun voyage Lausanne→Zermatt trouvé.");

        for (Journey j : journeys) {
            List<Journey.Leg> legs = j.legs();
            for (int i = 0; i + 1 < legs.size(); i++) {
                assertFalse(legs.get(i).arrTime().isAfter(legs.get(i + 1).depTime()),
                        "Étape " + i + " arrive après le départ de l'étape suivante.");
            }
        }
    }

    /**
     * Cherche un voyage commençant par une marche (marche initiale) et vérifie qu'elle
     * arrive au plus tard au départ de la première liaison, le bon jour. C'est le cas
     * précis corrigé (l'heure de la marche initiale était fausse).
     */
    @Test
    void initialFootLegArrivesAtFirstTransportDeparture() {
        int arr = id("Zermatt");
        Profile profile = router.profile(DATE, arr);

        Journey found = null;
        for (int station = 0; station < tt.stations().size() && found == null; station++) {
            for (Journey j : JourneyExtractor.journeys(profile, station)) {
                if (j.legs().size() >= 2
                        && j.legs().get(0) instanceof Journey.Leg.Foot
                        && j.legs().get(1) instanceof Journey.Leg.Transport) {
                    found = j;
                    break;
                }
            }
        }
        assumeTrue(found != null,
                "Aucun voyage avec marche initiale trouvé dans ces données : sous‑test ignoré.");

        Journey.Leg.Foot initial = (Journey.Leg.Foot) found.legs().get(0);
        Journey.Leg.Transport first = (Journey.Leg.Transport) found.legs().get(1);

        // la marche initiale doit arriver au départ de la première liaison (contiguïté)
        assertEquals(first.depTime(), initial.arrTime(),
                "La marche initiale doit arriver exactement au départ de la première liaison.");
        // et se dérouler le jour du voyage (pas la veille à minuit)
        assertEquals(DATE, initial.depTime().toLocalDate(),
                "La marche initiale ne doit pas être datée la veille.");
        assertFalse(initial.depTime().isAfter(initial.arrTime()),
                "Marche initiale : départ après arrivée.");
    }
}
