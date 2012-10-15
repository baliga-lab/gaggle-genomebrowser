package org.systemsbiology.genomebrowser.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Range;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.sqlite.TrackSaver;
import org.systemsbiology.genomebrowser.util.TrackUtils;
import org.systemsbiology.genomebrowser.visualization.TrackManager;
import org.systemsbiology.genomebrowser.visualization.TrackRenderer;
import org.systemsbiology.genomebrowser.visualization.TrackRendererRegistry;
import org.systemsbiology.util.ActionListenerSupport;
import org.systemsbiology.util.StringUtils;
import org.systemsbiology.util.swing.ETable;
import org.systemsbiology.util.swing.OpenDefaultComboBoxModel;

import com.bric.swing.ColorPicker;

// TODO needs refactoring to separate program logic from view (propogating updates and writing them back to data store)

/**
 * Creates a window with controls for setting the visual properties of
 * tracks. These properties include renderer, visibility, color, position, and scale.
 * 
 * @author cbare
 */
public class TrackVisualPropertiesEditor {
	private static final Logger log = Logger.getLogger(TrackVisualPropertiesEditor.class);
	private TrackManager trackManager;
	private TrackSaver trackSaver;
	private JFrame frame;
	private JPanel buttonPanel;
	private ColorIcon colorIcon;
	private JButton colorButton;
	private JTextField top;
	private JTextField height;
	private JTextField min;
	private JTextField max;
	private JLabel trueMin, trueMax;
	private JComboBox rendererChooser;
	private JComboBox trackChooser;
	private JTextField name;
	private JCheckBox visible;
	private JTextField groups;
	private JComboBox overlayChooser;
	private boolean supressUpdateEvents;
	private ActionListenerSupport actionListeners = new ActionListenerSupport();
	private JTable attributesTable;
	private TrackAttributesTableModel attributesTableModel;
	private TrackComboBoxModel trackComboBoxModel;


//	public TrackVisualPropertiesEditor(TrackManager trackManager) {
//		this.trackManager = trackManager;
//		initGui();
//		setTrack(getSelectedTrack());
//		frame.setVisible(true);
//	}

	public TrackVisualPropertiesEditor(TrackManager trackManager, JFrame parentFrame) {
		this(trackManager, parentFrame, null);
	}

	public TrackVisualPropertiesEditor(TrackManager trackManager, JFrame parentFrame, UUID uuid) {
		this.trackManager = trackManager;
		initGui();

		if (uuid!=null)
			selectTrack(uuid);
		else
			setTrack(getSelectedTrack());

		Point p = parentFrame.getLocationOnScreen();
		if (p.x > frame.getWidth()) {
			frame.setLocation(p.x-frame.getWidth(), p.y);
		}
		else {
			frame.setLocationRelativeTo(parentFrame);
		}
		frame.setVisible(true);
	}


	// + nice layout
	// + matching renderers with appropriate data sources
	// + implement pairing and overlays
	// + update automatically
	//   sliders to update numeric values
	// + z-order
	// + correctly display track renderer default colors
	// + additional attributes
	// + don't open more than one TrackEditor


	/**
	 * @return the track selected in the trackChooser combo-box or null.
	 */
	private Track<? extends Feature> getSelectedTrack() {
		TrackWrapper wrapper = (TrackWrapper)trackChooser.getSelectedItem();
		if (wrapper == null) return null;
		return wrapper.getTrack();
	}

