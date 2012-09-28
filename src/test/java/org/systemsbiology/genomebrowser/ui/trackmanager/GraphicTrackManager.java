package org.systemsbiology.genomebrowser.ui.trackmanager;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;


public class GraphicTrackManager extends JFrame {
	
	private TrackLayoutPanel trackLayoutPanel;
	private JButton singleTrackButton;
	private JButton dualTrackButton;
	private JButton centerTrackButton;

	public GraphicTrackManager() {
		setLayout(new BorderLayout());
		add(new JLabel("Track Manager"), BorderLayout.NORTH);
		trackLayoutPanel = new TrackLayoutPanel();
		add(trackLayoutPanel, BorderLayout.CENTER);

		singleTrackButton = new JButton("New single track");
		singleTrackButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				trackLayoutPanel.addSingleTrack();
			}
		});
		dualTrackButton = new JButton("New dual track");
		dualTrackButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				trackLayoutPanel.addDualTrack();
			}
		});
		centerTrackButton = new JButton("New Center Track");
		centerTrackButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				trackLayoutPanel.addCenterTrack();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(singleTrackButton);
		buttonPanel.add(dualTrackButton);
		buttonPanel.add(centerTrackButton);

		add(buttonPanel, BorderLayout.SOUTH);
		pack();
	}

	public static void main(String[] args) {
		GraphicTrackManager gtm = new GraphicTrackManager();
		gtm.setVisible(true);
	}
}
