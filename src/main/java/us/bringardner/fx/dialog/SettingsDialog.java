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

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import us.bringardner.fx.gradient.GradientLook;
import us.bringardner.swing.gradient.Appearance;
import us.bringardner.swing.gradient.Theme;

/**
 * The appearance settings, for a JavaFX application: the JavaFX twin of swing-widgets'
 * {@code SettingsDialog}. The font, and a theme chosen from the built-in ones and the user's own;
 * New theme..., Edit theme... and Delete theme ({@link ThemeDesigner}); a preview. OK puts the choice
 * into effect in every open window with the gradient look ({@link GradientLook#refresh()}) and, given
 * a preferences node, saves it where swing-widgets' {@code Appearance} keeps it, so Swing and
 * JavaFX applications share it. Cancel changes nothing.
 * <p>
 * An application loads the saved settings at start-up with {@link #load(Preferences)}.
 */
public class SettingsDialog {

	static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog";

	private final Stage stage = new Stage();
	private final Preferences prefs;
	private final Appearance original;
	private Appearance chosen;
	private boolean accepted;

	private final Label fontSample = new Label(SAMPLE_TEXT);
	private final Label fontName = new Label();
	private final Button standardFontButton = new Button("Use the standard font");
	private final List<ThemeCard> cards = new ArrayList<>();
	private final FlowPane grid = new FlowPane(10, 10);
	private final Label themeName = new Label();
	private final Button editButton = new Button("Edit theme...");
	private final Button deleteButton = new Button("Delete theme");
	private final ThemePreview preview = new ThemePreview();
	/** The applications' own tabs (none: no tabs). */
	private final List<FxSettingsPage> pages = FxSettingsPages.create();
	private TabPane tabs;

	/**
	 * @param owner the window it belongs to, or null
	 * @param prefs where OK saves the choice (the node the application loads from at start-up), or
	 *        null to only put it into effect
	 */
	public SettingsDialog(Window owner, Preferences prefs) {
		this.prefs = prefs;
		this.original = current();
		this.chosen = original;
		stage.setTitle("Settings");
		stage.initModality(Modality.APPLICATION_MODAL);
		if( owner != null ) {
			stage.initOwner(owner);
		}
		// the applications' tabs may want more room (a list, say)
		stage.setResizable(!pages.isEmpty());
		build();
		show(chosen);
	}

	/**
	 * Shows the settings and waits.
	 *
	 * @param owner the window it belongs to, or null
	 * @param prefs where OK saves the choice, or null to only put it into effect
	 * @return true if the user pressed OK and changed something
	 */
	public static boolean showDialog(Window owner, Preferences prefs) {
		SettingsDialog d = new SettingsDialog(owner, prefs);
		d.stage.showAndWait();
		return d.isAccepted() && !d.getAppearance().equals(d.original);
	}

	/**
	 * The saved settings into effect, at start-up, before any window is shown: the theme becomes
	 * current and the font is set on the gradient look.
	 *
	 * @param prefs the application's preferences node, or null for the defaults
	 * @return the settings
	 */
	public static Appearance load(Preferences prefs) {
		Appearance a = Appearance.load(prefs);
		Theme.setCurrent(a.getTheme());
		GradientLook.setFont(a.getFont());
		return a;
	}

	/** What is in effect now: the current theme and the gradient look's font. */
	public static Appearance current() {
		return new Appearance(Theme.current(), GradientLook.getFont());
	}

	/** The dialog's window. */
	public Stage getStage() {
		return stage;
	}

	/** True once OK was pressed. */
	public boolean isAccepted() {
		return accepted;
	}

	/** What is chosen in the dialog now (in effect only after OK). */
	public Appearance getAppearance() {
		return chosen;
	}

	// ---------------------------------------------------------------- actions

	/** Chooses a theme. */
	public void selectTheme(Theme theme) {
		chosen = chosen.withTheme(theme);
		show(chosen);
	}

	/** Chooses a font, or null for the standard font. */
	public void selectFont(java.awt.Font font) {
		chosen = chosen.withFont(font);
		show(chosen);
	}

	/** Back to the gold theme and the standard font (in effect only after OK). */
	public void reset() {
		chosen = Appearance.defaults();
		show(chosen);
	}

	/**
	 * OK: keeps the applications' tabs' changes, puts the appearance into effect, saves it if there
	 * is a preferences node, and closes. A tab that can't keep its changes is shown, with why, and
	 * the dialog stays open.
	 */
	public void ok() {
		for(FxSettingsPage p : pages) {
			String problem = p.problem();
			if( problem != null ) {
				showPage(p);
				tell(Alert.AlertType.WARNING, p.getTitle(), problem);
				return;
			}
		}
		for(FxSettingsPage p : pages) {
			try {
				p.ok();
			} catch (Exception e) {
				showPage(p);
				tell(Alert.AlertType.ERROR, p.getTitle(), "Can't keep the changes: " + e.getMessage());
				return;
			}
		}
		accepted = true;
		stage.close();
		if( !chosen.equals(original) ) {
			if( chosen.getTheme().isDark() != Theme.current().isDark() ) {
				// macOS title bars dark or not, now rather than at the next start
				us.bringardner.swing.gradient.MacAppearance.follow(chosen.getTheme());
			}
			Theme.setCurrent(chosen.getTheme());
			GradientLook.setFont(chosen.getFont());
			GradientLook.refresh();
			if( prefs != null ) {
				chosen.save(prefs);
			}
		}
	}

