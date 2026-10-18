/**
 *	Copyright 2026 Tony Bringardner
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 *	the specific language governing permissions and limitations under the License.
 */
package us.bringardner.fx.dialog;

import javafx.scene.Node;

/**
 * An application's own tab in the Settings dialog ({@link SettingsDialog}), after Appearance, as
 * swing-widgets' {@code SettingsPage} is in the Swing one. Register a way to make one with
 * {@link FxSettingsPages#register}; each dialog makes its own page.
 * <p>
 * The page keeps its changes to itself until OK: then {@link #problem()} is asked first (a problem
 * keeps the dialog open, on this tab), and {@link #ok()} keeps them. Cancel calls {@link #cancel()}.
 */
public interface FxSettingsPage {

	/** The tab's title, such as "Templates". */
	String getTitle();

	/** What the tab shows. */
	Node getNode();

	/** Why OK can't keep the changes (shown to the user), or null if it can. */
	default String problem() {
		return null;
	}

	/**
	 * OK: keep the changes.
	 *
	 * @throws Exception if they can't be kept (the dialog shows it and stays open)
	 */
	void ok() throws Exception;

	/** Cancel: forget the changes. Nothing by default. */
	default void cancel() {
	}
}
