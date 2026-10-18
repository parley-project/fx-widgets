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

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import us.bringardner.swing.gradient.Theme;

/**
 * A theme to click on, as in swing-widgets' settings dialog: a small picture of it (its gradient,
 * text, a field and a selection), its name underneath, and a ring when it is the one chosen.
 */
class ThemeCard extends VBox {

	static final double W = 84;
	static final double H = 56;

	final Theme theme;
	private final Canvas tile = new Canvas(W, H);
	private final Label label;
	private boolean selected;

	ThemeCard(Theme theme, Runnable onClick) {
		super(3);
		this.theme = theme;
		setAlignment(Pos.CENTER);
		label = new Label(theme.getName());
		label.setMaxWidth(W + 6);
		getChildren().addAll(tile, label);
		setCursor(Cursor.HAND);
		setAccessibleText(theme.getName() + " theme");
		setOnMouseClicked(e->onClick.run());
		paint();
	}

	void setSelected(boolean selected) {
		this.selected = selected;
		label.setStyle(selected ? "-fx-font-weight: bold;" : "");
		paint();
	}

	boolean isSelected() {
		return selected;
	}

	private void paint() {
		GraphicsContext g = tile.getGraphicsContext2D();
		g.clearRect(0, 0, W, H);
		double inset = 3;
		double tw = W - inset * 2;
		double th = H - inset * 2;
		g.save();
		g.beginPath();
		g.moveTo(inset + 5, inset);
		g.arcTo(inset + tw, inset, inset + tw, inset + th, 5);
		g.arcTo(inset + tw, inset + th, inset, inset + th, 5);
		g.arcTo(inset, inset + th, inset, inset, 5);
		g.arcTo(inset, inset, inset + tw, inset, 5);
		g.closePath();
		g.setFill(new LinearGradient(inset, inset, inset + tw, inset + th, false, CycleMethod.NO_CYCLE,
				new Stop(0, Colors.fx(theme.getStart())), new Stop(1, Colors.fx(theme.getEnd()))));
		g.fill();
		g.clip();
		// a line of text on the gradient
		g.setFill(Colors.fx(theme.getText()));
		g.setFont(Font.font(null, FontWeight.BOLD, 13));
		g.fillText("Aa", inset + 7, inset + 18);
		// a field, and a selected row
		g.setFill(Colors.fx(theme.getField()));
		g.fillRect(inset + 36, inset + 7, tw - 43, 12);
		g.setFill(Colors.fx(theme.getFieldText()));
		g.fillRect(inset + 39, inset + 12, 18, 2);
		g.setFill(Colors.fx(theme.getSelection()));
		g.fillRect(inset, inset + th - 16, tw, 10);
		g.setFill(Colors.fx(theme.getSelectionText()));
		g.fillRect(inset + 7, inset + th - 12, 24, 2);
		g.restore();
		if( selected ) {
			g.setLineWidth(3);
			g.setStroke(Color.rgb(0, 0, 0, 170 / 255.0));
			g.strokeRoundRect(inset - 1, inset - 1, tw + 2, th + 2, 12, 12);
			g.setLineWidth(1);
			g.setStroke(Color.rgb(255, 255, 255, 200 / 255.0));
			g.strokeRoundRect(inset + 1.5, inset + 1.5, tw - 3, th - 3, 9, 9);
		} else {
			g.setLineWidth(1);
			g.setStroke(Color.rgb(0, 0, 0, 70 / 255.0));
			g.strokeRoundRect(inset + 0.5, inset + 0.5, tw - 1, th - 1, 10, 10);
		}
	}
}