	/** Cancel: closes without changing anything. */
	public void cancel() {
		for(FxSettingsPage p : pages) {
			p.cancel();
		}
		accepted = false;
		stage.close();
	}

	protected void actionChangeFont() {
		java.awt.Font f = FontDialog.choose(stage, chosen.getFont());
		if( f != null ) {
			selectFont(f);
		}
	}

	/** New theme...: the designer, starting from the theme chosen here; a saved one is chosen. */
	protected void actionDesign() {
		Theme t = ThemeDesigner.design(stage, chosen.getTheme());
		if( t != null ) {
			designed(t);
		}
	}

	/** Edit theme...: the designer, changing the chosen theme (one of the user's own) in place. */
	protected void actionEdit() {
		Theme t = chosen.getTheme();
		if( t.isUserTheme() ) {
			Theme changed = ThemeDesigner.edit(stage, t);
			if( changed != null ) {
				designed(changed);
			}
		}
	}

	/** A theme the designer saved: it gets a card and is chosen. */
	public void designed(Theme t) {
		fillGrid();
		selectTheme(t);
	}

	/** Delete theme: deletes the chosen theme, one of the user's own, once they say so. */
	protected void actionDelete() {
		Theme t = chosen.getTheme();
		if( !t.isUserTheme() ) {
			return;
		}
		Alert a = new Alert(Alert.AlertType.WARNING, "Delete your theme \"" + t.getName() + "\"?", ButtonType.CANCEL,
				ButtonType.OK);
		a.initOwner(stage);
		a.setTitle("Delete Theme");
		a.setHeaderText(null);
		GradientLook.apply(a);
		if( a.showAndWait().filter(b->b == ButtonType.OK).isPresent() ) {
			deleteTheme(t);
		}
	}

	/** Deletes one of the user's themes; the gold theme is chosen in its place. */
	public void deleteTheme(Theme t) {
		if( t.isUserTheme() && Theme.deleteUserTheme(t.getName()) ) {
			fillGrid();
			selectTheme(Theme.GOLD);
		}
	}

	// ---------------------------------------------------------------- for tests

	List<ThemeCard> cards() {
		return cards;
	}

	Button editButton() {
		return editButton;
	}

	Button deleteButton() {
		return deleteButton;
	}

	Button standardFontButton() {
		return standardFontButton;
	}

	String themeNameText() {
		return themeName.getText();
	}

	String fontNameText() {
		return fontName.getText();
	}

	ThemePreview preview() {
		return preview;
	}

	/** The applications' tabs in this dialog. */
	public List<FxSettingsPage> getPages() {
		return pages;
	}

	/** Shows a tab: one of {@link #getPages()}. */
	public void showPage(FxSettingsPage p) {
		int i = pages.indexOf(p);
		if( tabs != null && i >= 0 ) {
			tabs.getSelectionModel().select(i + 1);
		}
	}

	/** Shows a message; tests replace it, as there's no one to close it. */
	protected void tell(Alert.AlertType type, String title, String message) {
		Alert a = new Alert(type, message, ButtonType.OK);
		a.initOwner(stage);
		a.setTitle(title);
		a.setHeaderText(null);
		a.showAndWait();
	}

	// ---------------------------------------------------------------- the window's parts

