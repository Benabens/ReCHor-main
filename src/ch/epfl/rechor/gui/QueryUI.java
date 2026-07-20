package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Interface de recherche de voyages : départ, arrivée, date et heure.
 *
 * Cette classe expose :
 * - rootNode : le nœud racine de l’interface
 * - depStopO, arrStopO : ObservableValue des arrêts choisis
 * - dateO, timeO : ObservableValue de la date et de l’heure
 *
 * Le bouton swap échange les deux champs sans rouvrir les popups.
 *
 * @param rootNode nœud principal de l’interface
 * @param depStopO observable du nom de l’arrêt de départ
 * @param arrStopO observable du nom de l’arrêt d’arrivée
 * @param dateO    observable de la date sélectionnée
 * @param timeO    observable de l’heure sélectionnée
 *
 * @author Benjamin (392901)
 * @author Jeremy  (397366)
 */
public record QueryUI(
        Node rootNode,
        ObservableValue<String> depStopO,
        ObservableValue<String> arrStopO,
        ObservableValue<LocalDate> dateO,
        ObservableValue<LocalTime> timeO
) {
    /**
     * Construit l’interface de recherche pour sélectionner
     * le départ, l’arrivée, la date et l’heure.
     *
     * @param index index des arrêts pour l’autocomplétion (non null)
     * @return une instance prête à l’emploi de QueryUI
     * @throws NullPointerException si {@code index} est null
     */
    public static QueryUI create(StopIndex index) {
        Objects.requireNonNull(index);

        // — StopFields —
        StopField dep = StopField.create(index);
        dep.field().setId("depStop");
        dep.field().setPromptText("Nom de l’arrêt de départ");

        StopField arr = StopField.create(index);
        arr.field().setId("arrStop");
        arr.field().setPromptText("Nom de l’arrêt d’arrivée");

        // — Bouton swap —
        Button swap = new Button("↔");
        swap.setOnAction(e -> {
            Node previousFocus = swap.getScene().getFocusOwner();
            String tmp = dep.field().getText();
            dep.setTo(arr.field().getText());
            arr.setTo(tmp);
            swap.requestFocus();
            Platform.runLater(() -> {
                if (previousFocus != null && previousFocus != swap) {
                    previousFocus.requestFocus();
                }
            });
        });
        KeyCombination swapKb = new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN);

        // — Ligne des arrêts —
        HBox stopsBox = new HBox(10,
                new Label("Départ\u202f:"), dep.field(),
                swap,
                new Label("Arrivée\u202f:"), arr.field()
        );
        stopsBox.setAlignment(Pos.CENTER_LEFT);

        // — Date et heure —
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setId("date");
        datePicker.setPromptText("Sélectionner une date");

        var timeFormatter = new TextFormatter<LocalTime>(lenientTimeConverter(), LocalTime.now());
        TextField timeField = new TextField();
        timeField.setId("time");
        timeField.setTextFormatter(timeFormatter);
        timeField.setPromptText("HH:mm");

        HBox dateTimeBox = new HBox(10,
                new Label("Date\u202f:"), datePicker,
                new Label("Heure\u202f:"), timeField
        );
        dateTimeBox.setAlignment(Pos.CENTER_LEFT);

        // — Container principal —
        // (Les feuilles de style sont chargées au niveau de la Scene par Main ;
        //  QueryUI n'ajoute donc pas query.css lui-même pour éviter un doublon.)
        VBox root = new VBox(12, stopsBox, dateTimeBox);
        root.setPadding(new Insets(12));

        // raccourci Ctrl+I et focus initial
        Platform.runLater(() -> {
            Scene sc = root.getScene();
            if (sc != null) {
                sc.getAccelerators().put(swapKb, swap::fire);

                // Un popup d'autocomplétion (Départ/Arrivée) encore ouvert ne doit pas
                // « voler » le premier clic destiné au DatePicker. Dès qu'un MOUSE_PRESSED
                // vise le DatePicker, on ferme proprement les deux popups StopField. Le
                // filtre s'exécute en phase de CAPTURE (avant que le DatePicker ne traite
                // le press) et NE consomme PAS l'événement : le press atteint quand même le
                // DatePicker, qui s'ouvre au même clic. On ne ferme que si la cible est bien
                // dans le DatePicker, pour ne pas fermer le popup d'un StopField qu'on vient
                // d'ouvrir en le cliquant.
                sc.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                    if (isWithin(e.getTarget(), datePicker)) {
                        dep.hidePopup();
                        arr.hidePopup();
                    }
                });
            }
            dep.field().requestFocus();
        });

        // — Ouverture du calendrier en UN seul clic, où qu'on clique dans le champ date —
        // Cause du bug « deux clics » : le nœud d'affichage du DatePicker est toujours un
        // TextField interne (FakeFocusTextField, NON mouse-transparent) qui CONSOMME le
        // MOUSE_PRESSED pour placer le caret ; ce press n'atteint donc jamais la logique
        // d'ouverture de ComboBoxBaseBehavior (seule la petite flèche l'ouvre). setEditable
        // (false) n'y changerait rien (le display node reste ce TextField). On force donc
        // l'ouverture via un FILTRE MOUSE_PRESSED sur l'éditeur : un filtre s'exécute en
        // phase de CAPTURE, donc AVANT que le TextField ne consomme l'événement. On utilise
        // MOUSE_PRESSED (vrai événement) et non MOUSE_CLICKED : ce dernier est synthétisé et
        // n'est émis que si press et release visent le même nœud sans drag — le moindre
        // micro-déplacement le perd, d'où l'intermittence de l'ancien setOnMouseClicked. On
        // NE consomme PAS : le press continue vers le TextField (caret + saisie clavier de
        // la date préservés). L'éditeur pouvant être recréé par le skin, on retire puis
        // (ré)installe le filtre — sans doublon — à chaque changement de skin.
        EventHandler<MouseEvent> openOnPress = e -> {
            if (!datePicker.isShowing()) {
                datePicker.show();
            }
        };
        Runnable installClickToOpen = () -> {
            TextField ed = datePicker.getEditor();
            if (ed != null) {
                ed.removeEventFilter(MouseEvent.MOUSE_PRESSED, openOnPress);
                ed.addEventFilter(MouseEvent.MOUSE_PRESSED, openOnPress);
            }
        };
        installClickToOpen.run();
        datePicker.skinProperty().addListener((o, oldSkin, newSkin) -> installClickToOpen.run());
        // NB : on a volontairement retiré l'avance-focus automatique vers le champ Heure
        // après le choix d'une date. Ce déplacement volait le focus pendant que le popup
        // se refermait et réintroduisait, dans plusieurs cas, le besoin d'un second clic.

        return new QueryUI(
                root,
                dep.stopO(),
                arr.stopO(),
                datePicker.valueProperty(),
                timeFormatter.valueProperty()
        );
    }

    /**
     * Indique si la cible d'un événement appartient au sous-arbre d'un nœud donné,
     * en remontant la chaîne des parents. Sert à ne réagir qu'aux clics qui visent
     * réellement le DatePicker (et non un autre contrôle).
     *
     * @param target   cible de l'événement (peut être null ou non-{@link Node})
     * @param ancestor nœud racine recherché
     * @return vrai si {@code target} est {@code ancestor} ou l'un de ses descendants
     */
    private static boolean isWithin(Object target, Node ancestor) {
        Node n = (target instanceof Node node) ? node : null;
        while (n != null) {
            if (n == ancestor) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    /**
     * Convertisseur d'heure tolérant à la saisie : accepte des entrées souples
     * (« 7 », « 7:5 », « 7h30 », « 07:00 ») et les normalise en « HH:mm » à la
     * validation (perte de focus / Entrée). Une saisie invalide (heure hors bornes
     * ou texte non numérique) fait revenir le champ à sa dernière valeur valide.
     *
     * @return un convertisseur {@link LocalTime} ⇄ {@link String}
     */
    private static StringConverter<LocalTime> lenientTimeConverter() {
        DateTimeFormatter display = DateTimeFormatter.ofPattern("HH:mm");
        return new StringConverter<>() {
            @Override
            public String toString(LocalTime time) {
                return time == null ? "" : display.format(time);
            }

            @Override
            public LocalTime fromString(String s) {
                if (s == null) {
                    return null;
                }
                String x = s.trim().replace('h', ':').replace('H', ':');
                if (x.isEmpty()) {
                    return null;
                }
                int colon = x.indexOf(':');
                int hours;
                int minutes;
                if (colon < 0) {
                    hours = Integer.parseInt(x);
                    minutes = 0;
                } else {
                    String hp = x.substring(0, colon).trim();
                    String mp = x.substring(colon + 1).trim();
                    hours = hp.isEmpty() ? 0 : Integer.parseInt(hp);
                    minutes = mp.isEmpty() ? 0 : Integer.parseInt(mp);
                }
                // LocalTime.of lève DateTimeException si l'heure est hors bornes ;
                // le TextFormatter rejette alors la saisie et restaure la dernière
                // valeur valide. Idem pour un NumberFormatException ci-dessus.
                return LocalTime.of(hours, minutes);
            }
        };
    }
}
