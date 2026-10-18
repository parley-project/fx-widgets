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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import us.bringardner.fx.gradient.GradientLook;
import us.bringardner.swing.gradient.Appearance;
import us.bringardner.swing.gradient.Theme;
import us.bringardner.swing.gradient.ThemeExtension;
import us.bringardner.swing.gradient.ThemeExtensions;

/** The JavaFX settings dialog, theme designer and font dialog. */
public class TestFxSettings {

	private static boolean started;
	private File dir;
	private String oldDir;
	private Theme oldTheme;
	private java.awt.Font oldFont;
	private Preferences prefs;

	@BeforeAll
	public static void start() throws Exception {
		CountDownLatch up = new CountDownLatch(1);
		try {
			Platform.startup(up::countDown);
			started = up.await(10, TimeUnit.SECONDS);
			Platform.setImplicitExit(false);
		} catch (IllegalStateException running) {
			started = true;
		} catch (Throwable e) {
			started = false;
		}
	}

	@BeforeEach
	public void themesOfOurOwn() throws Exception {
		dir = Files.createTempDirectory("fx-themes").toFile();
		oldDir = System.getProperty(Theme.USER_THEMES_PROPERTY);
		System.setProperty(Theme.USER_THEMES_PROPERTY, dir.getPath());
		oldTheme = Theme.current();
		oldFont = GradientLook.getFont();
		prefs = Preferences.userRoot().node("us/bringardner/fx/dialog/test-" + System.nanoTime());
	}

	@AfterEach
	public void putBack() throws Exception {
		for(File f : dir.listFiles()) {
			f.delete();
		}
		dir.delete();
		if( oldDir == null ) {
			System.clearProperty(Theme.USER_THEMES_PROPERTY);
		} else {
			System.setProperty(Theme.USER_THEMES_PROPERTY, oldDir);
		}
		Theme.setCurrent(oldTheme);
		GradientLook.setFont(oldFont);
		prefs.removeNode();
		ThemeExtensions.unregister(TestExtension.ID);
		FxThemePages.unregister(TestExtension.ID);
	}

	private static <T> T fx(Callable<T> code) throws Exception {
		Assumptions.assumeTrue(started, "JavaFX can't start here");
		CompletableFuture<T> f = new CompletableFuture<>();
		Platform.runLater(()->{
			try {
				f.complete(code.call());
			} catch (Throwable e) {
				f.completeExceptionally(e);
			}
		});
		try {
			return f.get(30, TimeUnit.SECONDS);
		} catch (java.util.concurrent.ExecutionException e) {
			if( e.getCause() instanceof Exception ) {
				throw (Exception) e.getCause();
			}
			throw e;
		}
	}

	private static Theme mine(String name) throws Exception {
		return Theme.saveUserTheme(Theme.fromColor(new java.awt.Color(0x336699)).withName(name));
	}

	// ---------------------------------------------------------------- the settings dialog

