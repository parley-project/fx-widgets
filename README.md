# fx-widgets

Reusable JavaFX components, the JavaFX counterpart of
[swing-widgets](https://github.com/parley-project/swing-widgets). Only what JavaFX lacks is here;
where JavaFX already has something (CSS styling, `Alert`, prompt text, `Task` progress) there is no
twin of the Swing widget.

- **Java 11** or later, like the rest of the library's components
- Depends on JavaFX 17 LTS (`javafx-controls`) and swing-widgets, for the UI-free list behind the menu
- Apache License 2.0

## Why this library exists

JavaFX is missing a few things a desktop application needs, such as a "recent files" menu, and
[swing-widgets](https://github.com/parley-project/swing-widgets) only helps Swing applications.
fx-widgets fills those gaps for JavaFX, and only those: it doesn't copy Swing widgets that JavaFX
already covers.

It's a separate library from swing-widgets because JavaFX costs more to depend on:

- **Native libraries for each platform.** Since Java 11, JavaFX isn't part of the JDK. It runs only
  on the platforms OpenJFX is built for, and an application has to ship it.

Keeping JavaFX here means a Swing application never needs it.

It depends on swing-widgets for one thing: `RecentItems`, the list behind both "recent" menus. It
uses no UI toolkit, so a Swing and a JavaFX menu behave the same and, given the same preferences,
share one list. Depending on swing-widgets doesn't start Swing, and every platform with JavaFX has it
anyway.

**Use fx-widgets** when your application's UI is JavaFX.

## Getting started

```xml
<dependency>
    <groupId>us.bringardner</groupId>
    <artifactId>bringardner-fx-widgets</artifactId>
    <version>1.0.0</version>
</dependency>
```

## What's inside

| Package | Class | Purpose |
|---|---|---|
| `us.bringardner.fx.menu` | `RecentItemsMenu` | A "Recent ..." menu of any kind of item, saved with `java.util.prefs.Preferences`. |
| | `StandardMenus` | The menu bar every Parley application uses: the twin of swing-widgets' `StandardMenus`. |
| `us.bringardner.fx.dialog` | `SettingsDialog` | The appearance settings (theme and font, with a preview), the twin of swing-widgets' `SettingsDialog`; saved where swing-widgets' `Appearance` keeps them, so Swing and JavaFX applications share them. |
| | `ThemeDesigner`, `ThemePreview` | Make a new theme or change one of the user's own, with readability checks; a preview of a theme. |
| | `FontDialog` | Choose a font: family, style and size, with a sample. |
| | `FxThemePage`, `FxThemePages` | An application's own page in the theme designer (the JavaFX side of swing-widgets' `ThemeExtension`). |
| `us.bringardner.fx.gradient` | `GradientLook` | parley-files-viewer's gradient look as a stylesheet: the twin of swing-widgets' `GradientLook`, with the same colors. |

### RecentItemsMenu

The twin of swing-widgets' `RecentItemsMenu`. Both show swing-widgets' `RecentItems`, which uses no
UI toolkit, so a Swing and a JavaFX menu given the same preferences node share one list. Subclasses
override the same methods (`getLabel`, `isStale`, `merge`, `openItem`, `getMaxItemsText`).

```java
RecentItemsMenu<String> recent = new RecentItemsMenu<>("Open Recent", prefs, codec);
recent.setOnOpened(value -> open((String) value));
...
recent.addEntry(path);
```

Differences from the Swing menu:

- A JavaFX `Menu` already has `getItems()` (its menu items), so the list's methods are
  `getEntries()`, `setEntries(...)` and `addEntry(...)`.
- The value `openItem` returns goes to `setOnOpened(...)` instead of to ActionListeners.
- The maximum is asked for by `askMaxItems`, and errors shown by `showError`; tests override them
  instead of showing dialogs.

## Standard menus

`StandardMenus` is the JavaFX twin of swing-widgets' `StandardMenus`: the same menu bar (File,
Edit, View, the application's menus, Window and Help) for a JavaFX window.

```java
StandardMenus menus = new StandardMenus("My App")
        .about(() -> showAbout())
        .settings(() -> showSettings())
        .quit(() -> okToQuit());
menus.file.getItems().add(StandardMenus.item("Open...", "Shortcut+O", () -> open()));
root.setTop(menus.build(stage, runMenu));
```

On macOS the bar uses the system menu bar, and About, Settings… and Quit go in the application
menu when the platform lets JavaFX set them there; otherwise they stay in the window's menus, as on
other platforms (Settings… and Exit at the end of File, About in Help).

## Settings

`SettingsDialog` is the appearance settings for a JavaFX application, the twin of swing-widgets'
`SettingsDialog`: a theme (the built-in ones and the user's own), New theme..., Edit theme... and
Delete theme, the font, and a preview. They are kept where swing-widgets' `Appearance` keeps them,
so a Swing and a JavaFX application given the same preferences node share them.

```java
// at start-up, before any window is shown
SettingsDialog.load(prefs);

// Settings... (StandardMenus' settings item)
if( SettingsDialog.showDialog(stage, prefs) ) {
    // the theme and font are already on every window with the gradient look;
    // anything else of the application's that follows the theme goes here
}
```

An application's own part of a theme (swing-widgets' `ThemeExtension`, such as fsh-ide's syntax
colors) gets a tab in the JavaFX theme designer when it registers a page:
`FxThemePages.register(extensionId, MyPage::new)`.

## The gradient look

The look of parley-files-viewer (a diagonal gradient from light to dark gold behind everything, trees
and tables you see it through, rounded gradient buttons) for a JavaFX application:

```java
Scene scene = GradientLook.apply(new Scene(root, 900, 600));
Alert alert = GradientLook.apply(new Alert(AlertType.CONFIRMATION, "Quit?"));  // dialogs are their own windows
```

The colors are swing-widgets' current `Theme` (Gold, Dark, Light, the color themes or a custom one),
so an application with both Swing and JavaFX windows changes them in one place. With a dark theme the
root has the `theme-dark` style class, for an application's own styles. Popups (a `Popup`'s scene) take `GradientLook.apply(popup.getScene())`. A library's dialogs use
`GradientLook.follow(dialog)` after `initOwner`: the dialog gets the look only if its owner has it.

## Swing alternatives

| fx-widgets | In Swing |
|---|---|
| `RecentItemsMenu` | swing-widgets' `RecentItemsMenu`, which shares its list with this one |
| `StandardMenus` | swing-widgets' `StandardMenus`: the same layout for a Swing `JMenuBar` |
| `SettingsDialog`, `ThemeDesigner`, `FontDialog` | swing-widgets' dialogs of the same names |
| `GradientLook` | swing-widgets' `GradientLook`; both take their colors from swing-widgets' `GradientColors` |

swing-widgets has more, because Swing lacks more: date and time pickers, a thread-safe progress
monitor updater, a password field with a show/hide toggle, and gradient styling. Its README's
"JavaFX alternatives" section shows what to use for each of those in JavaFX.

## Building

```bash
mvn package
```

The tests start JavaFX, so they need a display; where JavaFX can't start they're skipped.

## License

Apache License 2.0. See [LICENSE](LICENSE).
