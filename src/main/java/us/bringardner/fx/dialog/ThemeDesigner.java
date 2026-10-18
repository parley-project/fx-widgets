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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import us.bringardner.fx.gradient.GradientLook;
import us.bringardner.swing.gradient.Theme;
import us.bringardner.swing.gradient.ThemeExtension;
import us.bringardner.swing.gradient.ThemeExtensions;

/**
 * Makes a new theme from an existing one, or changes one of the user's own themes: the JavaFX twin
 * of swing-widgets' {@code ThemeDesigner}, saving to the same place, so a theme made in one
 * application is offered by all of them.
 * <p>
 * To design a new theme ({@link #design}): choose the theme to start from (or a color), change any of
 * its colors, watch a preview and the readability checks, and save it under a new name. To edit one
 * of the user's themes ({@link #edit}): change its colors, and its name if wanted, and save the
 * changes over it. The built-in themes can't be edited.
 * <p>
 * An application's own part of a theme ({@code ThemeExtension}) gets a tab when it has a JavaFX page
 * ({@link FxThemePages}); without one, its settings are those of the theme started from.
 */
public class ThemeDesigner {

	/** What each color is for, in the order of {@link Theme#KEYS}. */
	static final String[] ROLE_NAMES = {"Gradient, light end", "Gradient, dark end", "Text on the gradient",
			"Field background", "Field text", "Selected row", "Selected text", "Table header", "Header text",
			"Lines and arrows", "Button text"};

	/** The readability checks: [text, background, minimum ratio x10]. */
	static final int[][] CHECKS = {{2, 0, 45}, {2, 1, 30}, {4, 3, 45}, {6, 5, 30}, {8, 7, 30}, {10, 1, 30}};

	static final String[] CHECK_NAMES = {"Text on the light end", "Text on the dark end", "Field text",
			"Selected text", "Header text", "Button text"};

	/** The choice in "Start from" that makes the starting colors from one color the user picks. */
	static final String FROM_A_COLOR = "A color...";

	private final Stage stage = new Stage();
	private final ComboBox<Object> startFrom = new ComboBox<>();
	private final TextField name = new TextField();
	private final CheckBox dark = new CheckBox("Dark theme: light text on dark backgrounds");
	private final java.awt.Color[] colors = new java.awt.Color[Theme.KEYS.size()];
	private final ColorPicker[] pickers = new ColorPicker[colors.length];
	private final TextField[] hexes = new TextField[colors.length];
	private final Label[] checks = new Label[CHECKS.length];
	private final ThemePreview preview = new ThemePreview();
	private final Label nameNote = new Label(" ");
	private final Button saveButton = new Button();
	private boolean updating;
	private Theme saved;
	/** The user's theme being edited, or null when designing a new one. */
	private final Theme editing;
	/** The extensions that have a page here, and their pages. */
	private final List<ThemeExtension> extensions = new ArrayList<>();
	private final List<FxThemePage> pages = new ArrayList<>();
	/** The applications' settings of the theme started from, kept as they are unless a page changes them. */
	private Map<String, String> startExtras = Map.of();

	/**
	 * @param owner the window it belongs to, or null
	 * @param theme the theme to start from, or (edit) the user's theme to change
	 * @param edit true to change the theme itself, which must be one of the user's own
	 */
	public ThemeDesigner(Window owner, Theme theme, boolean edit) {
		if( edit && !theme.isUserTheme() ) {
			throw new IllegalArgumentException("\"" + theme.getName() + "\" isn't one of your themes");
		}
		editing = edit ? theme : null;
		stage.setTitle(edit ? "Edit Theme" : "New Theme");
		stage.initModality(Modality.APPLICATION_MODAL);
		if( owner != null ) {
			stage.initOwner(owner);
		}
		stage.setResizable(false);
		for(ThemeExtension x : ThemeExtensions.all()) {
			FxThemePage page = FxThemePages.create(x.getId());
			if( page != null ) {
				extensions.add(x);
				pages.add(page);
			}
		}
		build();
		startFrom(theme);
		if( edit ) {
			setThemeName(theme.getName());
		}
	}

	/**
	 * Shows the designer making a new theme, and waits.
	 *
	 * @param owner the window it belongs to, or null
	 * @param start the theme to start from
	 * @return the new theme, saved, or null if canceled
	 */
	public static Theme design(Window owner, Theme start) {
		ThemeDesigner d = new ThemeDesigner(owner, start, false);
		d.stage.showAndWait();
		return d.getSaved();
	}

