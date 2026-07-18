package ch.epfl.rechor.gui;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.journey.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Interface détaillée pour la visualisation des informations d'itinéraire.
 * Propose un affichage structuré des étapes et des actions associées au voyage.
 *
 * @author Benabens (392901)
 * @author Jeremy (397366)
 *
 * @param rootNode nœud principal contenant l'interface complète (ScrollPane)
 */
public record DetailUI(Node rootNode) {

    private static final String DETAIL_CSS = "detail.css";
    private static final String NO_JOURNEY_ID = "no-journey";
    private static final String BUTTONS_ID = "buttons";
    private static final String DETAIL_ID = "detail";

    /**
     * Fabrique une interface détaillée pour un trajet observable.
     *
     * @param journeyObservable le voyage à observer et afficher (non null)
     * @return une interface configurée prête à l'emploi
     * @throws NullPointerException si {@code journeyObservable} est null
     */
    public static DetailUI create(ObservableValue<Journey> journeyObservable) {
        Objects.requireNonNull(journeyObservable);
        ScrollPane mainContainer = new ScrollPane();
        mainContainer.setId(DETAIL_ID);
        mainContainer.getStylesheets().add(DETAIL_CSS);

        VBox contentContainer = new VBox();
        StackPane layeredContent = new StackPane();

        // Message quand aucun voyage n'est sélectionné
        VBox emptyStateMessage = new VBox(new Text("Aucun voyage"));
        emptyStateMessage.setId(NO_JOURNEY_ID);
        layeredContent.getChildren().add(emptyStateMessage);

        // Configuration des couches pour l'affichage du voyage
        Pane connectionsLayer = new Pane();
        connectionsLayer.setId("annotations");
        connectionsLayer.setMouseTransparent(true);

        // Composant d'affichage du voyage avec le panneau d'annotations
        JourneyView journeyView = new JourneyView(connectionsLayer);

        // Assemblage de la vue
        StackPane visualElements = new StackPane(connectionsLayer, journeyView);

        HBox actionsBar = new HBox();
        actionsBar.setSpacing(10);
        actionsBar.setAlignment(Pos.CENTER_RIGHT);
        actionsBar.setPadding(new Insets(10));
        actionsBar.setId(BUTTONS_ID);

        contentContainer.getChildren().addAll(visualElements, actionsBar);
        layeredContent.getChildren().add(contentContainer);
        mainContainer.setContent(layeredContent);

        // Boutons d'action créés UNE SEULE FOIS ; ils opèrent sur le voyage courant
        // (référencé via un petit conteneur), au lieu d'être recréés à chaque sélection.
        Journey[] current = { null };
        Button mapBtn = new Button("Carte");
        mapBtn.setOnAction(e -> {
            if (current[0] != null) {
                openMapInBrowser(current[0]);
            }
        });
        Button calendarBtn = new Button("Calendrier");
        calendarBtn.setOnAction(e -> {
            if (current[0] != null) {
                exportJourneyToICalendar(current[0], journeyView);
            }
        });

        // Met à jour l'affichage. Ne reconstruit rien si le voyage est identique
        // (record → equals structurel) et réutilise les boutons existants.
        Consumer<Journey> update = newJourney -> {
            if (newJourney == null) {
                current[0] = null;
                emptyStateMessage.setVisible(true);
                journeyView.clearContent();
                actionsBar.getChildren().clear();
                return;
            }
            if (newJourney.equals(current[0])) {
                return;
            }
            current[0] = newJourney;
            emptyStateMessage.setVisible(false);
            journeyView.displayJourney(newJourney);
            if (actionsBar.getChildren().isEmpty()) {
                actionsBar.getChildren().setAll(mapBtn, calendarBtn);
            }
        };

        journeyObservable.addListener((obs, oldJourney, newJourney) -> update.accept(newJourney));
        update.accept(journeyObservable.getValue());

        return new DetailUI(mainContainer);
    }