	@Test
	public void choosingAThemeShowsItAndOkPutsItIntoEffect() throws Exception {
		Theme.setCurrent(Theme.GOLD);
		fx(()->{
			SettingsDialog d = new SettingsDialog(null, prefs);
			assertEquals(Theme.all().size(), d.cards().size());
			assertTrue(d.cards().get(0).isSelected());
			assertTrue(d.editButton().isDisabled(), "a built-in theme can't be edited");
			assertTrue(d.deleteButton().isDisabled());
			assertTrue(d.standardFontButton().isDisabled());
			assertEquals("The standard font", d.fontNameText());

			d.selectTheme(Theme.DARK);
			assertTrue(d.themeNameText().startsWith("Dark"));
			assertEquals(Theme.DARK, d.preview().getTheme());
			assertTrue(d.preview().getStyleClass().contains(GradientLook.DARK_CLASS));
			assertEquals(Theme.GOLD, Theme.current(), "nothing changes before OK");

			java.awt.Font f = new java.awt.Font("Serif", java.awt.Font.BOLD, 15);
			d.selectFont(f);
			assertEquals("Serif, bold, 15 pt", d.fontNameText());
			assertFalse(d.standardFontButton().isDisabled());

			// a window with the look follows
			Stage other = new Stage();
			Scene scene = new Scene(new StackPane(new Label("x")));
			GradientLook.apply(scene);
			other.setScene(scene);
			other.show();
			d.ok();
			assertTrue(d.isAccepted());
			assertEquals(Theme.DARK, Theme.current());
			assertEquals(f, GradientLook.getFont());
			String style = scene.getRoot().getStyle();
			assertTrue(style.contains("-fx-font-family: \"Serif\""), style);
			assertTrue(style.contains("-fx-font-size: 15px"), style);
			assertTrue(scene.getRoot().getStyleClass().contains(GradientLook.DARK_CLASS));
			other.close();
			return null;
		});
		Appearance saved = Appearance.load(prefs);
		assertEquals(Theme.DARK, saved.getTheme());
		assertEquals("Serif", saved.getFont().getFamily());
		assertEquals(15, saved.getFont().getSize());
		// and the start-up of the next run
		Theme.setCurrent(Theme.GOLD);
		GradientLook.setFont(null);
		Appearance a = SettingsDialog.load(prefs);
		assertEquals(Theme.DARK, Theme.current());
		assertEquals(a.getFont(), GradientLook.getFont());
	}

	@Test
	public void cancelAndResetChangeNothing() throws Exception {
		Theme.setCurrent(Theme.named("Ocean"));
		Theme before = Theme.current();
		fx(()->{
			SettingsDialog d = new SettingsDialog(null, prefs);
			d.selectTheme(Theme.LIGHT);
			d.cancel();
			assertFalse(d.isAccepted());
			assertEquals(before, Theme.current());

			SettingsDialog r = new SettingsDialog(null, prefs);
			r.selectTheme(Theme.LIGHT);
			r.selectFont(new java.awt.Font("Serif", java.awt.Font.PLAIN, 20));
			r.reset();
			assertEquals(Appearance.defaults(), r.getAppearance());
			r.selectFont(new java.awt.Font("Serif", java.awt.Font.ITALIC, 20));
			assertTrue(r.fontNameText().contains("italic"));
			r.selectFont(null);
			assertEquals("The standard font", r.fontNameText());
			return null;
		});
		assertNull(prefs.get(Appearance.THEME, null), "cancel saves nothing");
	}

	@Test
	public void theUsersThemesCanBeEditedAndDeleted() throws Exception {
		Theme t = mine("Harbor");
		fx(()->{
			SettingsDialog d = new SettingsDialog(null, null);
			assertEquals(Theme.all().size(), d.cards().size());
			d.selectTheme(t);
			assertFalse(d.editButton().isDisabled());
			assertFalse(d.deleteButton().isDisabled());
			assertTrue(d.themeNameText().contains("One of your themes"));

			// a theme the designer made gets a card and is chosen
			Theme made = Theme.saveUserTheme(Theme.fromColor(new java.awt.Color(0x993366)).withName("Berry"));
			d.designed(made);
			assertEquals(Theme.all().size(), d.cards().size());
			assertEquals(made, d.getAppearance().getTheme());

			d.deleteTheme(made);
			assertNull(Theme.named("Berry"));
			assertEquals(Theme.GOLD, d.getAppearance().getTheme());
			d.deleteTheme(Theme.GOLD);
			assertNotNull(Theme.named("Gold"), "a built-in theme can't be deleted");
			return null;
		});
	}

	@Test
	public void aThemeMadeFromOneColorHasACard() throws Exception {
		Theme custom = Theme.fromColor(new java.awt.Color(0x228844));
		Theme.setCurrent(custom);
		fx(()->{
			SettingsDialog d = new SettingsDialog(null, null);
			assertEquals(Theme.all().size() + 1, d.cards().size());
			assertTrue(d.themeNameText().contains("Made from one color"));
			return null;
		});
	}

