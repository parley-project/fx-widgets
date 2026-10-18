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

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import us.bringardner.fx.gradient.GradientLook;
import us.bringardner.swing.gradient.Theme;

/**
 * A small window's worth of controls in a theme, whatever theme is in effect: text on the gradient,
 * a field, a table with its header and a selected row, and a button. The JavaFX twin of
 * swing-widgets' {@code ThemePreview}.
 */
public class ThemePreview extends BorderPane {

	/** A row of the preview's table. */
	public static class Row {
		private final String name;
		private final String size;
		private final String modified;

		Row(String name, String size, String modified) {
			this.name = name;
			this.size = size;
			this.modified = modified;
		}

		public String getName() {
			return name;
		}

		public String getSize() {
			return size;
		}

		public String getModified() {
			return modified;
		}
	}

	private final TableView<Row> table = new TableView<>();
	private Theme theme;

	public ThemePreview() {
		// a root of its own, so the look's colors are the ones this preview is given
		getStyleClass().addAll("root", "theme-preview");
		setPadding(new Insets(10));
		setStyle("-fx-border-color: rgba(0,0,0,0.25);");
		Label label = new Label("Folder:");
		TextField field = new TextField("Documents/Reports");
		field.setFocusTraversable(false);
		HBox north = new HBox(8, label, field);
		north.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(field, javafx.scene.layout.Priority.ALWAYS);
		setTop(north);

		TableColumn<Row, String> name = new TableColumn<>("Name");
		name.setCellValueFactory(new PropertyValueFactory<>("name"));
		name.setPrefWidth(150);
		TableColumn<Row, String> size = new TableColumn<>("Size");
		size.setCellValueFactory(new PropertyValueFactory<>("size"));
		size.setPrefWidth(70);
		TableColumn<Row, String> modified = new TableColumn<>("Modified");
		modified.setCellValueFactory(new PropertyValueFactory<>("modified"));
		modified.setPrefWidth(110);
		table.getColumns().add(name);
		table.getColumns().add(size);
		table.getColumns().add(modified);
		table.setItems(FXCollections.observableArrayList(new Row("Budget.xlsx", "48 KB", "Today"),
				new Row("Minutes.docx", "21 KB", "Yesterday"), new Row("Photos", "", "Last week")));
		table.getSelectionModel().select(1);
		table.setFocusTraversable(false);
		table.setPrefHeight(112);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		BorderPane.setMargin(table, new Insets(8, 0, 8, 0));
		setCenter(table);

		Button button = new Button("Open");
		button.setFocusTraversable(false);
		HBox south = new HBox(button);
		south.setAlignment(Pos.CENTER_RIGHT);
		setBottom(south);
		setPrefWidth(360);
		show(Theme.current(), null);
	}

	/**
	 * Shows a theme and a font.
	 *
	 * @param t the theme
	 * @param font the font, or null for the standard one
	 */
	public void show(Theme t, java.awt.Font font) {
		theme = t;
		setStyle(GradientLook.colorStyle(t) + " " + GradientLook.fontStyle(font) + " -fx-border-color: rgba(0,0,0,0.25);");
		getStyleClass().remove(GradientLook.DARK_CLASS);
		if( t.isDark() ) {
			getStyleClass().add(GradientLook.DARK_CLASS);
		}
		// the selected row in the theme's selection colors, as when the table has the focus
		table.setStyle("-fx-selection-bar-non-focused: -theme-selection;");
	}

	/** The theme shown. */
	public Theme getTheme() {
		return theme;
	}
}