    /**
     * Ouvre la carte du trajet dans le navigateur par défaut.
     */
    private static void openMapInBrowser(Journey journey) {
        try {
            String geoData = JourneyGeoJsonConverter.toGeoJson(journey).toString();
            URI mapUri = new URI(
                    "https",
                    "umap.osm.ch",
                    "/fr/map",
                    "data=" + geoData,
                    null
            );
            Desktop.getDesktop().browse(mapUri);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Exporte le trajet au format iCalendar via une boîte de dialogue.
     */
    private static void exportJourneyToICalendar(Journey journey, Node dialogParent) {
        FileChooser saveDialog = new FileChooser();
        saveDialog.setTitle("Exporter l'événement iCalendar");

        String defaultFilename = "voyage_" +
                journey.depTime()
                        .toLocalDate()
                        .format(DateTimeFormatter.ISO_DATE) +
                ".ics";
        saveDialog.setInitialFileName(defaultFilename);

        FileChooser.ExtensionFilter icsFilter =
                new FileChooser.ExtensionFilter("Fichiers iCalendar", "*.ics");
        saveDialog.getExtensionFilters().add(icsFilter);

        java.io.File selectedFile = saveDialog.showSaveDialog(dialogParent.getScene().getWindow());
        if (selectedFile != null) {
            Path targetPath = selectedFile.toPath();
            try {
                String icalContent = JourneyIcalConverter.toIcalendar(journey);
                Files.writeString(targetPath, icalContent);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Représente une connexion visuelle entre deux points dans l'interface.
     */
    private record ConnectionPoints(Circle start, Circle end) {}

    /**
     * Composant pour l'affichage visuel des étapes d'un itinéraire.
     */
    private static final class JourneyView extends GridPane {
        private final List<ConnectionPoints> connections = new ArrayList<>();
        private final Pane connectionsLayer;

        /**
         * Crée un nouveau composant d'affichage d'itinéraire.
         *
         * @param connectionsLayer panneau pour dessiner les connexions entre arrêts
         */
        public JourneyView(Pane connectionsLayer) {
            this.connectionsLayer = connectionsLayer;

            setHgap(5);
            setVgap(3);
            setId("legs");

            // Configuration des colonnes
            for (int colIdx = 0; colIdx < 4; ++colIdx) {
                getColumnConstraints().add(new ColumnConstraints());
            }

            // Ajouter un écouteur de changement de dimensions pour actualiser les connexions
            boundsInParentProperty().addListener((obs, oldBounds, newBounds) ->
                    updateVisualConnections());
        }

        /**
         * Efface le contenu graphique et réinitialise l'affichage.
         */
        public void clearContent() {
            getChildren().clear();
            connectionsLayer.getChildren().clear();
            connections.clear();
        }

        /**
         * Affiche un voyage en construisant sa représentation visuelle.
         *
         * @param journey le voyage à afficher
         */
        public void displayJourney(Journey journey) {
            clearContent();

            int rowPosition = 0;
            for (Journey.Leg segment : journey.legs()) {
                rowPosition = switch (segment) {
                    case Journey.Leg.Foot footSegment ->
                            renderFootSegment(footSegment, rowPosition);
                    case Journey.Leg.Transport transportSegment ->
                            renderTransportSegment(transportSegment, rowPosition);
                };
            }
        }

        /**
         * Affiche un segment de trajet à pied dans la grille.
         *
         * @param segment segment à afficher
         * @param startRow indice de la ligne où commencer l'affichage
         * @return indice de la ligne suivant ce segment
         */
        private int renderFootSegment(Journey.Leg.Foot segment, int startRow) {
            Text walkInfo = new Text(FormatterFr.formatLeg(segment));
            add(walkInfo, 2, startRow, 2, 1);
            return startRow + 1;
        }

        /**
         * Affiche un segment de transport en commun avec ses arrêts.
         *
         * @param segment segment à afficher
         * @param startRow indice de la ligne où commencer l'affichage
         * @return indice de la ligne suivant ce segment
         */
        private int renderTransportSegment(Journey.Leg.Transport segment, int startRow) {
            // Point et heure de départ
            addTimeElement(FormatterFr.formatTime(segment.depTime()), 0, startRow, "departure");
            Circle departPoint = createConnectionPoint();
            add(departPoint, 1, startRow);

            // Nom de l'arrêt de départ et quai
            add(new Text(segment.depStop().name()), 2, startRow);
            addTimeElement(FormatterFr.formatPlatformName(segment.depStop()), 3, startRow, "departure");

            // Icône du véhicule
            ImageView vehicleIcon = createVehicleIcon(segment.vehicle());
            add(vehicleIcon, 0, startRow + 1);

            // Information sur la ligne et destination
            add(new Text(FormatterFr.formatRouteDestination(segment)), 2, startRow + 1, 2, 1);

            // Traitement des arrêts intermédiaires
            int additionalRows = 0;
            if (!segment.intermediateStops().isEmpty()) {
                TitledPane stopsPanel = createIntermediateStopsPanel(segment);
                add(stopsPanel, 2, startRow + 2, 2, 1);
                GridPane.setRowSpan(vehicleIcon, 2);
                additionalRows = 1;
            }

            // Point et heure d'arrivée
            int arrivalRow = startRow + 2 + additionalRows;
            addTimeElement(FormatterFr.formatTime(segment.arrTime()), 0, arrivalRow, null);
            Circle arrivalPoint = createConnectionPoint();
            add(arrivalPoint, 1, arrivalRow);

            // Nom de l'arrêt d'arrivée et quai
            add(new Text(segment.arrStop().name()), 2, arrivalRow);
            add(new Text(FormatterFr.formatPlatformName(segment.arrStop())), 3, arrivalRow);

            // Enregistrement des points pour la connexion visuelle
            connections.add(new ConnectionPoints(departPoint, arrivalPoint));

            return arrivalRow + 1;
        }

        /**
         * Crée un point de connexion (cercle) pour les connexions visuelles.
         */
        private Circle createConnectionPoint() {
            return new Circle(3, Color.BLACK);
        }

        /**
         * Crée une icône pour le type de transport.
         */
        private ImageView createVehicleIcon(Vehicle vehicle) {
            ImageView icon = new ImageView(VehicleIcons.iconFor(vehicle));
            icon.setFitHeight(31);
            icon.setFitWidth(31);
            setValignment(icon, VPos.CENTER);
            return icon;
        }

        /**
         * Crée un panneau déroulant pour les arrêts intermédiaires.
         */
        private TitledPane createIntermediateStopsPanel(Journey.Leg.Transport segment) {
            GridPane stopsGrid = new GridPane();
            stopsGrid.setHgap(5);
            stopsGrid.setVgap(2);
            stopsGrid.getStyleClass().add("intermediate-stops");

            List<Journey.Leg.IntermediateStop> stops = segment.intermediateStops();
            int stopRow = 0;

            for (Journey.Leg.IntermediateStop stop : stops) {
                stopsGrid.add(new Text(FormatterFr.formatTime(stop.arrTime())), 0, stopRow);
                stopsGrid.add(new Text(FormatterFr.formatTime(stop.depTime())), 1, stopRow);
                stopsGrid.add(new Text(stop.stop().name()), 2, stopRow);
                ++stopRow;
            }

            String title = stopRow + " arrêt" + (stopRow > 1 ? "s" : "") +
                    ", " + FormatterFr.formatDuration(segment.duration());

            TitledPane panel = new TitledPane(title, stopsGrid);
            panel.setExpanded(false);

            // Recalcul des connexions lors de l'expansion/contraction
            panel.expandedProperty().addListener((obs, oldVal, newVal) -> {
                requestLayout();
                updateVisualConnections();
            });

            return panel;
        }

        /**
         * Ajoute un élément texte avec style optionnel.
         */
        private void addTimeElement(String text, int col, int row, String styleClass) {
            Text element = new Text(text);
            if (styleClass != null) {
                element.getStyleClass().add(styleClass);
            }
            add(element, col, row);
        }

        /**
         * Met à jour les connexions visuelles entre les points.
         * Cette méthode est appelée automatiquement lors des changements de géométrie.
         */
        private void updateVisualConnections() {
            // Effacer les connexions précédentes
            connectionsLayer.getChildren().clear();

            // Redessiner les nouvelles connexions
            for (ConnectionPoints connection : connections) {
                double startX = connection.start()
                        .getBoundsInParent()
                        .getCenterX();
                double startY = connection.start()
                        .getBoundsInParent()
                        .getCenterY();
                double endX = connection.end()
                        .getBoundsInParent()
                        .getCenterX();
                double endY = connection.end()
                        .getBoundsInParent()
                        .getCenterY();

                Line connectionLine = new Line(startX, startY, endX, endY);
                connectionLine.setStroke(Color.RED);
                connectionLine.setStrokeWidth(2);

                connectionsLayer.getChildren().add(connectionLine);
            }
        }
    }
}