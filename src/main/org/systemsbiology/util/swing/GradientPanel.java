/**
 * 
 */
package org.systemsbiology.util.swing;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JPanel;

public class GradientPanel extends JPanel {
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Color c = getBackground();
		Color d = c.darker();
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle r = getBounds();
		g2.setPaint(new GradientPaint(0, 0, c, r.width, r.height, d));
		g2.fill(r);
	}
}