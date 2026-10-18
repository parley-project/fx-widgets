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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

/** The standard JavaFX menus: their order, and About, Settings and Exit by platform. */
public class TestStandardMenus {

	private static boolean started;

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
		return f.get(20, TimeUnit.SECONDS);
	}

	private static List<String> titles(MenuBar bar) {
		List<String> ret = new ArrayList<>();
		bar.getMenus().forEach(m->ret.add(m.getText()));
		return ret;
	}

	private static List<String> items(Menu m) {
		List<String> ret = new ArrayList<>();
		for(MenuItem i : m.getItems()) {
			ret.add(i instanceof SeparatorMenuItem ? "-" : i.getText());
		}
		return ret;
	}

	@Test
	public void elsewhereSettingsAndExitEndFileAndAboutIsInHelp() throws Exception {
		AtomicInteger settings = new AtomicInteger();
		AtomicInteger about = new AtomicInteger();
		fx(()->{
			Stage s = new Stage();
			StandardMenus m = new StandardMenus("Test App").settings(settings::incrementAndGet)
					.about(about::incrementAndGet).quit(()->false);
			m.file.getItems().add(StandardMenus.item("Open...", "Shortcut+O", ()->{}));
			MenuBar bar = m.build(s, false, new Menu("Run"));
			assertEquals(List.of("File", "Run", "Window", "Help"), titles(bar));
			List<String> file = items(bar.getMenus().get(0));
			assertEquals("Open...", file.get(0));
			assertTrue(file.contains("Settings..."));
			assertEquals(KeyCombination.keyCombination("Shortcut+COMMA"), m.file.getItems().get(2).getAccelerator());
			m.file.getItems().get(2).fire();
			m.help.getItems().get(0).fire();
			assertEquals(List.of("About Test App"), items(m.help));
			assertTrue(bar.isUseSystemMenuBar());
			return null;
		});
		assertEquals(1, settings.get());
		assertEquals(1, about.get());
	}

	@Test
	public void inTheApplicationMenuTheyAreLeftOut() throws Exception {
		fx(()->{
			StandardMenus m = new StandardMenus("Test App").settings(()->{}).about(()->{}).quit(()->true);
			m.view.getItems().add(StandardMenus.item("Refresh", null, ()->{}));
			MenuBar bar = m.build(new Stage(), true);
			assertEquals(List.of("View", "Window"), titles(bar), "File and Help are empty");
			return null;
		});
	}

	@Test
	public void theWindowMenuListsTheOpenWindows() throws Exception {
		fx(()->{
			Stage one = new Stage();
			one.setTitle("One");
			Stage two = new Stage();
			two.setTitle("Two");
			one.show();
			two.show();
			try {
				StandardMenus m = new StandardMenus("Test App");
				m.build(one, true);
				m.listWindows(one);
				List<String> w = items(m.window);
				assertEquals("Minimize", w.get(0));
				assertTrue(w.contains("One") && w.contains("Two"), w.toString());
				for(MenuItem i : m.window.getItems()) {
					if( i instanceof CheckMenuItem ) {
						assertEquals("One".equals(i.getText()), ((CheckMenuItem) i).isSelected());
					}
				}
				two.hide();
				m.listWindows(one);
				assertFalse(items(m.window).contains("Two"));
			} finally {
				one.hide();
				two.hide();
			}
			return null;
		});
	}
}