	// ---------------------------------------------------------------- the designer

	@Test
	public void designsANewTheme() throws Exception {
		fx(()->{
			ThemeDesigner d = new ThemeDesigner(null, Theme.GOLD, false);
			assertFalse(d.isEditing());
			assertEquals("Gold 2", d.getTheme().getName());
			assertEquals(Theme.GOLD.getName(), ((Theme) d.startFromBox().getValue()).getName());
			assertEquals(6, d.checkTexts().size());
			assertTrue(d.checkTexts().get(0).contains("reads well"));

			d.setColor(2, new java.awt.Color(0xF2CE70));
			assertTrue(d.checkTexts().get(0).contains("hard to read"), "text the color of its background");
			assertEquals("#F2CE70", d.hexField(2).getText());

			// a typed color, and a bad one put back
			d.hexField(4).setText("#123456");
			d.readHex(4);
			assertEquals(new java.awt.Color(0x123456), d.getTheme().getFieldText());
			d.hexField(4).setText("not a color");
			d.readHex(4);
			assertEquals("#123456", d.hexField(4).getText());

			// starting from another theme takes its colors and a name from its name
			d.startFromBox().setValue(Theme.LIGHT);
			assertEquals("Light 2", d.getTheme().getName());
			assertTrue(d.getTheme().sameColors(Theme.LIGHT.withName("Light 2")));

			d.setDark(true);
			assertTrue(d.getTheme().isDark());

			d.setThemeName("");
			assertEquals("Give the theme a name", d.nameNoteText());
			assertTrue(d.saveButton().isDisabled());
			d.setThemeName("custom");
			assertTrue(d.nameNoteText().contains("kept for themes made from one color"));
			d.setThemeName("gold");
			assertTrue(d.nameNoteText().contains("already a theme called \"Gold\""));
			assertFalse(d.save());

			d.setThemeName("Sunrise");
			assertTrue(d.save());
			assertNotNull(d.getSaved());
			return null;
		});
		Theme saved = Theme.named("Sunrise");
		assertNotNull(saved);
		assertTrue(saved.isUserTheme());
		assertTrue(saved.isDark());
	}

	@Test
	public void startsFromAColor() throws Exception {
		fx(()->{
			ThemeDesigner d = new ThemeDesigner(null, Theme.current(), false);
			d.startFromColor(new java.awt.Color(0x884422));
			assertEquals(ThemeDesigner.FROM_A_COLOR, d.startFromBox().getValue());
			assertEquals("My theme 2", d.getTheme().getName());
			d.cancel();
			assertNull(d.getSaved());
			return null;
		});
	}

	@Test
	public void editsAndRenamesOneOfTheUsersThemes() throws Exception {
		Theme t = mine("Harbor");
		assertThrows(IllegalArgumentException.class, ()->fx(()->new ThemeDesigner(null, Theme.GOLD, true)));
		fx(()->{
			ThemeDesigner d = new ThemeDesigner(null, t, true);
			assertTrue(d.isEditing());
			assertEquals("Harbor", d.getTheme().getName());
			assertEquals(" ", d.nameNoteText(), "its own name is fine");
			d.setColor(0, new java.awt.Color(0xAABBCC));
			d.setThemeName("Harbour");
			assertTrue(d.save());
			return null;
		});
		assertNull(Theme.named("Harbor"), "renamed: the old one goes");
		assertEquals(new java.awt.Color(0xAABBCC), Theme.named("Harbour").getStart());
	}

	@Test
	public void suggestsAFreeName() throws Exception {
		mine("Ocean 2");
		assertEquals("Ocean 3", ThemeDesigner.suggestName("Ocean"));
		assertEquals("Ocean 3", ThemeDesigner.suggestName("Ocean 2"));
		assertEquals("My theme 2", ThemeDesigner.suggestName(Theme.CUSTOM));
	}

	/** An application's part of a theme, with a JavaFX page. */
	static class TestExtension implements ThemeExtension {
		static final String ID = "fx-widgets.test";

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public String getTitle() {
			return "Test page";
		}

