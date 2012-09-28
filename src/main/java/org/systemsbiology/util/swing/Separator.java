package org.systemsbiology.util.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;


public class Separator extends JPanel {

	public Separator() {
		super();
		setPreferredSize(new Dimension(0,5));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		int w = this.getWidth();
		int y = this.getHeight()/2;
		Color oldColor = g.getColor();
		for (int x=2; x<w; x+=2) {
			float alpha = 1.0f - ((float)Math.abs(x-w)) / ((float)w);
			g.setColor(new Color(0.33f, 0.66f, 0.99f, alpha));
			g.drawLine(x-2, y, x, y);
		}
		g.setColor(oldColor);
	}
}