	/**
	 * Shows the designer changing one of the user's themes, and waits.
	 *
	 * @param owner the window it belongs to, or null
	 * @param theme the user's theme
	 * @return the theme as changed and saved, or null if canceled
	 */
	public static Theme edit(Window owner, Theme theme) {
		ThemeDesigner d = new ThemeDesigner(owner, theme, true);
		d.stage.showAndWait();
		return d.getSaved();
	}

	/** The designer's window. */
	public Stage getStage() {
		return stage;
	}

	/** True when changing one of the user's themes rather than designing a new one. */
	public boolean isEditing() {
		return editing != null;
	}

	/** The theme saved, or null if not (yet). */
	public Theme getSaved() {
		return saved;
	}

	/** The theme as the designer has it now (unsaved). */
	public Theme getTheme() {
		String n = name.getText() == null ? "" : name.getText().trim();
		Theme t = Theme.of(n.isEmpty() ? "Untitled" : n, colors.clone(), dark.isSelected()).withExtras(startExtras);
		for(int i = 0; i < pages.size(); i++) {
			Map<String, String> v = pages.get(i).getValues();
			if( v != null ) {
				t = t.withValues(extensions.get(i), v);
			}
		}
		return editing != null && editing.getDescription() != null ? t.withDescription(editing.getDescription()) : t;
	}

	// ---------------------------------------------------------------- actions

	/** Starts from a theme: its colors, its applications' settings, and a new name made from its name. */
	public void startFrom(Theme t) {
		updating = true;
		System.arraycopy(t.colors(), 0, colors, 0, colors.length);
		dark.setSelected(t.isDark());
		startExtras = t.getExtras();
		for(int i = 0; i < pages.size(); i++) {
			pages.get(i).setValues(t.values(extensions.get(i)), t);
		}
		if( editing == null ) {
			selectInStartFrom(t);
		}
		String n = name.getText() == null ? "" : name.getText();
		if( editing == null && (n.trim().isEmpty() || isSuggested(n)) ) {
			name.setText(suggestName(t.getName()));
		}
		updating = false;
		refresh();
	}

	/** Starts from a theme made from one color. */
	public void startFromColor(java.awt.Color c) {
		startFrom(Theme.fromColor(c));
	}

	/** Sets one color (by its index in {@link Theme#KEYS}). */
	public void setColor(int role, java.awt.Color c) {
		colors[role] = c;
		refresh();
	}

	/** Sets the theme's name. */
	public void setThemeName(String n) {
		name.setText(n);
		refresh();
	}

	public void setDark(boolean b) {
		dark.setSelected(b);
		refresh();
	}

	/**
	 * Saves the theme, as a new one of the user's or over the one being edited, and closes; or
	 * explains why it can't.
	 *
	 * @return true if saved
	 */
	public boolean save() {
		String problem = nameProblem();
		if( problem != null ) {
			nameNote.setText(problem);
			return false;
		}
		try {
			saved = Theme.saveUserTheme(getTheme());
			if( editing != null && !editing.getName().trim().equalsIgnoreCase(saved.getName()) ) {
				// renamed: the old one goes
				Theme.deleteUserTheme(editing.getName());
			}
		} catch (IOException | RuntimeException e) {
			saved = null;
			nameNote.setText("Can't save it: " + e.getMessage());
			return false;
		}
		stage.close();
		return true;
	}

	public void cancel() {
		saved = null;
		stage.close();
	}

	/** Why the name can't be used, or null if it can. */
	String nameProblem() {
		String n = name.getText() == null ? "" : name.getText().trim();
		if( n.isEmpty() ) {
			return "Give the theme a name";
		}
		if( Theme.CUSTOM.equalsIgnoreCase(n) ) {
			return "\"" + Theme.CUSTOM + "\" is kept for themes made from one color";
		}
		if( editing != null && editing.getName().trim().equalsIgnoreCase(n) ) {
			return null;
		}
		if( Theme.named(n) != null ) {
			return "There is already a theme called \"" + Theme.named(n).getName() + "\"";
		}
		return null;
	}

	/** A name for a theme started from another: "Ocean 2", "Ocean 3", ... the first that's free. */
	static String suggestName(String from) {
		String base = from.replaceAll("\\s+\\d+$", "");
		if( Theme.CUSTOM.equals(base) ) {
			base = "My theme";
		}
		for(int i = 2; ; i++) {
			String n = base + " " + i;
			if( Theme.named(n) == null ) {
				return n;
			}
		}
	}