		@Override
		public Map<String, String> defaults(Theme theme) {
			return Map.of("color", theme.isDark() ? "black" : "white");
		}

		@Override
		public Page createPage() {
			throw new UnsupportedOperationException("not in Swing");
		}
	}

	static class TestPage implements FxThemePage {
		final Label node = new Label();
		Map<String, String> values = new HashMap<>();
		Runnable listener;
		/** The theme the page was last told of. */
		Theme heard;

		@Override
		public void themeChanged(Theme theme) {
			heard = theme;
		}

		@Override
		public Node getNode() {
			return node;
		}

		@Override
		public void setValues(Map<String, String> v, Theme theme) {
			values = new HashMap<>(v);
			node.setText(v.get("color"));
		}

		@Override
		public Map<String, String> getValues() {
			return values;
		}

		@Override
		public void setChangeListener(Runnable listener) {
			this.listener = listener;
		}
	}

	@Test
	public void anApplicationsPageIsATab() throws Exception {
		ThemeExtensions.register(new TestExtension());
		FxThemePages.register(TestExtension.ID, TestPage::new);
		fx(()->{
			ThemeDesigner d = new ThemeDesigner(null, Theme.DARK, false);
			List<FxThemePage> pages = d.pages();
			assertEquals(1, pages.size());
			TestPage p = (TestPage) pages.get(0);
			assertEquals("black", p.node.getText(), "the defaults for a theme without its own");
			assertEquals(Theme.DARK.getStart(), p.heard.getStart(), "told of the theme");
			d.setColor(0, new java.awt.Color(0x123456));
			assertEquals(new java.awt.Color(0x123456), p.heard.getStart(), "and of a color changed");
			d.setColor(0, Theme.DARK.getStart());
			assertNotNull(p.listener);
			p.values.put("color", "red");
			p.listener.run();
			d.setThemeName("Red night");
			assertTrue(d.save());
			return null;
		});
		assertEquals("red", Theme.named("Red night").values(new TestExtension()).get("color"));
		fx(()->{
			// a new theme starts with those of the theme it is made from
			ThemeDesigner d = new ThemeDesigner(null, Theme.named("Red night"), false);
			assertEquals("red", ((TestPage) d.pages().get(0)).node.getText());
			return null;
		});
		FxThemePages.unregister(TestExtension.ID);
		assertNull(FxThemePages.create(TestExtension.ID));
	}

	// ---------------------------------------------------------------- the applications' tabs

	static class MinePage implements FxSettingsPage {
		final Label node = new Label("mine");
		String problem;
		Exception failure;
		int oks;
		int cancels;

		@Override
		public String getTitle() {
			return "Mine";
		}

		@Override
		public Node getNode() {
			return node;
		}

		@Override
		public String problem() {
			return problem;
		}

		@Override
		public void ok() throws Exception {
			if( failure != null ) {
				throw failure;
			}
			oks++;
		}

		@Override
		public void cancel() {
			cancels++;
		}
	}

	/** The dialog, with what it would have told the user. */
	static class TellingDialog extends SettingsDialog {
		final java.util.List<String> told = new java.util.ArrayList<>();

		TellingDialog() {
			super(null, null);
		}

		@Override
		protected void tell(javafx.scene.control.Alert.AlertType type, String title, String message) {
			told.add(title + ": " + message);
		}
	}

