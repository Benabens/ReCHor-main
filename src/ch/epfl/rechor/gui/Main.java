package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyExtractor;
import ch.epfl.rechor.journey.Profile;
import ch.epfl.rechor.journey.Router;
import ch.epfl.rechor.timetable.CachedTimeTable;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Classe principale de l’application ReCHor.
 *
 * Elle charge les données horaires, construit l’interface graphique
 * et relie les différentes parties via des valeurs observables.
 *
 * @author Benabens (392901)
 * @author Jeremy (397366)
 */
public final class Main extends Application {
    /**
     * Propriété observable de la liste des voyages calculés.
     * Conservée dans un champ pour éviter qu’elle soit libérée par le ramasse‐miettes.
     */
    private ObservableValue<List<Journey>> journeysO;

    /**
     * Cache du dernier profil calculé, réutilisé lorsque la date et la station
     * d’arrivée n’ont pas changé. Manipulé UNIQUEMENT depuis le fil de l’exécuteur
     * {@link #profileExecutor} (mono‐thread), d’où l’absence de course de données ;
     * {@code volatile} garantit la visibilité entre deux tâches successives.
     */
    private volatile Profile profileCache;

    /**
     * Jeton de la dernière requête émise (incrémenté sur le fil JavaFX). À la fin
     * d’une tâche, son résultat n’est publié que s’il correspond encore au jeton
     * courant : les calculs supersédés (date/arrêt changés entre‐temps) sont ignorés.
     */
    private long computeToken = 0;