	private static boolean isSuggested(String n) {
		return n.matches(".*\\s\\d+$");
	}

	// ---------------------------------------------------------------- for tests

	ComboBox<Object> startFromBox() {
		return startFrom;
	}

	TextField hexField(int role) {
		return hexes[role];
	}

	ColorPicker picker(int role) {
		return pickers[role];
	}

	Button saveButton() {
		return saveButton;
	}

	String nameNoteText() {
		return nameNote.getText();
	}

	ThemePreview preview() {
		return preview;
	}

	List<FxThemePage> pages() {
		return pages;
	}

	/** The checks' texts. */
	List<String> checkTexts() {
		List<String> ret = new ArrayList<>();
		for(Label l : checks) {
			ret.add(l.getText());
		}
		return ret;
	}

	// ---------------------------------------------------------------- the window's parts

	private void build() {
		VBox left = new VBox(8);
		if( editing == null ) {
			left.getChildren().add(row("Start from:", startFromBox(true)));
		}
		left.getChildren().add(row("Name:", name));
		nameNote.setStyle("-fx-font-style: italic;");
		left.getChildren().add(nameNote);
		dark.setOnAction(e->refresh());
		left.getChildren().addAll(dark, colorGrid());
		name.textProperty().addListener((o, was, is)->refresh());

		Node leftSide = left;
		if( !pages.isEmpty() ) {
			// a tab for the look's colors, and one for each application's page
			TabPane tabs = new TabPane();
			tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
			left.setPadding(new Insets(8));
			tabs.getTabs().add(new Tab("Colors", left));
			for(int i = 0; i < pages.size(); i++) {
				FxThemePage page = pages.get(i);
				page.setChangeListener(this::refresh);
				Node node = page.getNode();
				if( node instanceof javafx.scene.layout.Region ) {
					((javafx.scene.layout.Region) node).setPadding(new Insets(8));
				}
				tabs.getTabs().add(new Tab(extensions.get(i).getTitle(), node));
			}
			leftSide = tabs;
		}

		VBox right = new VBox(6);
		right.getChildren().addAll(heading("Preview"), preview, heading("Readability"));
		GridPane checkGrid = new GridPane();
		checkGrid.setHgap(12);
		checkGrid.setVgap(2);
		for(int i = 0; i < checks.length; i++) {
			checks[i] = new Label();
			checkGrid.add(new Label(CHECK_NAMES[i]), 0, i);
			checkGrid.add(checks[i], 1, i);
		}
		Label note = new Label("The contrast between text and what is behind it: 4.5 or more reads easily; "
				+ "3 is the least for large or bold text and selected rows.");
		note.setWrapText(true);
		note.setMaxWidth(360);
		note.setStyle("-fx-font-size: 0.9em;");
		right.getChildren().addAll(checkGrid, note);
		VBox.setMargin(right.getChildren().get(2), new Insets(8, 0, 0, 0));

		Button cancelButton = new Button("Cancel");
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(e->cancel());
		saveButton.setText(editing == null ? "Save as new theme" : "Save changes");
		saveButton.setDefaultButton(true);
		saveButton.setOnAction(e->save());
		HBox buttons = new HBox(8, cancelButton, saveButton);
		buttons.setAlignment(Pos.CENTER_RIGHT);

		BorderPane content = new BorderPane();
		content.setPadding(new Insets(16, 18, 12, 18));
		content.setLeft(leftSide);
		BorderPane.setMargin(leftSide, new Insets(0, 16, 0, 0));
		content.setCenter(right);
		content.setBottom(buttons);
		BorderPane.setMargin(buttons, new Insets(12, 0, 0, 0));
		Scene scene = new Scene(content);
		GradientLook.apply(scene);
		stage.setScene(scene);
	}

	private Node startFromBox(boolean withColor) {
		startFrom.getItems().addAll(Theme.all());
		if( withColor ) {
			startFrom.getItems().add(FROM_A_COLOR);
		}
		startFrom.setCellFactory(l->new NameCell());
		startFrom.setButtonCell(new NameCell());
		startFrom.setMaxWidth(Double.MAX_VALUE);
		startFrom.valueProperty().addListener((o, was, is)->{
			if( updating ) {
				return;
			}
			if( is instanceof Theme ) {
				startFrom((Theme) is);
			} else if( FROM_A_COLOR.equals(is) ) {
				java.awt.Color c = chooseColor("Start from a color", colors[1]);
				if( c != null ) {
					startFromColor(c);
				} else {
					// back to what it was
					updating = true;
					startFrom.setValue(was);
					updating = false;
				}
			}
		});
		return startFrom;
	}

