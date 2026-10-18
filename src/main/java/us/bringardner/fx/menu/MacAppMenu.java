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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * The macOS application menu of a JavaFX application: About, Settings... and Quit in it, under the
 * application's name, as a Swing application has them.
 * <p>
 * JavaFX makes that menu itself (Hide, Hide Others, Show All, Quit) and offers no way to add to it,
 * and java.awt.Desktop's handlers don't reach it (JavaFX, not AWT, owns the menu), so this goes
 * through JavaFX's own macOS classes (com.sun.glass.ui), by reflection. They are internal: where
 * they can't be reached (JavaFX on the module path without them exported, a later JavaFX that has
 * changed them) {@link #available()} is false and StandardMenus puts the items in File and Help.
 */
final class MacAppMenu {

	/** The menus of the window last in front: the application menu acts for it. */
	private static StandardMenus current;
	private static boolean customized;
	private static int tries;

	private MacAppMenu() {
	}

	/** JavaFX's application, if it is JavaFX's macOS one and can be reached; null otherwise. */
	private static Object app() {
		try {
			Class<?> c = Class.forName("com.sun.glass.ui.Application");
			Object app = c.getMethod("GetApplication").invoke(null);
			return app != null && app.getClass().getName().equals("com.sun.glass.ui.mac.MacApplication") ? app : null;
		} catch (Throwable e) {
			return null;
		}
	}

	/** True on macOS when the application menu can be given About, Settings and Quit. */
	static boolean available() {
		Object app = app();
		if( app == null ) {
			return false;
		}
		try {
			method(app, "getAppleMenu");
			Class.forName("com.sun.glass.ui.MenuItem$Callback");
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	/**
	 * The application menu acts for these menus while their window is in front, and gets its items
	 * once the window is shown (JavaFX makes the menu then).
	 */
	static void use(StandardMenus menus, String appName, Stage stage) {
		if( current == null ) {
			current = menus;
			// the name in "Hide ..." and "Quit ..." (JavaFX takes it when it makes the menu)
			invoke(app(), "setName", new Class<?>[] {String.class}, appName);
		}
		stage.focusedProperty().addListener((o, was, is)->{
			if( is ) {
				current = menus;
			}
		});
		stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e->{
			current = menus;
			customize(appName);
		});
	}

	/** About, Settings... and Quit in the application menu, once; it is tried again until JavaFX has made it. */
	private static void customize(String appName) {
		if( customized ) {
			return;
		}
		Object app = app();
		Object menu = app == null ? null : invoke(app, "getAppleMenu", new Class<?>[0]);
		if( menu == null ) {
			if( tries++ < 50 ) {
				PauseTransition later = new PauseTransition(Duration.millis(100));
				later.setOnFinished(e->customize(appName));
				later.play();
			} else {
				customized = true;
				failed("JavaFX made no application menu");
			}
			return;
		}
		customized = true;
		// the menu's title: macOS shows the process's name ("java" when started with java) unless
		// this takes
		invoke(menu, "setTitle", new Class<?>[] {String.class}, appName);
		try {
			Class<?> menuClass = Class.forName("com.sun.glass.ui.Menu");
			Class<?> itemClass = Class.forName("com.sun.glass.ui.MenuItem");
			Class<?> callbackClass = Class.forName("com.sun.glass.ui.MenuItem$Callback");
			Object separator = itemClass.getField("Separator").get(null);
			Method insert = menuClass.getMethod("insert", itemClass, int.class);
			Method create = method(app, "createMenuItem", String.class, callbackClass, int.class, int.class);

			// JavaFX's own: Hide <name>, ..., Quit <name>: the name, and Quit asks the application
			@SuppressWarnings("unchecked")
			List<Object> items = (List<Object>) menuClass.getMethod("getItems").invoke(menu);
			for(Object item : items) {
				if( item != separator && itemClass.isInstance(item) ) {
					String title = (String) itemClass.getMethod("getTitle").invoke(item);
					if( title != null && title.startsWith("Quit") ) {
						itemClass.getMethod("setTitle", String.class).invoke(item, "Quit " + appName);
						itemClass.getMethod("setCallback", callbackClass).invoke(item, callback(callbackClass, ()->{
							if( current != null ) {
								current.quit();
							} else {
								Platform.exit();
							}
						}));
					} else if( title != null && title.startsWith("Hide") && !title.equals("Hide Others") ) {
						itemClass.getMethod("setTitle", String.class).invoke(item, "Hide " + appName);
					}
				}
			}

			int at = 0;
			insert.invoke(menu, create.invoke(app, "About " + appName, callback(callbackClass, ()->{
				if( current != null && current.aboutAction() != null ) {
					current.aboutAction().run();
				}
			}), 0, 0), at++);
			insert.invoke(menu, separator, at++);
			// ⌘, : glass's key is the character, its modifier for ⌘ is 16
			insert.invoke(menu, create.invoke(app, "Settings...", callback(callbackClass, ()->{
				if( current != null && current.settingsAction() != null ) {
					current.settingsAction().run();
				}
			}), ',', 16), at++);
			insert.invoke(menu, separator, at++);
		} catch (Throwable e) {
			failed(e.toString());
		}
	}

	/** The application menu can't have the items: the windows' File and Help menus get them instead. */
	private static void failed(String why) {
		System.err.println("fx-widgets: About, Settings and Quit are in File and Help: can't add them to the macOS application menu: " + why);
		StandardMenus.inWindowMenus();
	}

	/** A glass menu item callback that runs an action on the JavaFX thread. */
	private static Object callback(Class<?> callbackClass, Runnable action) {
		return Proxy.newProxyInstance(callbackClass.getClassLoader(), new Class<?>[] {callbackClass}, (proxy, m, args)->{
			switch (m.getName()) {
				case "action":
					if( Platform.isFxApplicationThread() ) {
						action.run();
					} else {
						Platform.runLater(action);
					}
					return null;
				case "validate":
					return null;
				case "hashCode":
					return System.identityHashCode(proxy);
				case "equals":
					return proxy == args[0];
				case "toString":
					return "fx-widgets application menu item";
				default:
					return null;
			}
		});
	}

	private static Object invoke(Object target, String method, Class<?>[] types, Object... args) {
		if( target == null ) {
			return null;
		}
		try {
			return method(target, method, types).invoke(target, args);
		} catch (Throwable e) {
			return null;
		}
	}

	/**
	 * A public method of an object, callable: JavaFX's macOS application class isn't public, so
	 * its own methods (getAppleMenu) can only be called once made accessible.
	 */
	static Method method(Object target, String name, Class<?>... types) throws ReflectiveOperationException {
		Method m = target.getClass().getMethod(name, types);
		if( !java.lang.reflect.Modifier.isPublic(m.getDeclaringClass().getModifiers()) ) {
			m.setAccessible(true);
		}
		return m;
	}
}
