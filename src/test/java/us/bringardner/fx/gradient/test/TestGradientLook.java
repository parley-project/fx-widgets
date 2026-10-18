package us.bringardner.fx.gradient.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import us.bringardner.fx.gradient.GradientLook;
import us.bringardner.swing.gradient.GradientColors;
import us.bringardner.swing.gradient.Theme;

public class TestGradientLook {

	@AfterEach
	public void restoreColors() {
		Theme.setCurrent(Theme.GOLD);
	}

	@Test
	public void theStylesheetIsInTheJar() {
		assertNotNull(GradientLook.class.getResource(GradientLook.STYLESHEET));
		assertTrue(GradientLook.stylesheet().endsWith(GradientLook.STYLESHEET));
	}

	@Test
	public void applyAddsTheSheetOnceAndPaintsTheGradient() throws Exception {
		Paint fill = Fx.call(()->{
			BorderPane root = new BorderPane();
			Scene scene = new Scene(root, 200, 100);
			GradientLook.apply(scene);
			GradientLook.apply(scene);
			assertEquals(1, scene.getStylesheets().size());
			root.applyCss();
			return firstFill(root);
		});
		LinearGradient g = assertGradient(fill);
		assertEquals(color(GradientColors.DEFAULT_START), g.getStops().get(0).getColor());
		assertEquals(color(GradientColors.DEFAULT_END), g.getStops().get(g.getStops().size() - 1).getColor());
	}

	@Test
	public void theColorsComeFromGradientColors() throws Exception {
		GradientColors.setStart(new java.awt.Color(0x11, 0x22, 0x33));
		GradientColors.setEnd(new java.awt.Color(0x44, 0x55, 0x66));
		Paint fill = Fx.call(()->{
			BorderPane root = new BorderPane();
			root.setStyle("-fx-padding: 3;");
			GradientLook.apply(new Scene(root, 200, 100));
			assertTrue(root.getStyle().contains("-fx-padding: 3;"), "the node's own style is kept");
			root.applyCss();
			return firstFill(root);
		});
		LinearGradient g = assertGradient(fill);
		assertEquals(Color.web("#112233"), g.getStops().get(0).getColor());
		assertEquals(Color.web("#445566"), g.getStops().get(g.getStops().size() - 1).getColor());
	}

	@Test
	public void aNewRootGetsTheColorsToo() throws Exception {
		String style = Fx.call(()->{
			Scene scene = GradientLook.apply(new Scene(new BorderPane(), 200, 100));
			BorderPane other = new BorderPane();
			scene.setRoot(other);
			return other.getStyle();
		});
		assertTrue(style.contains("-gradient-start"), style);
	}

	@Test
	public void aTableIsSeeThrough() throws Exception {
		Paint fill = Fx.call(()->{
			TableView<String> table = new TableView<>();
			table.getColumns().add(new TableColumn<>("Name"));
			table.getItems().addAll(List.of("a", "b"));
			GradientLook.apply(new Scene(new BorderPane(table), 300, 200));
			table.applyCss();
			return firstFill(table);
		});
		assertEquals(Color.TRANSPARENT, fill);
	}

	@Test
	public void aDialogGetsTheLook() throws Exception {
		Paint fill = Fx.call(()->{
			Alert a = GradientLook.apply(new Alert(Alert.AlertType.INFORMATION, "hello"));
			assertTrue(a.getDialogPane().getStylesheets().contains(GradientLook.stylesheet()));
			assertTrue(a.getDialogPane().getStyle().contains("-gradient-end"));
			a.getDialogPane().applyCss();
			return firstFill(a.getDialogPane());
		});
		assertGradient(fill);
	}

	private static LinearGradient assertGradient(Paint fill) {
		assertTrue(fill instanceof LinearGradient, "a gradient, not " + fill);
		return (LinearGradient) fill;
	}

	private static Paint firstFill(Region r) {
		Background b = r.getBackground();
		return b == null || b.getFills().isEmpty() ? null : b.getFills().get(0).getFill();
	}