	private void initGui() {
		trackComboBoxModel = new TrackComboBoxModel(trackManager, false);
		trackChooser = new JComboBox(trackComboBoxModel);
		trackChooser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!supressUpdateEvents)
					setTrack(getSelectedTrack());
			}
		});

		name = new JTextField();
		name.addKeyListener(new EnterKeyListener());

		visible = new JCheckBox();
		visible.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTrack();
			}
		});

		// position
		top = new JTextField();
		top.addKeyListener(new ArrowKeyListener("top", top, true));

		height = new JTextField();
		height.addKeyListener(new ArrowKeyListener("height", height, false));

		// z-order
		JButton front = new JButton("\u21E4");
		JButton forward = new JButton("\u2190");
		JButton backward = new JButton("\u2192");
		JButton back = new JButton("\u21E5");

		front.setToolTipText("Bring to front");
		forward.setToolTipText("Bring forward");
		backward.setToolTipText("Send backward");
		back.setToolTipText("Send to back");

		front.setActionCommand("front");
		forward.setActionCommand("forward");
		backward.setActionCommand("backward");
		back.setActionCommand("back");

		// handle moving tracks forward or back in z-order.
		ActionListener zOrderActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Track<? extends Feature> track = getSelectedTrack();
				if ("front".equals(event.getActionCommand())) {
					trackManager.sendToFront(track);
				}
				else if ("forward".equals(event.getActionCommand())) {
					trackManager.forward(track);
				}
				else if ("backward".equals(event.getActionCommand())) {
					trackManager.back(track);
				}
				else if ("back".equals(event.getActionCommand())) {
					trackManager.sendToBack(track);
				}
				else {
					log.warn("unknown z-order action command: " + event.getActionCommand());
				}
				updateTrack();
			}
		};

		front.addActionListener(zOrderActionListener);
		forward.addActionListener(zOrderActionListener);
		backward.addActionListener(zOrderActionListener);
		back.addActionListener(zOrderActionListener);

		JToolBar zOrderToolbar = new JToolBar();
		zOrderToolbar.setOpaque(false);
		zOrderToolbar.setFloatable(false);
		zOrderToolbar.add(front);
		zOrderToolbar.add(forward);
		zOrderToolbar.add(backward);
		zOrderToolbar.add(back);

		// range limits
		min = new JTextField();
		min.addKeyListener(new ArrowKeyListener("rangeMin", min, false));
		max = new JTextField();
		max.addKeyListener(new ArrowKeyListener("rangeMax", max, false));
		trueMin = new JLabel();
		trueMax = new JLabel();

		// select track renderer
		rendererChooser = new JComboBox();
		rendererChooser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTrack();
			}
		});

		// color
		colorIcon = new ColorIcon(20,20);
		colorButton = new JButton(colorIcon);
		colorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Color originalColor = colorIcon.color==null ? Color.BLUE : colorIcon.color;
				Color result = ColorPicker.showDialog(frame, originalColor, true);
				if (result != null) {
					colorIcon.color = result;
					colorButton.repaint();
					updateTrack();
				}
			}
		});

		// grouping
		groups = new JTextField();

		overlayChooser = new JComboBox();
		overlayChooser.setEditable(true);
		overlayChooser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTrack();
			}
		});

		attributesTableModel = new TrackAttributesTableModel();
		attributesTable = new ETable(attributesTableModel);

		JButton addRow = new JButton("+");
		addRow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				attributesTableModel.addRow();
			}
		});
		JButton removeRows = new JButton("-");
		removeRows.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				attributesTableModel.removeRows(attributesTable.getSelectedRows());
			}
		});


		// layout

		JPanel propertyTab = new JPanel();
		propertyTab.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		propertyTab.add(new JLabel("Name:"), c);

		c.gridx = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		propertyTab.add(name, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		propertyTab.add(new JLabel("Visible:"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,2,2,2);
		propertyTab.add(visible, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		propertyTab.add(new JLabel("Z-order:"), c);

		c.gridx = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,2,2,2);
		propertyTab.add(zOrderToolbar, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		propertyTab.add(new JLabel("Renderer:"), c);

		c.gridx = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		propertyTab.add(rendererChooser, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 4;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(8,2,2,2);
		propertyTab.add(new JLabel("Track Position:"), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		propertyTab.add(new JLabel("Top:"), c);

		c.gridx = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		propertyTab.add(top, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		propertyTab.add(new JLabel("Height:"), c);

		c.gridx = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		propertyTab.add(height, c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 4;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(8,2,2,2);
		propertyTab.add(new JLabel("Range of Values:"), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		propertyTab.add(new JLabel("Min:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		propertyTab.add(min, c);

		c.gridx = 3;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		propertyTab.add(trueMin, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		propertyTab.add(new JLabel("Max:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		propertyTab.add(max, c);

		c.gridx = 3;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		propertyTab.add(trueMax, c);

		Box colorBox = new Box(BoxLayout.X_AXIS);
		colorBox.add(new JLabel("Color:"));
		colorBox.add(Box.createHorizontalStrut(4));
		colorBox.add(colorButton);
		colorBox.add(Box.createHorizontalStrut(4));

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 4;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(8,12,2,2);
		propertyTab.add(colorBox, c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 4;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(8,2,2,2);
		propertyTab.add(new JLabel("Track grouping:"), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(3,12,2,2);
		propertyTab.add(new JLabel("Overlay:"), c);

		c.gridx = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		propertyTab.add(overlayChooser, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(3,12,2,2);
		propertyTab.add(new JLabel("Groups:"), c);

		c.gridx = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		propertyTab.add(groups, c);

		Box addRemoveRowsBox = new Box(BoxLayout.Y_AXIS);
		addRemoveRowsBox.add(addRow);
		addRemoveRowsBox.add(removeRows);

		Box detailsTab = new Box(BoxLayout.X_AXIS);
		detailsTab.add(new JScrollPane(attributesTable));
		detailsTab.add(addRemoveRowsBox);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Track Properties", propertyTab);
		tabbedPane.add("Details", detailsTab);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(trackChooser);
		mainPanel.add(tabbedPane);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fireOkEvent();
			}
		});

		JButton updateButton = new JButton("Update");
		updateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTrack();
			}
		});

		buttonPanel = new JPanel();
		buttonPanel.add(updateButton);
		buttonPanel.add(okButton);

		frame = new JFrame("Track Visual Properties Editor");
		frame.setLayout(new BorderLayout());
		frame.add(mainPanel, BorderLayout.CENTER);
		frame.add(buttonPanel, BorderLayout.SOUTH);

		// map escape and command-w to cancel the dialog
		frame.getRootPane().getActionMap().put("close-window-on-escape", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		InputMap im = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		frame.pack();
	}

	// dependency
	/**
	 * Allows TrackVisualPropertiesEditor to save changes in attributes
	 */
	public void setTrackSaver(TrackSaver trackSaver) {
		this.trackSaver = trackSaver;
	}

	public void bringToFront() {
		frame.setVisible(true);
		frame.setVisible(true);
	}

	public void selectTrack(UUID uuid) {
		setTrack(trackManager.getTrack(uuid));
	}

	@SuppressWarnings("unchecked")
	public void setTrack(Track<? extends Feature> track) {
		if (track==null) {
			name.setText("");
			visible.setSelected(false);
			top.setText("");
			height.setText("");
			min.setText("");
			max.setText("");
			trueMin.setText("");
			trueMax.setText("");
			rendererChooser.removeAllItems();
			colorIcon.color = null;
			colorButton.repaint();
			groups.setText("");
			overlayChooser.removeAllItems();
			attributesTableModel.setTrack(null);
		}
		else {
			try {
				supressUpdateEvents = true;

				for (int i=0; i<trackChooser.getModel().getSize(); i++) {
					TrackWrapper wrapper = (TrackWrapper)trackChooser.getItemAt(i);
					if (track.getUuid().equals(wrapper.getTrack().getUuid())) {
						trackChooser.setSelectedItem(wrapper);
						break;
					}
				}

				name.setText(track.getName());
				visible.setSelected(track.getAttributes().getBoolean("visible", true));

				// position
				top.setText(track.getAttributes().getString("top"));
				height.setText(track.getAttributes().getString("height"));

				// range limits
				min.setText(track.getAttributes().getString("rangeMin"));
				max.setText(track.getAttributes().getString("rangeMax"));

				if (track instanceof Track.Quantitative) {
					min.setEnabled(true);
					max.setEnabled(true);
					Range range = ((Track.Quantitative<? extends Feature.Quantitative>)track).getRange();
					trueMin.setText(String.format("(%.2f)", range.min));
					trueMax.setText(String.format("(%.2f)", range.max));
					if (!track.getAttributes().containsKey("rangeMin")) {
						min.setText(String.format("%.2f", range.min));
					}
					if (!track.getAttributes().containsKey("rangeMax")) {
						max.setText(String.format("%.2f", range.max));
					}
				}
				else {
					trueMin.setText("");
					trueMax.setText("");
					min.setEnabled(false);
					max.setEnabled(false);
				}
		
				// select track renderer
				rendererChooser.removeAllItems();

				TrackRendererRegistry registry = trackManager.getTrackRendererRegistry();
				for (String rendererName: registry.getRenderersForTrack(track)) {
					rendererChooser.addItem(rendererName);
				}
				rendererChooser.setSelectedItem(track.getAttributes().getString("viewer"));

				// color
				colorIcon.color = track.getAttributes().getColor("color", null);
				if (colorIcon.color == null) {
					TrackRenderer renderer = trackManager.getRendererFor(track);
					if (renderer != null)
						colorIcon.color = renderer.getColor();
				}
				colorButton.repaint();

				// groups
				groups.setText(track.getAttributes().getString("groups"));

				overlayChooser.removeAllItems();
				for (String overlayGroup: trackManager.getOverlayGroups())
					overlayChooser.addItem(overlayGroup);
				overlayChooser.setSelectedItem(track.getAttributes().getString("overlay"));
				
				attributesTableModel.setTrack(track);
			}
			finally {
				supressUpdateEvents = false;
			}
		}
	}

	/**
	 * copy user's updates from UI form to track
	 */
	public void updateTrack() {
		if (supressUpdateEvents) return;
		Track<? extends Feature> track = getSelectedTrack();
		if (track==null) return;
		try {
			track.setName(name.getText().trim());
			track.getAttributes().put("visible", visible.isSelected());

			// position
			track.getAttributes().put("top", StringUtils.validateDouble(top.getText()));
			track.getAttributes().put("height", StringUtils.validateDouble(height.getText()));

			// range limits
			if (!StringUtils.isNullOrEmpty(min.getText()))
				track.getAttributes().put("rangeMin", StringUtils.validateDouble(min.getText()));
			if (!StringUtils.isNullOrEmpty(max.getText()))
				track.getAttributes().put("rangeMax", StringUtils.validateDouble(max.getText()));

			// select track renderer
			track.getAttributes().put("viewer", rendererChooser.getSelectedItem());

			// color
			if (colorIcon.color != null) {
				track.getAttributes().put("color", "0x" + Integer.toHexString(colorIcon.color.getRGB()));
			}

			// groups
			String g = groups.getText();
			if (g != null)
				g = g.trim();
			if (g != null && g.length() > 0)
				track.getAttributes().put("groups", g);
			else
				track.getAttributes().remove("groups");

			// TODO use a UID for each track, rather than assume unique names

			String overlayGroup = ((String)overlayChooser.getSelectedItem());
			if (overlayGroup != null)
				overlayGroup = overlayGroup.trim();
			if (overlayGroup != null && overlayGroup.length() > 0)
				track.getAttributes().put("overlay", overlayGroup);
			else
				track.getAttributes().remove("overlay");

			// if any tracks are overlaid on the selected track, update their info as well
			propagateUpdates(track);

			attributesTableModel.setTrack(track);

			fireUpdateEvent();
		}
		catch (Exception e) {
			e.printStackTrace();
			showErrorMessage("Validation error: " + e.getMessage());
		}
	}

	/**
	 * copy user's changes from the table UI to the track.
	 */
	public void updateTrackFromTable() {
		if (supressUpdateEvents) return;
		Track<? extends Feature> track = getSelectedTrack();
		// TODO does this work?
		setTrack(track);
		propagateUpdates(track);
		fireUpdateEvent();
	}

	// TODO should propagateUpdates move to TrackManager?

	/**
	 * Adjust tracks that inherit properties from the given track.
	 */
	private void propagateUpdates(Track<? extends Feature> track) {
		// maintain a stack of tracks whose dependents need adjusting
		LinkedList<Track<? extends Feature>> todo = new LinkedList<Track<? extends Feature>>();
		// since the graph we're traversing probably has cycles, we keep track of
		// tracks we're already adjusted. We don't adjust any track more than once
		LinkedList<Track<? extends Feature>> updated = new LinkedList<Track<? extends Feature>>();
		todo.add(track);
		updated.add(track);

		while (!todo.isEmpty()) {
			Track<? extends Feature> source = todo.removeFirst();

			// overlays
			String overlayId = source.getAttributes().getString("overlay");
			if (overlayId != null) {
				for (Track<? extends Feature> otherTrack: trackManager.getTracks()) {
					if (overlayId.equals(otherTrack.getAttributes().getString("overlay")) && !updated.contains(otherTrack)) {
						otherTrack.getAttributes().put("top", source.getAttributes().getString("top"));
						otherTrack.getAttributes().put("height", source.getAttributes().getString("height"));
						if (!StringUtils.isNullOrEmpty(min.getText()))
							otherTrack.getAttributes().put("rangeMin", StringUtils.validateDouble(min.getText()));
						if (!StringUtils.isNullOrEmpty(max.getText()))
							otherTrack.getAttributes().put("rangeMax", StringUtils.validateDouble(max.getText()));

						updated.add(otherTrack);
					}
				}
			}
		}
		log.info("propogated updates to " + updated.size() + " tracks.");
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.WARNING_MESSAGE);
	}

	// ---- ActionListenerSupport methods -------------------------------------

	public void addActionListener(ActionListener listener) {
		actionListeners.addActionListener(listener);
	}

	public void removeActionListener(ActionListener listener) {
		actionListeners.removeActionListener(listener);
	}
	
	private void cancel() {
		frame.setVisible(false);
		frame.dispose();
		actionListeners.fireActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "cancel", System.currentTimeMillis(), 0));
	}

	private void fireOkEvent() {
		trackManager.refresh();
		frame.setVisible(false);
		frame.dispose();

		// TODO DB stuff doesn't belong here!
		// write track attributes back to DB
		if (trackSaver != null) {
			for (Track<? extends Feature> track : trackManager.getTracks()) {
				trackSaver.updateTrack(track);
			}
		}

		actionListeners.fireActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "ok", System.currentTimeMillis(), 0));
	}

	private void fireUpdateEvent() {
		trackManager.refresh();
		actionListeners.fireActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "update", System.currentTimeMillis(), 0));
	}

	// ------------------------------------------------------------------------

	/**
	 * the purpose of this extension to ComboBoxModel is so we can clear
	 * the comboBox's contents and reload it and only generate one event.
	 */
	@SuppressWarnings("serial")
	class TrackComboBoxModel extends OpenDefaultComboBoxModel {
		private boolean hasEmptyOption;

		public TrackComboBoxModel(TrackManager trackManager, boolean hasEmptyOption) {
			this.hasEmptyOption = hasEmptyOption;
	        objects.ensureCapacity( trackManager.getTracks().size() );
            if (hasEmptyOption)
            	this.objects.add(new TrackWrapper());
			for (Track<? extends Feature> track : TrackUtils.sortedByName(trackManager.getTracks())) {
				this.objects.add(new TrackWrapper(track));
			}
	        if ( getSize() > 0 ) {
	            selectedObject = getElementAt( 0 );
	        }
		}

		public void reload() {
            objects.removeAllElements();
            if (hasEmptyOption)
            	this.objects.add(new TrackWrapper());
			for (Track<? extends Feature> track : TrackUtils.sortedByName(trackManager.getTracks())) {
				this.objects.add(new TrackWrapper(track));
			}
			this.fireContentsChanged(this, 0, this.getSize());
		}

		public void reloadExcept(Track<? extends Feature> exceptedTrack) {
            objects.removeAllElements();
            if (hasEmptyOption)
            	this.objects.add(new TrackWrapper());
			for (Track<? extends Feature> track : TrackUtils.sortedByName(trackManager.getTracks())) {
				if (!track.equals(exceptedTrack))
					this.objects.add(new TrackWrapper(track));
			}
			this.fireContentsChanged(this, 0, this.getSize());
		}
	}

	/**
	 * Wrap a track so to return it's name from toString for use
	 * in a ComboBox.
	 */
	static class TrackWrapper {
		private Track<? extends Feature> track;

		public TrackWrapper() {}

		public TrackWrapper(Track<? extends Feature> track) {
			this.track = track;
		}

		public Track<? extends Feature> getTrack() {
			return track;
		}

		public String toString() {
			return track==null ? "" : track.getName();
		}

        @Override
        public int hashCode() {
            return ((track == null) ? 0 : track.hashCode());
        }

		@Override
		public boolean equals(Object object) {
            if (this == object)
                return true;
		    if (object==null)
		        return false;
		    if (object instanceof TrackWrapper) {
		        TrackWrapper other = (TrackWrapper)object;
		        if (this.track==null || other.track==null)
		            return this.track==null && other.track==null;
		        return this.track.equals(other.getTrack());
		    }
		    return false;
		}
	}

	// ------------------------------------------------------------------------
	private class EnterKeyListener implements KeyListener {

		public void keyPressed(KeyEvent event) {
			if (event.getKeyCode() == KeyEvent.VK_ENTER) {
				updateTrack();
			}
		}

		public void keyReleased(KeyEvent event) {}
		public void keyTyped(KeyEvent event) {}
	}

	private class ArrowKeyListener implements KeyListener {
		String property;
		JTextField textField;
		int invert = 1;

		@SuppressWarnings("unused")
		public ArrowKeyListener(String property, JTextField textField) {
			this.property = property;
			this.textField = textField;
		}

		public ArrowKeyListener(String property, JTextField textField, boolean invert) {
			this.property = property;
			this.invert = invert ? -1 : 1;
			this.textField = textField;
		}

		public void keyPressed(KeyEvent event) {
			if (event.getKeyCode() == KeyEvent.VK_ENTER) {
				updateTrack();
			}
			else if (event.getKeyCode() == KeyEvent.VK_DOWN) {
				Track<? extends Feature> track = getSelectedTrack();
				double value;
				try {
					value = Double.parseDouble(textField.getText());
				}
				catch (NumberFormatException e) {
					if (track==null)
						value=0.0;
					else
						value = track.getAttributes().getDouble(property, 0.0);
				}
				value -= invert * (((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) > 0) ? 0.10 : 0.01);
				textField.setText(String.format("%.2f",value));
				updateTrack();
			}
			else if (event.getKeyCode() == KeyEvent.VK_UP) {
				Track<? extends Feature> track = getSelectedTrack();
				double value;
				try {
					value = Double.parseDouble(textField.getText());
				}
				catch (NumberFormatException e) {
					if (track==null)
						value=0.0;
					else
						value = track.getAttributes().getDouble(property, 0.0);
				}
				value += invert * (((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) > 0) ? 0.10 : 0.01);
				textField.setText(String.format("%.2f",value));
				updateTrack();
			}
		}

		public void keyReleased(KeyEvent event) {}
		public void keyTyped(KeyEvent event) {}
	}

	
	@SuppressWarnings("serial")
	class TrackAttributesTableModel extends AbstractTableModel {
		Track<? extends Feature> track;
		List<String> keys = new ArrayList<String>();

		public void setTrack(Track<? extends Feature> track) {
			this.track = track;
			keys.clear();
			if (track != null) {
				keys.addAll(track.getAttributes().keySet());
				Collections.sort(keys);
			}
			fireTableDataChanged();
		}

		public int getColumnCount() {
			return 2;
		}

		public int getRowCount() {
			return keys.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			String key = keys.get(rowIndex);
			if (columnIndex==0) {
				return StringUtils.nullToEmptyString(key);
			}
			else if (columnIndex==1) {
				return track.getAttributes().getString(key, "");
			}
			else
				return null;
		}

		public void setValueAt(Object value, int rowIndex,	int columnIndex) {
			String key = keys.get(rowIndex);
			if (columnIndex==1) {
				track.getAttributes().put(key, String.valueOf(value));
				setTrack(track);
				updateTrackFromTable();
			}
			else if (columnIndex==0) {
				if (key!=null) {
					String existingValue = track.getAttributes().getString(key);
					track.getAttributes().remove(key);
					track.getAttributes().put(String.valueOf(value), existingValue);
				}
				keys.set(rowIndex, String.valueOf(value));
			}
			else {
				log.warn("Something funny is going on w/ the track attributes table!");
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex==0)
				return "Property";
			else if (columnIndex==1)
				return "Value";
			else
				return "?";
		}

		public void addRow() {
			keys.add(null);
			fireTableRowsInserted(keys.size()-1, keys.size());
		}

		public void removeRows(int[] rows) {
			for (int row: rows) {
				track.getAttributes().remove(keys.get(row));
			}
			setTrack(track);
		}
	}
}
