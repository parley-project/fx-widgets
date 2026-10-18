# Changelog

## fx-widgets 1.1.0 (unreleased)

### Added

- `us.bringardner.fx.menu.StandardMenus`: the JavaFX twin of swing-widgets' `StandardMenus`, the
  same menu layout for JavaFX applications.
- `us.bringardner.fx.gradient.GradientLook`: parley-files-viewer's gradient look for JavaFX, the
  twin of swing-widgets' `GradientLook`. `apply(scene)` and `apply(dialog)` add its stylesheet
  (`gradient-look.css`) and set its two colors from swing-widgets' `GradientColors`.
  `follow(dialog)` gives a library's dialog the look only if the window that owns it has it.
- Themes: the stylesheet takes every color from swing-widgets' current `Theme` (Gold, Dark, Light,
  the color themes or a custom one): text, fields, selections, table headers, outlines and button
  text, not only the gradient. A dark theme also puts the `theme-dark` style class on the root
  (`GradientLook.DARK_CLASS`) for an application's own styles.

## fx-widgets 1.0.0 (2026-10-16)

### Changed

- Runs on Java 11, like the library's other components (was Java 17), with JavaFX 17 LTS
  (17.0.20; was JavaFX 21, which needs Java 17).

### Added

- `us.bringardner.fx.menu.RecentItemsMenu`, a "Recent ..." menu of any kind of item, saved with
  `java.util.prefs.Preferences`: the JavaFX twin of swing-widgets' `RecentItemsMenu`. Both show
  swing-widgets' `RecentItems`, so they share a list.
