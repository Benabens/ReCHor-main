package ch.epfl.rechor.gui;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.journey.Journey;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Affiche la vue d’ensemble des voyages optimaux d’une journée.
 *
 * Chaque entrée de la liste présente pour un {@link Journey} :
 *  - l’icône et la destination de la première étape en transport,
 *  - l’heure de départ du voyage (style « departure »),
 *  - une barre chronologique avec des cercles noirs aux extrémités
 *    et des cercles blancs pour chaque transfert, placés proportionnellement,
 *  - l’heure d’arrivée du voyage,
 *  - la durée totale du voyage (style « duration »).
 *
 * La liste est triée par heure de départ puis heure d’arrivée croissante, et
 * sélectionne automatiquement le premier voyage partant à ou après l’heure cible,
 * ou le dernier si aucun ne convient.
 *
 * @param rootNode         le nœud JavaFX racine de cette vue (un ListView<Journey>)
 * @param selectedJourneyO observable renvoyant le {@link Journey} sélectionné
 *
 * @author Benjamin (392901)
 * @author Jeremy  (397366)
 */
public record SummaryUI(Node rootNode,
                        ObservableValue<Journey> selectedJourneyO) {

    /**
     * Crée l’interface summary.
     *
     * @param journeysO    observable fournissant la liste des voyages
     * @param targetTimeO  observable fournissant l’heure cible de départ
     * @return une instance de SummaryUI prête à l’emploi
     * @throws NullPointerException si l’un des paramètres est null
     */
    public static SummaryUI create(ObservableValue<List<Journey>> journeysO,
                                   ObservableValue<LocalTime>   targetTimeO) {
        Objects.requireNonNull(journeysO);
        Objects.requireNonNull(targetTimeO);

        ListView<Journey> listView = new ListView<>();
        listView.setId("summary");
        listView.getStylesheets().add("summary.css");
        listView.setCellFactory(lv -> new SummaryCell());

        ObjectProperty<Journey> selProp = new SimpleObjectProperty<>();
        listView.getSelectionModel()
                .selectedItemProperty()
                .addListener((o, a, b) -> selProp.set(b));

        // tri et sélection automatique
        journeysO.addListener((o, oldL, newL) -> {
            listView.setItems(sorted(newL));
            selectByTime(listView, targetTimeO.getValue());
        });
        targetTimeO.addListener((o, oldT, newT) ->
                selectByTime(listView, newT)
        );

        // initialisation
        listView.setItems(sorted(journeysO.getValue()));
        selectByTime(listView, targetTimeO.getValue());

        return new SummaryUI(listView, selProp);
    }

    /**
     * Trie la liste des voyages par heure de départ puis d’arrivée.
     *
     * @param list liste non triée de {@link Journey}
     * @return liste observable triée
     */
    private static ObservableList<Journey> sorted(List<Journey> list) {
        ObservableList<Journey> items = FXCollections.observableArrayList();
        if (list != null) {
            items.addAll(list);
            items.sort(Comparator
                    .comparing(Journey::depTime)
                    .thenComparing(Journey::arrTime));
        }
        return items;
    }

    /**
     * Sélectionne dans la vue le voyage partant à ou juste après l’heure donnée.
     * Si aucun voyage ne correspond, sélectionne le dernier de la liste.
     *
     * @param listView composant ListView contenant les voyages
     * @param time     heure cible pour la sélection
     */
    private static void selectByTime(ListView<Journey> listView, LocalTime time) {
        if (time == null) return;
        var items = listView.getItems();
        if (items.isEmpty()) return;

        int idx = items.size() - 1;
        for (int i = 0; i < items.size(); i++) {
            LocalTime d = items.get(i).depTime().toLocalTime();
            if (!d.isBefore(time)) { idx = i; break; }
        }
        listView.getSelectionModel().select(idx);
        listView.scrollTo(idx);
    }

    /**
     * Cellule personnalisée affichant un résumé graphique d’un voyage.
     */
    private static class SummaryCell extends ListCell<Journey> {
        /** Icône du véhicule de transport. */
        private final ImageView icon = new ImageView();
        /** Libellé de la route et destination. */
        private final Text routeLabel = new Text();
        /** Étiquette de l’heure de départ. */
        private final Text departLabel = new Text();
        /** Panneau pour dessiner la chronologie du voyage. */
        private final Pane timelinePane;
        /** Ligne centrale de la chronologie. */
        private final javafx.scene.shape.Line timelineLine = new javafx.scene.shape.Line();
        /** Étiquette de l’heure d’arrivée. */
        private final Text arrivalLabel = new Text();
        /** Étiquette de la durée totale. */
        private final Text durationLabel = new Text();
        /** Conteneur principal pour tous les éléments graphiques. */
        private final BorderPane container = new BorderPane();

        /**
         * Initialise la cellule avec sa structure graphique.
         */
        SummaryCell() {
            // icône + route
            icon.setFitWidth(20);
            icon.setFitHeight(20);
            HBox topHBox = new HBox(4, icon, routeLabel);
            topHBox.getStyleClass().add("route");
            topHBox.setAlignment(Pos.CENTER_LEFT);

            // horaires/durée
            departLabel.getStyleClass().add("departure");
            HBox bottomHBox = new HBox(durationLabel);
            bottomHBox.getStyleClass().add("duration");
            bottomHBox.setAlignment(Pos.CENTER);

            // pane pour timeline avec redéfinition du layout
            timelinePane = new Pane() {
                {
                    setPrefSize(0, 0);
                    setManaged(true);
                    getChildren().add(timelineLine);
                }
                @Override
                protected void layoutChildren() {
                    super.layoutChildren();
                    double w = getWidth(), h = getHeight(), y = h * 0.5;
                    timelineLine.setStartX(5);
                    timelineLine.setStartY(y);
                    timelineLine.setEndX(w - 5);
                    timelineLine.setEndY(y);
                    for (Node n : getChildren()) {
                        if (n instanceof javafx.scene.shape.Circle c) {
                            double frac = (Double) c.getUserData();
                            c.setCenterX(5 + frac * (w - 10));
                            c.setCenterY(y);
                        }
                    }
                }
            };

            // assemblage du container
            container.getStyleClass().add("journey");
            container.setTop(topHBox);
            container.setLeft(departLabel);
            container.setCenter(timelinePane);
            container.setRight(arrivalLabel);
            container.setBottom(bottomHBox);
            BorderPane.setAlignment(departLabel, Pos.CENTER_LEFT);
            BorderPane.setAlignment(arrivalLabel, Pos.CENTER_RIGHT);

            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        /**
         * Met à jour le contenu de la cellule pour le voyage donné.
         *
         * @param j     le {@link Journey} à afficher (peut être null/empty)
         * @param empty true si la cellule est vide
         */
        @Override
        protected void updateItem(Journey j, boolean empty) {
            super.updateItem(j, empty);
            if (empty || j == null) {
                setGraphic(null);
                return;
            }

            // icône et route de la première section transport
            Optional<Journey.Leg.Transport> optT =
                    j.legs().stream()
                            .filter(l -> l instanceof Journey.Leg.Transport)
                            .map(l -> (Journey.Leg.Transport) l)
                            .findFirst();
            if (optT.isPresent()) {
                Journey.Leg.Transport firstT = optT.get();
                icon.setImage(VehicleIcons.iconFor(firstT.vehicle()));
                routeLabel.setText(FormatterFr.formatRouteDestination(firstT));
            } else {
                icon.setImage(null);
                routeLabel.setText("");
            }

            // horaires et durée
            departLabel.setText(FormatterFr.formatTime(j.depTime()));
            arrivalLabel.setText(FormatterFr.formatTime(j.arrTime()));
            durationLabel.setText(FormatterFr.formatDuration(j.duration()));

            // préparer la timeline
            timelinePane.getChildren().retainAll(timelineLine);

            LocalDateTime t0 = j.depTime();
            Duration total = j.duration();
            // départ et arrivée
            timelinePane.getChildren().add(makeCircle("dep-arr", 0.0));
            // transferts intermédiaires
            List<Journey.Leg> legs = j.legs();
            for (int i = 0; i < legs.size(); i++) {
                Journey.Leg leg = legs.get(i);
                if (leg instanceof Journey.Leg.Foot f) {
                    boolean mark = f.isTransfer()
                            || (i > 0 && legs.get(i - 1) instanceof Journey.Leg.Transport
                            && i < legs.size() - 1 && legs.get(i + 1) instanceof Journey.Leg.Transport);
                    if (mark) {
                        double frac = Duration.between(t0, f.depTime()).toMillis()
                                / (double) total.toMillis();
                        timelinePane.getChildren().add(makeCircle("transfer", frac));
                    }
                }
            }
            timelinePane.getChildren().add(makeCircle("dep-arr", 1.0));

            // cercles noirs au-dessus
            var children = timelinePane.getChildren();
            List<javafx.scene.shape.Circle> blacks = children.stream()
                    .filter(n -> n instanceof javafx.scene.shape.Circle c
                            && c.getStyleClass().contains("dep-arr"))
                    .map(n -> (javafx.scene.shape.Circle) n)
                    .toList();
            children.removeAll(blacks);
            children.addAll(blacks);

            setGraphic(container);
        }

        /**
         * Crée un cercle pour la timeline à la position fractionnaire donnée.
         *
         * @param style classe CSS du cercle ("dep-arr" ou "transfer")
         * @param frac  fraction de la largeur (0.0 à 1.0)
         * @return le cercle positionné (UserData=frac)
         */
        private javafx.scene.shape.Circle makeCircle(String style, double frac) {
            javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(3);
            c.getStyleClass().add(style);
            c.setUserData(frac);
            return c;
        }
    }
}