	private static Color color(java.awt.Color c) {
		return Color.rgb(c.getRed(), c.getGreen(), c.getBlue());
	}

	@Test
	public void aDialogFollowsItsOwner() throws Exception {
		boolean[] r = Fx.call(()->{
			javafx.stage.Stage plain = new javafx.stage.Stage();
			plain.setScene(new Scene(new BorderPane(), 100, 100));
			javafx.stage.Stage gold = new javafx.stage.Stage();
			gold.setScene(GradientLook.apply(new Scene(new BorderPane(), 100, 100)));

			Alert a = new Alert(Alert.AlertType.INFORMATION, "a");
			a.initOwner(plain);
			GradientLook.follow(a);
			Alert b = new Alert(Alert.AlertType.INFORMATION, "b");
			b.initOwner(gold);
			GradientLook.follow(b);
			// a dialog that follows a dialog: its pane, not its scene, has the sheet
			Alert c = new Alert(Alert.AlertType.INFORMATION, "c");
			c.initOwner(b.getDialogPane().getScene().getWindow());
			GradientLook.follow(c);
			Alert d = new Alert(Alert.AlertType.INFORMATION, "d");
			GradientLook.follow(d);
			return new boolean[] {
					a.getDialogPane().getStylesheets().contains(GradientLook.stylesheet()),
					b.getDialogPane().getStylesheets().contains(GradientLook.stylesheet()),
					c.getDialogPane().getStylesheets().contains(GradientLook.stylesheet()),
					d.getDialogPane().getStylesheets().contains(GradientLook.stylesheet()),
					GradientLook.isApplied(null)};
		});
		assertTrue(!r[0], "an owner without the look: left alone");
		assertTrue(r[1], "an owner with the look: the dialog gets it");
		assertTrue(r[2], "a dialog owned by a dialog with the look gets it");
		assertTrue(!r[3], "no owner: left alone");
		assertTrue(!r[4]);
	}

	@Test
	public void aDarkThemeColorsTextAndMarksTheRoot() throws Exception {
		Theme.setCurrent(Theme.DARK);
		Object[] r = Fx.call(()->{
			javafx.scene.control.Label label = new javafx.scene.control.Label("x");
			javafx.scene.control.TextField field = new javafx.scene.control.TextField("y");
			BorderPane root = new BorderPane(field);
			root.setTop(label);
			GradientLook.apply(new Scene(root, 200, 100));
			root.applyCss();
			label.applyCss();
			field.applyCss();
			boolean dark = root.getStyleClass().contains(GradientLook.DARK_CLASS);
			Paint text = label.getTextFill();
			// the field's layers: its border first, its inside last
			Paint inside = field.getBackground().getFills().get(field.getBackground().getFills().size() - 1).getFill();
			// Modena shades the inside slightly, ending in the field color
			Paint fieldBg = inside instanceof LinearGradient
					? ((LinearGradient) inside).getStops().get(((LinearGradient) inside).getStops().size() - 1).getColor()
					: inside;
			// back to gold: the class goes
			Theme.setCurrent(Theme.GOLD);
			GradientLook.apply(root.getScene());
			return new Object[] {dark, text, fieldBg, root.getStyleClass().contains(GradientLook.DARK_CLASS)};
		});
		assertTrue((Boolean) r[0]);
		assertEquals(color(Theme.DARK.getText()), r[1]);
		assertEquals(color(Theme.DARK.getField()), r[2]);
		assertTrue(!(Boolean) r[3]);
	}

	@Test
	public void colorStyleHasEveryRole() {
		String s = GradientLook.colorStyle();
		for(String role : new String[] {"-gradient-start", "-gradient-end", "-theme-text", "-theme-field:",
				"-theme-field-text", "-theme-selection:", "-theme-selection-text", "-theme-header:",
				"-theme-header-text", "-theme-accent", "-theme-button-text"}) {
			assertTrue(s.contains(role), role);
		}
	}
}