    /**
     * Exécuteur mono‐thread sur lequel s’exécute l’intégralité du calcul (profil
     * coûteux + extraction). Un seul calcul à la fois ⇒ un seul accès concurrent à
     * la table horaire mise en cache. Threads démons pour ne pas bloquer la fermeture.
     */
    private final ExecutorService profileExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "rechor-profile");
                t.setDaemon(true);
                return t;
            });

    /**
     * Point d’entrée de l’application.
     *
     * @param args arguments de la ligne de commande (ignorés)
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Initialise et affiche la fenêtre principale de l’application.
     *
     * @param stage fenêtre principale fournie par JavaFX
     * @throws IOException si le chargement des données horaires échoue
     */
    @Override
    public void start(Stage stage) throws IOException {
        // 1. Charger la table des horaires depuis ./timetable
        TimeTable tt = new CachedTimeTable(FileTimeTable.in(Path.of("timetable")));
        Router router = new Router(tt);

        // 2. Construire l'index des arrêts
        List<String> names = new ArrayList<>();
        Map<String,Integer> nameToId = new HashMap<>();
        for (int i = 0; i < tt.stations().size(); i++) {
            String station = tt.stations().name(i);
            names.add(station);
            nameToId.put(station, i);
        }
        Map<String,String> aliasMap = new HashMap<>();
        for (int i = 0; i < tt.stationAliases().size(); i++) {
            aliasMap.put(
                    tt.stationAliases().alias(i),
                    tt.stationAliases().stationName(i)
            );
        }
        StopIndex index = new StopIndex(names, aliasMap);

        // 3. Créer l'interface de requête
        var queryUI = QueryUI.create(index);
        var depStopO = queryUI.depStopO();
        var arrStopO = queryUI.arrStopO();
        var dateO    = queryUI.dateO();
        var timeO    = queryUI.timeO();

        // 4. Liste des voyages calculée HORS du fil JavaFX.
        //    Le calcul du profil (~1 s) est l'opération la plus coûteuse : l'exécuter
        //    sur le fil d'application figerait l'IHM. Tout le calcul (profil + extraction)
        //    est donc délégué à un exécuteur mono-thread ; seule la publication de la
        //    liste résultante se fait sur le fil JavaFX, via setOnSucceeded.
        SimpleObjectProperty<List<Journey>> journeysP =
                new SimpleObjectProperty<>(List.of());
        journeysO = journeysP;

        // Indicateur de chargement : vrai pendant qu'un calcul de PROFIL (l'opération
        // lente) est en cours. Une simple ré-extraction (changement de départ, profil
        // déjà en cache) ne l'allume pas, pour éviter tout clignotement.
        BooleanProperty loading = new SimpleBooleanProperty(false);

        Runnable recompute = () -> {
            String dep = depStopO.getValue();
            String arr = arrStopO.getValue();
            LocalDate date = dateO.getValue();
            if (dep == null || dep.isBlank()
                    || arr == null || arr.isBlank()
                    || date == null) {
                loading.set(false);
                journeysP.set(List.of());
                return;
            }
            Integer depId = nameToId.get(dep);
            Integer arrId = nameToId.get(arr);
            if (depId == null || arrId == null) {
                loading.set(false);
                journeysP.set(List.of());
                return;
            }

            final long myToken = ++computeToken;
            final LocalDate reqDate = date;
            final int reqArr = arrId;
            final int reqDep = depId;

            // Heuristique (lecture d'un champ volatile) : n'afficher l'indicateur que
            // sur le chemin lent (recalcul de profil), pas sur une simple extraction.
            Profile cached = profileCache;
            boolean needsProfile = cached == null
                    || !cached.date().equals(reqDate)
                    || cached.arrStationId() != reqArr;
            loading.set(needsProfile);

            Task<List<Journey>> task = new Task<>() {
                @Override
                protected List<Journey> call() {
                    // Exécuté sur le fil (unique) de profileExecutor : accès
                    // exclusif au cache de profil et à la table horaire.
                    Profile prof = profileCache;
                    if (prof == null
                            || !prof.date().equals(reqDate)
                            || prof.arrStationId() != reqArr) {
                        prof = router.profile(reqDate, reqArr);
                        profileCache = prof;
                    }
                    return JourneyExtractor.journeys(prof, reqDep);
                }
            };
            task.setOnSucceeded(e -> {
                if (myToken == computeToken) {
                    loading.set(false);
                    journeysP.set(task.getValue());
                }
            });
            task.setOnFailed(e -> {
                if (myToken == computeToken) {
                    loading.set(false);
                    journeysP.set(List.of());
                }
            });
            profileExecutor.submit(task);
        };

        depStopO.addListener((o, ov, nv) -> recompute.run());
        arrStopO.addListener((o, ov, nv) -> recompute.run());
        dateO.addListener((o, ov, nv) -> recompute.run());
        recompute.run();

        // 5. Construire SummaryUI et DetailUI
        var summaryUI = SummaryUI.create(journeysO, timeO);
        var detailUI  = DetailUI.create(summaryUI.selectedJourneyO());

        // Petit indicateur de chargement superposé au résumé, visible seulement
        // pendant un recalcul de profil (transparent aux clics).
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(48, 48);
        loadingIndicator.setMouseTransparent(true);
        loadingIndicator.visibleProperty().bind(loading);
        loadingIndicator.managedProperty().bind(loading);
        StackPane summaryPane = new StackPane(summaryUI.rootNode(), loadingIndicator);
        StackPane.setAlignment(loadingIndicator, Pos.TOP_CENTER);

        // 6. Assembler le graphe de scène
        SplitPane split = new SplitPane(
                summaryPane,
                detailUI.rootNode()
        );
        split.setDividerPositions(0.4);

        BorderPane root = new BorderPane();
        root.setTop(queryUI.rootNode());
        root.setCenter(split);

        // 7. Configurer et afficher la scène
        Scene scene = new Scene(root);
        // Une seule source par feuille (aucun nœud stylé deux fois) :
        //  - query.css au niveau Scene, car son sélecteur global « HBox » doit
        //    s'appliquer à toute la fenêtre ;
        //  - summary.css et detail.css sont chargées par SummaryUI / DetailUI,
        //    composants autonomes, donc inutile de les redoubler ici.
        scene.getStylesheets().add("query.css");
        stage.setScene(scene);
        stage.setTitle("ReCHor");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();

        // 8. Focus initial sur le champ "Départ"
        Platform.runLater(() -> {
            Node depField = scene.lookup("#depStop");
            if (depField != null) {
                depField.requestFocus();
            }
        });
    }

    /**
     * Arrête proprement l'exécuteur de calcul lors de la fermeture de l'application.
     */
    @Override
    public void stop() {
        profileExecutor.shutdownNow();
    }
}
