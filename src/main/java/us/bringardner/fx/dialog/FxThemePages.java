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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The JavaFX pages of the theme extensions (swing-widgets' {@code ThemeExtensions}), by extension id:
 * the JavaFX theme designer shows a tab for each registered extension that has one.
 */
public final class FxThemePages {

	private static final Map<String, Supplier<FxThemePage>> PAGES = new ConcurrentHashMap<>();

	private FxThemePages() {
	}

	/**
	 * Gives an extension its JavaFX page; one already given is replaced.
	 *
	 * @param extensionId the extension's id ({@code ThemeExtension.getId()})
	 * @param page makes a new page, on the JavaFX thread
	 */
	public static void register(String extensionId, Supplier<FxThemePage> page) {
		PAGES.put(Objects.requireNonNull(extensionId, "extensionId"), Objects.requireNonNull(page, "page"));
	}

	/** Takes an extension's JavaFX page away. */
	public static void unregister(String extensionId) {
		PAGES.remove(extensionId);
	}

	/** A new page for an extension, or null if it has none. */
	public static FxThemePage create(String extensionId) {
		Supplier<FxThemePage> s = PAGES.get(extensionId);
		return s == null ? null : s.get();
	}
}