	private void build() {
		VBox sections = new VBox();
		grid.setPrefWrapLength(5 * (ThemeCard.W + 16));
		fillGrid();

		Button designButton = new Button("New theme...");
		designButton.setTooltip(new Tooltip("Make a new theme, starting from the one chosen here"));
		designButton.setOnAction(e->actionDesign());
		editButton.setTooltip(new Tooltip("Change the chosen theme (only your own themes can be changed)"));
		editButton.setOnAction(e->actionEdit());
		deleteButton.setTooltip(new Tooltip("Delete the chosen theme (only your own themes can be deleted)"));
		deleteButton.setOnAction(e->actionDelete());
		HBox themeButtons = new HBox(8, designButton, editButton, deleteButton);
		themeButtons.setPadding(new Insets(8, 0, 0, 0));
		themeName.setPadding(new Insets(4, 0, 0, 0));

		fontSample.setWrapText(true);
		fontSample.setMaxWidth(Double.MAX_VALUE);
		fontSample.setPadding(new Insets(8, 10, 8, 10));
		fontName.setStyle("-fx-font-style: italic;");
		Button change = new Button("Change...");
		change.setOnAction(e->actionChangeFont());
		change.setMaxWidth(Double.MAX_VALUE);
		standardFontButton.setOnAction(e->selectFont(null));
		standardFontButton.setMaxWidth(Double.MAX_VALUE);
		VBox fontButtons = new VBox(4, change, standardFontButton);
		VBox fontLeft = new VBox(4, fontSample, fontName);
		HBox.setHgrow(fontLeft, Priority.ALWAYS);
		HBox fontRow = new HBox(10, fontLeft, fontButtons);

		sections.getChildren().addAll(ThemeDesigner.heading("Theme"), grid, themeButtons, themeName,
				gap(), ThemeDesigner.heading("Font"), fontRow, gap(), ThemeDesigner.heading("Preview"), preview);
		VBox.setMargin(grid, new Insets(6, 0, 0, 0));
		VBox.setMargin(fontRow, new Insets(6, 0, 0, 0));
		VBox.setMargin(preview, new Insets(6, 0, 0, 0));

		Button resetButton = new Button("Reset to defaults");
		resetButton.setOnAction(e->reset());
		Button cancelButton = new Button("Cancel");
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(e->cancel());
		Button okButton = new Button("OK");
		okButton.setDefaultButton(true);
		okButton.setOnAction(e->ok());
		Region spring = new Region();
		HBox.setHgrow(spring, Priority.ALWAYS);
		HBox buttons = new HBox(8, resetButton, spring, cancelButton, okButton);
		buttons.setAlignment(Pos.CENTER_LEFT);

		BorderPane content = new BorderPane(sections);
		if( !pages.isEmpty() ) {
			// Appearance, then the applications' tabs
			tabs = new TabPane();
			tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
			sections.setPadding(new Insets(10));
			tabs.getTabs().add(new Tab("Appearance", sections));
			for(FxSettingsPage p : pages) {
				tabs.getTabs().add(new Tab(p.getTitle(), p.getNode()));
			}
			// the appearance's defaults: shown with the Appearance tab only
			resetButton.visibleProperty().bind(tabs.getSelectionModel().selectedIndexProperty().isEqualTo(0));
			content.setCenter(tabs);
		}
		content.setPadding(new Insets(16, 18, 12, 18));
		content.setBottom(buttons);
		BorderPane.setMargin(buttons, new Insets(12, 0, 0, 0));
		Scene scene = new Scene(content);
		GradientLook.apply(scene);
		stage.setScene(scene);
	}

	private static Region gap() {
		Region r = new Region();
		r.setMinHeight(14);
		return r;
	}

	/** A card for each theme: the built-in ones, the user's, and a custom one if it is in effect. */
	private void fillGrid() {
		grid.getChildren().clear();
		cards.clear();
		List<Theme> themes = Theme.all();
		if( chosen.getTheme().isCustom() || original.getTheme().isCustom() ) {
			themes.add(chosen.getTheme().isCustom() ? chosen.getTheme() : original.getTheme());
		}
		for(Theme t : themes) {
			ThemeCard card = new ThemeCard(t, ()->selectTheme(t));
			cards.add(card);
			grid.getChildren().add(card);
		}
		if( stage.getScene() != null ) {
			stage.sizeToScene();
		}
	}

	/** Shows a choice in every part of the dialog. */
	private void show(Appearance a) {
		Theme t = a.getTheme();
		java.awt.Font f = a.getFont();
		// the sample is in the size chosen, small or large, so a change of size shows
		fontSample.setFont(f == null ? Font.getDefault() : FontDialog.fx(f));
		fontSample.setStyle("-fx-background-color: " + css(t.getField()) + "; -fx-text-fill: " + css(t.getFieldText())
				+ "; -fx-border-color: rgba(0,0,0,0.25);");
		fontName.setText(f == null ? "The standard font"
				: f.getName() + ", " + styleName(f) + ", " + f.getSize() + " pt");
		standardFontButton.setDisable(f == null);

		for(ThemeCard c : cards) {
			c.setSelected(c.theme.equals(t));
		}
		String about = t.getDescription() != null ? t.getDescription()
				: t.isCustom() ? "Made from one color"
				: t.isUserTheme() ? "One of your themes" : null;
		themeName.setText(t.getName() + (about == null ? "" : ": " + about));
		editButton.setDisable(!t.isUserTheme());
		deleteButton.setDisable(!t.isUserTheme());

		// the preview, in the chosen theme (not the one in effect)
		preview.show(t, f);
		if( stage.getScene() != null ) {
			stage.sizeToScene();
		}
	}

	private static String css(java.awt.Color c) {
		return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
	}

	static String styleName(java.awt.Font f) {
		if( f.isBold() && f.isItalic() ) {
			return "bold italic";
		}
		return f.isBold() ? "bold" : f.isItalic() ? "italic" : "plain";
	}
}
