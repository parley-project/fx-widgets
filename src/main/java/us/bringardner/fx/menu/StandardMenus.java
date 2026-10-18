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
package us.bringardner.fx.menu;

import java.util.function.BooleanSupplier;

import javafx.application.Platform;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * The menus every JavaFX application has, as swing-widgets' StandardMenus has them for Swing: File,
 * Edit, View, the application's own menus, Window and Help, in that order.
 * <p>
 * About, Settings... and Quit follow the platform: on macOS they are in the application menu at the
 * top of the screen, under the application's name (JavaFX's own menu, given the items; see
 * MacAppMenu); elsewhere, or if that can't be done, Settings... and Exit end the File menu and About
 * is in Help. Shortcuts use "Shortcut", the
 * platform's shortcut key (⌘ on macOS, Ctrl elsewhere).
 */
public class StandardMenus {

	public final Menu file = new Menu("File");
	public final Menu edit = new Menu("Edit");
	public final Menu view = new Menu("View");
	public final Menu window = new Menu("Window");
	public final Menu help = new Menu("Help");

	private final String appName;
	private Runnable about;
	private Runnable settings;
	private BooleanSupplier quit;

	/** @param appName the application's name, as About shows it */
	public StandardMenus(String appName) {
		this.appName = appName;
	}

	public static boolean isMac() {
		return System.getProperty("os.name", "").toLowerCase().startsWith("mac");
	}

	/**
	 * A menu item that runs an action.
	 *
	 * @param accelerator a KeyCombination such as "Shortcut+O", or null for none
	 */
	public static MenuItem item(String text, String accelerator, Runnable action) {
		MenuItem item = new MenuItem(text);
		if( accelerator != null ) {
			item.setAccelerator(KeyCombination.keyCombination(accelerator));
		}
		item.setOnAction(e->action.run());
		return item;
	}

	public StandardMenus about(Runnable action) {
		this.about = action;
		return this;
	}

	public StandardMenus settings(Runnable action) {
		this.settings = action;
		return this;
	}

	/** What Quit (Exit) does: close the windows if it can, and say whether the application may end. */
	public StandardMenus quit(BooleanSupplier okToQuit) {
		this.quit = okToQuit;
		return this;
	}

	/**
	 * The menu bar for a window: File, Edit, View, the application's menus, Window and Help (a menu
	 * with nothing in it is left out). It uses the screen's menu bar where there is one (macOS).
	 *
	 * @param stage the window
	 * @param appMenus the application's own menus, between View and Window
	 */
	public MenuBar build(Stage stage, Menu... appMenus) {
		boolean inAppMenu = isMac() && MacAppMenu.available();
		if( inAppMenu ) {
			MacAppMenu.use(this, appName, stage);
		}
		return build(stage, inAppMenu, appMenus);
	}

	/** What About does, or null. */
	Runnable aboutAction() {
		return about;
	}

	/** What Settings... does, or null. */
	Runnable settingsAction() {
		return settings;
	}

	/** As above, with About, Settings and Quit in the application menu (inAppMenu) or not: for tests. */
	MenuBar build(Stage stage, boolean inAppMenu, Menu... appMenus) {
		if( !inAppMenu ) {
			if( settings != null ) {
				separate(file);
				file.getItems().add(item("Settings...", "Shortcut+COMMA", settings));
			}
			if( quit != null ) {
				separate(file);
				file.getItems().add(item(isMac() ? "Quit " + appName : "Exit", isMac() ? "Shortcut+Q" : null, this::quit));
			}
			if( about != null ) {
				separate(help);
				help.getItems().add(item("About " + appName, null, about));
			}
		}
		windowMenu(stage);
		MenuBar bar = new MenuBar();
		for(Menu m : new Menu[] {file, edit, view}) {
			if( !m.getItems().isEmpty() ) {
				bar.getMenus().add(m);
			}
		}
		bar.getMenus().addAll(appMenus);
		bar.getMenus().add(window);
		if( !help.getItems().isEmpty() ) {
			bar.getMenus().add(help);
		}
		bar.setUseSystemMenuBar(true);
		return bar;
	}

	private static void separate(Menu menu) {
		if( !menu.getItems().isEmpty() ) {
			menu.getItems().add(new SeparatorMenuItem());
		}
	}

	/** Window: Minimize, then the application's open windows, this one checked. */
	private void windowMenu(Stage stage) {
		window.getItems().add(item("Minimize", "Shortcut+M", ()->stage.setIconified(true)));
		// listed afresh each time the menu opens
		window.setOnShowing(e->listWindows(stage));
		listWindows(stage);
	}

	/** The open windows under Minimize: the application's stages that are showing and have a title. */
	void listWindows(Stage stage) {
		window.getItems().remove(1, window.getItems().size());
		boolean first = true;
		for(Window w : Window.getWindows()) {
			if( w instanceof Stage && w.isShowing() && ((Stage) w).getTitle() != null && !((Stage) w).getTitle().isEmpty() ) {
				Stage s = (Stage) w;
				if( first ) {
					window.getItems().add(new SeparatorMenuItem());
					first = false;
				}
				CheckMenuItem item = new CheckMenuItem(s.getTitle());
				item.setSelected(s == stage);
				item.setOnAction(e->{
					item.setSelected(s == stage);
					s.setIconified(false);
					s.toFront();
					s.requestFocus();
				});
				window.getItems().add(item);
			}
		}
	}

	/** Quit or Exit: the application's say, then the end of JavaFX and the application. */
	void quit() {
		if( quit == null || quit.getAsBoolean() ) {
			Platform.exit();
			System.exit(0);
		}
	}
}
