package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enregistrement combinant un champ textuel et la valeur observable
 * du nom d’arrêt sélectionné, avec autocomplétion via un StopIndex.
 *
 * @param field champ de texte pour la saisie de la requête ou du nom validé
 * @param stopO valeur observable (StringProperty) du nom d’arrêt validé
 *              (toujours un nom valide ou chaîne vide)
 *
 * @author Benjamin (392901)
 * @author Jeremy  (397366)
 */
public record StopField(TextField field, ObservableValue<String> stopO) {
    /** Nombre maximal de suggestions affichées. */
    private static final int MAX_RESULTS = 30;
    /** Hauteur maximale de la liste de suggestions. */
    private static final double MAX_LIST_HEIGHT = 240;
    /** Feuille de style pour la liste de suggestions. */
    private static final String STYLE_SHEET = "query.css";
    /** Délai d’inactivité (ms) avant de lancer une recherche (debounce). */
    private static final int DEBOUNCE_MS = 150;
    /** Bloque temporairement l’apparition des popups lors de mises à jour programmatiques. */
    private static boolean suppressPopup = false;
    /**
     * Association TextField → Popup pour garder le Popup en vie
     * sans l’exposer dans la signature publique.
     */
    private static final WeakHashMap<TextField, Popup> POPUPS = new WeakHashMap<>();
    /** Association TextField → minuterie de debounce, pour pouvoir l’annuler dans setTo(). */
    private static final WeakHashMap<TextField, PauseTransition> DEBOUNCERS = new WeakHashMap<>();
    /**
     * Exécuteur mono‐thread (démon) sur lequel tourne {@code stopsMatching}, afin de
     * ne jamais bloquer le fil JavaFX pendant la frappe. Partagé par tous les champs.
     */
    private static final ExecutorService SEARCH_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "rechor-stop-search");
                t.setDaemon(true);
                return t;
            });

    /**
     * Crée un StopField (TextField + Popup) pour l’autocomplétion à partir
     * de l’index d’arrêts fourni.
     *
     * @param index index des arrêts (non null)
     * @return StopField prêt à l’emploi
     * @throws NullPointerException si {@code index} est null
     */
    public static StopField create(StopIndex index) {
        Objects.requireNonNull(index);

        TextField tf = new TextField();
        tf.setPromptText("Nom de l’arrêt…");
        StringProperty selected = new SimpleStringProperty("");

        Popup popup = new Popup();
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);

        ListView<String> listView = new ListView<>();
        listView.setFocusTraversable(false);
        listView.setMaxHeight(MAX_LIST_HEIGHT);
        listView.getStylesheets().add(STYLE_SHEET);
        popup.getContent().add(listView);

        // On garde le popup associé pour le retrouver dans setTo()
        POPUPS.put(tf, popup);

        // Repositionne le popup si la fenêtre bouge ou change de taille
        Runnable reposition = () -> {
            if (!popup.isShowing()) return;
            Bounds b = tf.localToScreen(tf.getBoundsInLocal());
            if (b != null) {
                popup.setAnchorX(b.getMinX());
                popup.setAnchorY(b.getMaxY());
            }
        };

        // Suit les déplacements de la fenêtre. Les listeners sont enregistrés UNE SEULE FOIS
        // (quand la scène puis la fenêtre deviennent disponibles), et non à chaque ouverture
        // du popup — ce qui, avant, accumulait des listeners sans fin (fuite).
        tf.sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene == null) return;
            newScene.windowProperty().addListener((o2, oldWin, newWin) -> {
                if (newWin != null) {
                    newWin.xProperty().addListener((a, b1, c) -> reposition.run());
                    newWin.yProperty().addListener((a, b1, c) -> reposition.run());
                }
            });
        });

        // Jeton pour écarter les résultats d'une recherche supersédée.
        long[] searchToken = {0};

        // Lance une recherche HORS du fil JavaFX (stopsMatching parcourt tous les arrêts),
        // puis publie le résultat dans la ListView via le fil JavaFX (setOnSucceeded).
        Runnable launchSearch = () -> {
            if (suppressPopup) return;
            final long myToken = ++searchToken[0];
            final String raw = tf.getText();
            Task<List<String>> task = new Task<>() {
                @Override
                protected List<String> call() {
                    String q = (raw == null || raw.isBlank()) ? "" : raw.trim();
                    return index.stopsMatching(q, MAX_RESULTS);
                }
            };
            task.setOnSucceeded(e -> {
                if (myToken != searchToken[0] || suppressPopup) return;
                List<String> suggestions = task.getValue();
                if (suggestions.isEmpty()) {
                    popup.hide();
                    return;
                }
                listView.getItems().setAll(suggestions);
                listView.getSelectionModel().selectFirst();
                if (!popup.isShowing() && tf.getScene() != null) {
                    Bounds b = tf.localToScreen(tf.getBoundsInLocal());
                    if (b != null) {
                        popup.show(tf, b.getMinX(), b.getMaxY());
                    }
                }
            });
            SEARCH_EXECUTOR.submit(task);
        };

        // Debounce : ne recherche qu'après ~150 ms sans nouvelle frappe.
        PauseTransition debounce = new PauseTransition(Duration.millis(DEBOUNCE_MS));
        debounce.setOnFinished(e -> launchSearch.run());
        DEBOUNCERS.put(tf, debounce);

        // Valide la sélection (ENTER/TAB ou clic)
        Runnable commit = () -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            if (sel == null) sel = "";
            tf.setText(sel);
            selected.set(sel);
            popup.hide();
            // setText a relancé le debounce via le listener : on l'annule pour ne pas rouvrir le popup.
            debounce.stop();
        };

        // Navigation clavier dans la liste
        tf.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (!popup.isShowing()) return;
            switch (e.getCode()) {
                case UP    -> { listView.getSelectionModel().selectPrevious(); e.consume(); }
                case DOWN  -> { listView.getSelectionModel().selectNext();     e.consume(); }
                case ENTER, TAB -> { commit.run(); e.consume(); }
                default    -> { }
            }
        });

        // Frappe : (re)démarre le debounce ; la recherche part après la pause.
        tf.textProperty().addListener((obs, oldText, newText) -> {
            if (suppressPopup) return;
            debounce.playFromStart();
        });

        // Focus : affiche immédiatement des suggestions (recherche asynchrone) ou masque.
        tf.focusedProperty().addListener((obs, was, isNow) -> {
            if (isNow && !suppressPopup) {
                launchSearch.run();
            } else {
                popup.hide();
            }
        });

        // Validation par clic sur la liste
        listView.setOnMouseClicked(e -> commit.run());

        return new StopField(tf, selected);
    }

    /**
     * Met à jour le champ sans rouvrir le popup (utilisé pour les mises
     * à jour programmatiques, ex. swap), et remet en service l’affichage
     * après un tour de boucle JavaFX.
     *
     * @param stopName nom d’arrêt à afficher (non null)
     * @throws NullPointerException si {@code stopName} est null
     */
    public void setTo(String stopName) {
        Objects.requireNonNull(stopName);
        suppressPopup = true;
        PauseTransition d = DEBOUNCERS.get(field);
        if (d != null) {
            d.stop();
        }
        Popup p = POPUPS.get(field);
        if (p != null) {
            p.hide();
        }
        field.setText(stopName);
        ((StringProperty) stopO).setValue(stopName);
        Platform.runLater(() -> suppressPopup = false);
    }

    /**
     * Masque le popup d'autocomplétion s'il est affiché, sans toucher au texte,
     * au focus ni à la valeur validée. Utilisé par {@link QueryUI} pour fermer
     * proprement une suggestion ouverte dès qu'un clic vise un autre contrôle
     * (p. ex. le DatePicker), afin de garantir l'ouverture de ce contrôle en un
     * seul clic (sinon le premier clic ne servirait qu'à fermer ce popup).
     */
    void hidePopup() {
        Popup p = POPUPS.get(field);
        if (p != null) {
            p.hide();
        }
    }
}
