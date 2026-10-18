/**
 * <PRE>
 *
 * Copyright 1998-2026 <A href="http://bringardner.us/tony">Tony Bringardner</A>
 *
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       <A href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</A>
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  </PRE>
 *
 *
 *	@author Tony Bringardner
 */
package us.bringardner.fx.dialog;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import us.bringardner.fx.gradient.GradientLook;

/**
 * Chooses a font: a family (each shown in itself, with a filter), a style and a size, with a
 * sample. The JavaFX twin of swing-widgets' {@code FontDialog}; the font is a {@code java.awt.Font},
 * as swing-widgets' {@code Appearance} keeps it.
 */
public class FontDialog {

	static final List<String> STYLES = List.of("Plain", "Bold", "Italic", "Bold italic");

	private final Stage stage = new Stage();
	private final TextField filter = new TextField();
	private final javafx.collections.ObservableList<String> allFamilies = FXCollections.observableArrayList(Font.getFamilies());
	private final FilteredList<String> families = new FilteredList<>(allFamilies);
	private final ListView<String> familyList = new ListView<>(families);
	private final ChoiceBox<String> style = new ChoiceBox<>(FXCollections.observableArrayList(STYLES));
	private final Spinner<Integer> size = new Spinner<>(6, 72, 13);
	private final Label sample = new Label(SettingsDialog.SAMPLE_TEXT);
	private java.awt.Font chosen;
	private boolean accepted;
	private boolean updating;

	/**
	 * @param owner the window it belongs to, or null
	 * @param start the font to start with, or null for the standard one
	 */
	public FontDialog(Window owner, java.awt.Font start) {
		stage.setTitle("Font");
		stage.initModality(Modality.APPLICATION_MODAL);
		if( owner != null ) {
			stage.initOwner(owner);
		}
		build();
		Font standard = Font.getDefault();
		select(start != null ? start : new java.awt.Font(standard.getFamily(), java.awt.Font.PLAIN, (int) Math.round(standard.getSize())));
	}

	/**
	 * Shows the dialog and waits.
	 *
	 * @return the font chosen, or null if canceled
	 */
	public static java.awt.Font choose(Window owner, java.awt.Font start) {
		FontDialog d = new FontDialog(owner, start);
		d.stage.showAndWait();
		return d.accepted ? d.chosen : null;
	}

	/** The dialog's window. */
	public Stage getStage() {
		return stage;
	}

	/** The font as chosen now. */
	public java.awt.Font getChosen() {
		return chosen;
	}

	public boolean isAccepted() {
		return accepted;
	}

	/** Shows a font: its family selected, its style and size. */
	public void select(java.awt.Font f) {
		updating = true;
		filter.clear();
		if( !allFamilies.contains(f.getName()) ) {
			// a family not among those found (the standard "System", say): still offered
			allFamilies.add(0, f.getName());
		}
		familyList.getSelectionModel().select(f.getName());
		familyList.scrollTo(f.getName());
		style.getSelectionModel().select(f.getStyle() & 3);
		size.getValueFactory().setValue(f.getSize());
		updating = false;
		chosen = f;
		showSample();
	}

	public void ok() {
		accepted = true;
		stage.close();
	}

	public void cancel() {
		accepted = false;
		stage.close();
	}

	/** The family list, for tests. */
	ListView<String> familyList() {
		return familyList;
	}

	/** The style, for tests. */
	ChoiceBox<String> styleBox() {
		return style;
	}

	/** The size, for tests. */
	Spinner<Integer> sizeSpinner() {
		return size;
	}

	/** The filter, for tests. */
	TextField filterField() {
		return filter;
	}

	/** The sample, for tests. */
	Label sample() {
		return sample;
	}

	private void changed() {
		if( updating ) {
			return;
		}
		String family = familyList.getSelectionModel().getSelectedItem();
		if( family == null ) {
			family = chosen.getName();
		}
		int s = Math.max(0, style.getSelectionModel().getSelectedIndex());
		chosen = new java.awt.Font(family, s, size.getValue());
		showSample();
	}

	private void showSample() {
		sample.setFont(fx(chosen));
	}

	/** The JavaFX font for an AWT one. */
	static Font fx(java.awt.Font f) {
		return Font.font(f.getName(), f.isBold() ? FontWeight.BOLD : FontWeight.NORMAL,
				f.isItalic() ? FontPosture.ITALIC : FontPosture.REGULAR, f.getSize());
	}

	private void build() {
		filter.setPromptText("Filter");
		filter.textProperty().addListener((o, was, is)->{
			String selected = familyList.getSelectionModel().getSelectedItem();
			String f = is == null ? "" : is.trim().toLowerCase();
			families.setPredicate(n->f.isEmpty() || n.toLowerCase().contains(f));
			if( selected != null && families.contains(selected) ) {
				familyList.getSelectionModel().select(selected);
			}
		});
		familyList.setCellFactory(l->new ListCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty ? null : item);
				setFont(empty || item == null ? Font.getDefault() : Font.font(item, 13));
			}
		});
		familyList.setPrefSize(240, 280);
		familyList.getSelectionModel().selectedItemProperty().addListener((o, was, is)->changed());
		style.getSelectionModel().selectedIndexProperty().addListener((o, was, is)->changed());
		size.setEditable(true);
		size.setPrefWidth(80);
		size.valueProperty().addListener((o, was, is)->changed());

		VBox left = new VBox(6, filter, familyList);
		VBox.setVgrow(familyList, Priority.ALWAYS);

		HBox styleRow = new HBox(8, new Label("Style:"), style, new Label("Size:"), size);
		styleRow.setAlignment(Pos.CENTER_LEFT);
		sample.setWrapText(true);
		sample.setPrefWidth(300);
		sample.setMinHeight(120);
		sample.setAlignment(Pos.TOP_LEFT);
		sample.setPadding(new Insets(8, 10, 8, 10));
		sample.setStyle("-fx-background-color: -theme-field; -fx-text-fill: -theme-field-text;"
				+ " -fx-border-color: rgba(0,0,0,0.25);");
		VBox right = new VBox(10, styleRow, sample);

		Button cancelButton = new Button("Cancel");
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(e->cancel());
		Button okButton = new Button("OK");
		okButton.setDefaultButton(true);
		okButton.setOnAction(e->ok());
		HBox buttons = new HBox(8, cancelButton, okButton);
		buttons.setAlignment(Pos.CENTER_RIGHT);

		BorderPane content = new BorderPane();
		content.setPadding(new Insets(14, 16, 12, 16));
		content.setLeft(left);
		BorderPane.setMargin(left, new Insets(0, 14, 0, 0));
		content.setCenter(right);
		content.setBottom(buttons);
		BorderPane.setMargin(buttons, new Insets(12, 0, 0, 0));
		Scene scene = new Scene(content);
		scene.setOnKeyPressed(e->{
			if( e.getCode() == KeyCode.ESCAPE ) {
				cancel();
			}
		});
		GradientLook.apply(scene);
		stage.setScene(scene);
	}
}
