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

import java.util.Map;

import javafx.scene.Node;
import us.bringardner.swing.gradient.Theme;

/**
 * An application's page in the JavaFX theme designer: the JavaFX twin of swing-widgets'
 * {@code ThemeExtension.Page}, for the settings a {@code ThemeExtension} keeps in a theme (a script
 * editor's syntax colors, say). Register one with {@link FxThemePages#register}; an extension
 * without one keeps the settings of the theme the user starts from.
 */
public interface FxThemePage {

	/** What the designer shows. */
	Node getNode();

	/**
	 * Shows a theme's settings.
	 *
	 * @param values the settings (keys without the extension's prefix), defaults filled in
	 * @param theme the theme being designed
	 */
	void setValues(Map<String, String> values, Theme theme);

	/** The settings as the user has them now (keys without the prefix). */
	Map<String, String> getValues();

	/** Called whenever the user changes a setting. */
	void setChangeListener(Runnable listener);
}