	/** A theme's name in the start-from list. */
	private static class NameCell extends ListCell<Object> {
		@Override
		protected void updateItem(Object item, boolean empty) {
			super.updateItem(item, empty);
			setText(empty || item == null ? null : item instanceof Theme ? ((Theme) item).getName() : item.toString());
		}
	}

	/** A color from a small dialog with a color picker, or null if canceled. */
	java.awt.Color chooseColor(String title, java.awt.Color start) {
		Dialog<ButtonType> d = new Dialog<>();
		d.initOwner(stage);
		d.setTitle(title);
		ColorPicker p = new ColorPicker(Colors.fx(start));
		d.getDialogPane().setContent(new VBox(8, new Label("The color:"), p));
		d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
		GradientLook.apply(d);
		return d.showAndWait().filter(b->b == ButtonType.OK).map(b->Colors.awt(p.getValue())).orElse(null);
	}

	private void selectInStartFrom(Theme t) {
		for(Object o : startFrom.getItems()) {
			if( o instanceof Theme && ((Theme) o).getName().equals(t.getName()) && ((Theme) o).sameColors(t) ) {
				startFrom.setValue(o);
				return;
			}
		}
		startFrom.setValue(FROM_A_COLOR);
	}

	private Node colorGrid() {
		GridPane grid = new GridPane();
		grid.setHgap(8);
		grid.setVgap(4);
		for(int i = 0; i < colors.length; i++) {
			final int role = i;
			grid.add(new Label(ROLE_NAMES[i]), 0, i);
			pickers[i] = new ColorPicker();
			pickers[i].setTooltip(new Tooltip("Choose the " + ROLE_NAMES[i].toLowerCase()));
			pickers[i].setStyle("-fx-color-label-visible: false;");
			pickers[i].setOnAction(e->{
				if( !updating ) {
					setColor(role, Colors.awt(pickers[role].getValue()));
				}
			});
			grid.add(pickers[i], 1, i);
			hexes[i] = new TextField();
			hexes[i].setPrefColumnCount(7);
			hexes[i].setTooltip(new Tooltip("#RRGGBB"));
			hexes[i].setOnAction(e->readHex(role));
			hexes[i].focusedProperty().addListener((o, was, is)->{
				if( !is ) {
					readHex(role);
				}
			});
			grid.add(hexes[i], 2, i);
		}
		return grid;
	}

	/** Takes a color typed in a hex field; a bad one is put back as it was. */
	void readHex(int role) {
		try {
			java.awt.Color c = java.awt.Color.decode(hexes[role].getText().trim());
			if( !c.equals(colors[role]) ) {
				setColor(role, c);
			}
		} catch (NumberFormatException e) {
			hexes[role].setText(Theme.hex(colors[role]));
		}
	}

	/** Shows the colors, the preview, the checks and whether the name can be saved. */
	private void refresh() {
		if( updating ) {
			return;
		}
		updating = true;
		for(int i = 0; i < colors.length; i++) {
			hexes[i].setText(Theme.hex(colors[i]));
			pickers[i].setValue(Colors.fx(colors[i]));
		}
		updating = false;
		Theme t = getTheme();
		preview.show(t, null);
		for(FxThemePage p : pages) {
			p.themeChanged(t);
		}
		for(int i = 0; i < CHECKS.length; i++) {
			double ratio = Theme.contrast(colors[CHECKS[i][0]], colors[CHECKS[i][1]]);
			boolean ok = ratio * 10 >= CHECKS[i][2];
			checks[i].setText(String.format("%.1f  %s", ratio, ok ? "✓ reads well" : "⚠ hard to read"));
			checks[i].setStyle(ok ? "" : "-fx-text-fill: #B00020; -fx-font-weight: bold;");
		}
		String problem = nameProblem();
		nameNote.setText(problem == null ? " " : problem);
		saveButton.setDisable(problem != null);
	}

	private static Node row(String label, Node c) {
		Label l = new Label(label);
		l.setMinWidth(80);
		HBox box = new HBox(8, l, c);
		box.setAlignment(Pos.CENTER_LEFT);
		if( c instanceof javafx.scene.layout.Region ) {
			HBox.setHgrow(c, javafx.scene.layout.Priority.ALWAYS);
		}
		return box;
	}

	static Label heading(String text) {
		Label l = new Label(text);
		l.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");
		return l;
	}
}
