# Changelog

## fx-widgets 1.1.0 (unreleased)

### Added

- `FxThemePage.themeChanged(Theme)`: the theme designer tells an application's page of the theme
  as it changes, as swing-widgets' `ThemeExtension.Page` does. It does nothing by default.
- Folders (parley-files-fx's file views) in the theme's colors, as swing-widgets' `FolderIcon`: the
  stylesheet colors `.folder-icon`.
- On macOS, title bars go dark or light with the theme: when the first window gets the gradient
  look, and when Settings changes to or from a dark theme (swing-widgets' `MacAppearance`; the
  application needs JNA on its class path).
- `us.bringardner.fx.dialog`: the appearance settings for JavaFX applications, twins of
  swing-widgets' dialogs. `SettingsDialog` (theme and font, with a preview; shared with Swing
  applications), `ThemeDesigner` (new and edited themes, readability checks, applications' own
  pages through `FxThemePages`), `ThemePreview` and `FontDialog`.
- `GradientLook.setFont(font)` and `refresh()`: the font of everything with the look, and the theme
  and font put on every open window after a change. `colorStyle(theme)` gives any theme's colors.
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
