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
package us.bringardner.fx.gradient;

import java.net.URL;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.stage.Window;
import us.bringardner.swing.gradient.Theme;

/**
 * The gradient look for JavaFX: the twin of swing-widgets' GradientLook, and what
 * parley-files-viewer looks like. A diagonal gradient from the light color to the dark one behind
 * everything, see-through trees, tables and lists, the dark color for grid lines, headers and
 * selections, and rounded gradient buttons.
 * <p>
 * The colors are swing-widgets' current {@link Theme}'s, so one change there reaches the Swing and
 * JavaFX parts of an application alike; a dark theme also puts the {@value #DARK_CLASS} style class
 * on the root, for an application's own styles. Dialogs and popups are windows of their own, so apply the
 * look to each of them too.
 */
public final class GradientLook {

	private static boolean macAppearanceSet;

	/** The stylesheet's name, in this package. */
	public static final String STYLESHEET = "gradient-look.css";

	private GradientLook() {
	}

	/** The stylesheet's URL, for {@code getStylesheets().add(...)}. */
	public static String stylesheet() {
		URL url = GradientLook.class.getResource(STYLESHEET);
		if( url == null ) {
			throw new IllegalStateException(STYLESHEET + " is missing from the jar");
		}
		return url.toExternalForm();
	}

	/**
	 * Gives a scene the gradient look: adds the stylesheet (once) and sets its colors from
	 * the current {@link Theme}.
	 *
	 * @param scene the scene
	 * @return the scene
	 */
	public static Scene apply(Scene scene) {
		if( !macAppearanceSet ) {
			// the first window: macOS title bars dark for a dark theme (JavaFX has started by now)
			macAppearanceSet = true;
			us.bringardner.swing.gradient.MacAppearance.follow(Theme.current());
		}
		String css = stylesheet();
		if( !scene.getStylesheets().contains(css) ) {
			scene.getStylesheets().add(css);
		}
		if( scene.getRoot() != null ) {
			colors(scene.getRoot());
		}
		scene.rootProperty().addListener((o, was, is)->{
			if( is != null ) {
				colors(is);
			}
		});
		return scene;
	}

	/**
	 * Gives a dialog the gradient look. A dialog is its own window, so it doesn't get the look from
	 * the window that owns it.
	 *
	 * @param dialog the dialog
	 * @return the dialog
	 */
	public static <T extends Dialog<?>> T apply(T dialog) {
		Parent pane = dialog.getDialogPane();
		String css = stylesheet();
		if( !pane.getStylesheets().contains(css) ) {
			pane.getStylesheets().add(css);
		}
		colors(pane);
		return dialog;
	}

	/**
	 * True if a window shows the gradient look: its scene, or its scene's root (as a dialog's
	 * pane), has the stylesheet.
	 *
	 * @param window the window, or null
	 * @return true if it has the look
	 */
	public static boolean isApplied(Window window) {
		if( window == null || window.getScene() == null ) {
			return false;
		}
		String css = stylesheet();
		Scene scene = window.getScene();
		return scene.getStylesheets().contains(css)
				|| (scene.getRoot() != null && scene.getRoot().getStylesheets().contains(css));
	}

	/**
	 * For a library's dialogs: gives a dialog the gradient look if the window that owns it has it,
	 * and leaves it alone otherwise. Call it after {@code initOwner}.
	 *
	 * @param dialog the dialog
	 * @return the dialog
	 */
	public static <T extends Dialog<?>> T follow(T dialog) {
		return isApplied(dialog.getOwner()) ? apply(dialog) : dialog;
	}

	/** The CSS that sets the current theme's colors, for a node's style. */
	public static String colorStyle() {
		return colorStyle(Theme.current());
	}

	/**
	 * The CSS that sets a theme's colors, for a node's style: a node with the {@code root} style class
	 * and this style shows the theme, whatever theme is in effect (a preview, say).
	 *
	 * @param t the theme
	 * @return the style
	 */
	public static String colorStyle(Theme t) {
		return "-gradient-start: " + css(t.getStart()) + "; -gradient-end: " + css(t.getEnd())
				+ "; -theme-text: " + css(t.getText()) + "; -theme-field: " + css(t.getField())
				+ "; -theme-field-text: " + css(t.getFieldText()) + "; -theme-selection: " + css(t.getSelection())
				+ "; -theme-selection-text: " + css(t.getSelectionText()) + "; -theme-header: " + css(t.getHeader())
				+ "; -theme-header-text: " + css(t.getHeaderText()) + "; -theme-accent: " + css(t.getAccent())
				+ "; -theme-button-text: " + css(t.getButtonText()) + ";";
	}

	/** The style class on the root of a scene or dialog while a dark theme is in effect. */
	public static final String DARK_CLASS = "theme-dark";

	private static volatile java.awt.Font font;

	/**
	 * Sets the font of everything with the look (swing-widgets' {@code Appearance} font), or null for
	 * JavaFX's own. It is used from the next {@link #apply} or {@link #refresh()}.
	 *
	 * @param f the font, or null
	 */
	public static void setFont(java.awt.Font f) {
		font = f;
	}

	/** The font set with {@link #setFont}, or null for JavaFX's own. */
	public static java.awt.Font getFont() {
		return font;
	}

	/**
	 * The CSS for a font, for a node's style: its family, size (in pixels, as Swing's points are on
	 * the screen), weight and posture; empty for null.
	 *
	 * @param f the font, or null
	 * @return the style
	 */
	public static String fontStyle(java.awt.Font f) {
		if( f == null ) {
			return "";
		}
		return "-fx-font-family: \"" + f.getName().replace("\"", "") + "\"; -fx-font-size: " + f.getSize()
				+ "px; -fx-font-weight: " + (f.isBold() ? "bold" : "normal") + "; -fx-font-style: "
				+ (f.isItalic() ? "italic" : "normal") + ";";
	}

	/**
	 * Puts the current theme and font on every open window that has the look, after a change
	 * (a settings dialog's OK, say).
	 */
	public static void refresh() {
		for(Window w : new java.util.ArrayList<>(Window.getWindows())) {
			if( isApplied(w) ) {
				colors(w.getScene().getRoot());
			}
		}
	}

	private static void colors(Parent root) {
		String style = root.getStyle() == null ? "" : root.getStyle();
		// replace what an earlier call set, keep the rest
		style = style.replaceAll("-(gradient|theme)-[a-z-]+:\\s*#[0-9A-Fa-f]{6};\\s*", "")
				.replaceAll("-fx-font-(family|size|weight|style):[^;]*;\\s*", "").trim();
		root.setStyle((colorStyle() + " " + fontStyle(font) + " " + style).replaceAll("\\s+$", "").replaceAll("  +", " ").trim());
		root.getStyleClass().remove(DARK_CLASS);
		if( Theme.current().isDark() ) {
			root.getStyleClass().add(DARK_CLASS);
		}
	}

	static String css(java.awt.Color c) {
		return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
	}
}
