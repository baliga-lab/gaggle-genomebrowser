/**
 * 
 */
package org.systemsbiology.genomebrowser.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

public class ColorIcon implements Icon {
	int width;
	int height;
	public Color color = null;

	public ColorIcon(int w, int h) {
		this.width = w;
		this.height = h;
	}

	public int getIconHeight() {
		return height;
	}

	public int getIconWidth() {
		return width;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Color old = g.getColor();
		if (color == null) {
			g.setColor(Color.RED);
			g.drawOval(x+1, y+1, width-2, height-2);
			g.drawLine(x, y+height, x+width, y);
		}
		else {
			g.setColor(color);
			g.fillRect(x, y, width, height);
		}
		g.setColor(old);
	}
	
}