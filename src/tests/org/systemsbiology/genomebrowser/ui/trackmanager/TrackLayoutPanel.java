package org.systemsbiology.genomebrowser.ui.trackmanager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;


public class TrackLayoutPanel extends JPanel {
	List<Track> tracks = new ArrayList<Track>();
	Color evenFillColor = new Color(0x30993333, true);
	Color evenBorderColor = new Color(0x90993333, true);
	Color oddFillColor = new Color(0x30336699, true);
	Color oddBorderColor = new Color(0x90336699, true);
	Dimension size = new Dimension();

	public TrackLayoutPanel() {
		setPreferredSize(new Dimension(600,500));
		setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	}

	
	@Override
	protected void paintComponent(Graphics g) {
		Color fillColor, borderColor;
		super.paintComponent(g);

		getSize(size);

		boolean parity = false;
		for (Track track : tracks) {
			int y = (int)Math.round(track.top * size.height);
			int h = (int)Math.round(track.height * size.height);

			if (parity) {
				fillColor = evenFillColor;
				borderColor = evenBorderColor;
			}
			else {
				fillColor = oddFillColor;
				borderColor = oddBorderColor;
			}
			parity = !parity;

			g.setColor(fillColor);
			g.fillRect(0, y, size.width-1, h);
			g.setColor(borderColor);
			g.drawRect(0, y, size.width-1, h);
			g.setColor(Color.BLACK);
			((Graphics2D)g).drawString(track.name, 20.0f, (float)(y) + 18.0f);
		}
	}

	public void addSingleTrack() {
		squeezeTracks();
		double h = 1.0;
		if (tracks.size() > 0)
			h = 1.0 / (tracks.size() + 1);
		tracks.add(new Track(0.0, h, String.valueOf(tracks.size() + 1)));
		repaint();
	}

	public void addCenterTrack() {
		
	}

	public void addDualTrack() {
		
	}

	private void squeezeTracks() {
		double fraction = 1.0 / (tracks.size() + 1);
		for (Track track : tracks) {
			track.height = track.height * (1.0 - fraction);
			track.top = track.top + (1.0 - track.top) * fraction;
		}
	}
}

class Track {
	double top;
	double height;
	String name;

	public Track(double top, double height, String name) {
		this.top = top;
		this.height = height;
		this.name = name;
	}
}