	@Test
	public void anApplicationsSettingsPageIsATab() throws Exception {
		try {
			assertNull(fx(()->new SettingsDialog(null, null).getStage().getScene().getRoot().lookup(".tab-pane")),
					"no pages: no tabs");
			FxSettingsPages.register("test.mine", MinePage::new);
			fx(()->{
				TellingDialog d = new TellingDialog();
				javafx.scene.control.TabPane tabs = (javafx.scene.control.TabPane) d.getStage().getScene().getRoot()
						.lookup(".tab-pane");
				assertNotNull(tabs);
				assertEquals("Appearance", tabs.getTabs().get(0).getText());
				assertEquals("Mine", tabs.getTabs().get(1).getText());
				MinePage p = (MinePage) d.getPages().get(0);

				p.problem = "not yet";
				d.ok();
				assertFalse(d.isAccepted());
				assertEquals(1, tabs.getSelectionModel().getSelectedIndex(), "shown on the page");
				assertEquals(List.of("Mine: not yet"), d.told);

				p.problem = null;
				p.failure = new java.io.IOException("disk full");
				d.ok();
				assertFalse(d.isAccepted());
				assertTrue(d.told.get(1).contains("disk full"));

				p.failure = null;
				d.ok();
				assertTrue(d.isAccepted());
				assertEquals(1, p.oks);

				TellingDialog c = new TellingDialog();
				c.cancel();
				assertEquals(1, ((MinePage) c.getPages().get(0)).cancels);
				return null;
			});
		} finally {
			FxSettingsPages.unregister("test.mine");
		}
	}

	// ---------------------------------------------------------------- the font dialog

	@Test
	public void choosesAFont() throws Exception {
		fx(()->{
			FontDialog d = new FontDialog(null, new java.awt.Font("Serif", java.awt.Font.PLAIN, 14));
			assertEquals("Serif", d.getChosen().getName());
			d.styleBox().getSelectionModel().select(3);
			assertTrue(d.getChosen().isBold() && d.getChosen().isItalic());
			d.sizeSpinner().getValueFactory().setValue(22);
			assertEquals(22, d.getChosen().getSize());
			assertEquals(22, d.sample().getFont().getSize(), 0.01);
			String other = d.familyList().getItems().stream().filter(f->!f.equals("Serif")).findFirst().get();
			d.familyList().getSelectionModel().select(other);
			assertEquals(other, d.getChosen().getName());

			// the filter keeps what matches
			d.filterField().setText("zzz-no-such-font");
			assertTrue(d.familyList().getItems().isEmpty());
			d.filterField().setText("");
			assertFalse(d.familyList().getItems().isEmpty());

			d.ok();
			assertTrue(d.isAccepted());

			FontDialog c = new FontDialog(null, null);
			assertNotNull(c.getChosen(), "the standard font to start with");
			assertTrue(c.familyList().getItems().contains(c.getChosen().getName()));
			c.cancel();
			assertFalse(c.isAccepted());
			return null;
		});
	}

	@Test
	public void fontStyles() throws Exception {
		assertEquals("", GradientLook.fontStyle(null));
		String s = GradientLook.fontStyle(new java.awt.Font("Some \"Font\"", java.awt.Font.ITALIC, 11));
		assertTrue(s.contains("-fx-font-family: \"Some Font\""), s);
		assertTrue(s.contains("-fx-font-style: italic"), s);
		assertTrue(s.contains("-fx-font-weight: normal"), s);
		assertNotEquals(GradientLook.colorStyle(Theme.GOLD), GradientLook.colorStyle(Theme.DARK));
		assertEquals("plain", SettingsDialog.styleName(new java.awt.Font("Serif", java.awt.Font.PLAIN, 3)));
		assertEquals("bold italic", SettingsDialog.styleName(new java.awt.Font("Serif", java.awt.Font.BOLD | java.awt.Font.ITALIC, 3)));
		// a font then none: the style is taken off again
		fx(()->{
			Scene scene = new Scene(new StackPane());
			scene.getRoot().setStyle("-app-own: 1;");
			GradientLook.setFont(new java.awt.Font("Serif", java.awt.Font.PLAIN, 18));
			GradientLook.apply(scene);
			assertTrue(scene.getRoot().getStyle().contains("-fx-font-size: 18px"));
			GradientLook.setFont(null);
			GradientLook.apply(scene);
			assertFalse(scene.getRoot().getStyle().contains("-fx-font"), scene.getRoot().getStyle());
			assertTrue(scene.getRoot().getStyle().contains("-app-own: 1;"));
			return null;
		});
	}
}
