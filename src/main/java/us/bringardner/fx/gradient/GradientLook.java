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
		Theme t = Theme.current();
		return "-gradient-start: " + css(t.getStart()) + "; -gradient-end: " + css(t.getEnd())
				+ "; -theme-text: " + css(t.getText()) + "; -theme-field: " + css(t.getField())
				+ "; -theme-field-text: " + css(t.getFieldText()) + "; -theme-selection: " + css(t.getSelection())
				+ "; -theme-selection-text: " + css(t.getSelectionText()) + "; -theme-header: " + css(t.getHeader())
				+ "; -theme-header-text: " + css(t.getHeaderText()) + "; -theme-accent: " + css(t.getAccent())
				+ "; -theme-button-text: " + css(t.getButtonText()) + ";";
	}

	/** The style class on the root of a scene or dialog while a dark theme is in effect. */
	public static final String DARK_CLASS = "theme-dark";

	private static void colors(Parent root) {
		String style = root.getStyle() == null ? "" : root.getStyle();
		// replace what an earlier call set, keep the rest
		style = style.replaceAll("-(gradient|theme)-[a-z-]+:\\s*#[0-9A-Fa-f]{6};\\s*", "").trim();
		root.setStyle((colorStyle() + " " + style).trim());
		root.getStyleClass().remove(DARK_CLASS);
		if( Theme.current().isDark() ) {
			root.getStyleClass().add(DARK_CLASS);
		}
	}

	static String css(java.awt.Color c) {
		return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
	}
}
