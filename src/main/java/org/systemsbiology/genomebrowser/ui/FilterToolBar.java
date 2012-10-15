package org.systemsbiology.genomebrowser.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.UUID;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.event.Event;
import org.systemsbiology.genomebrowser.app.ExternalAPI;
import org.systemsbiology.genomebrowser.app.Plugin;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.util.FeatureUtils;
import org.systemsbiology.genomebrowser.util.TrackUtils;
import org.systemsbiology.util.FileUtils;

// TODO This is a hack
// Need to have a generalized concept of a filter. This probably requires changes to how
// tracks work, maybe a track iterator is needed that would be aware of any filters applied,
// so filtering could be independent of renderer.

// Right now, filtering is implemented ONLY in two specific renderers:
//   * TriangleMarkerPvalueRenderer
//   * VerticalLineRenderer



/**
 * The filter toolbar allows the user to adjust a cutoff threshold for
 * quantitative tracks. Only features whose value exceeds the threshold
 * will be rendered.
 * 
 * Renderers are (conceptually) presented with a list of features. We'd
 * probably want to filter as a step outside the renderer. I believe this
 * sort of thing is discussed in the Jeffery Heer design patterns for
 * visualization paper. 
 */
public class FilterToolBar extends JToolBar implements Plugin {
	private static final Logger log = Logger.getLogger(FilterToolBar.class);
	public static final String TITLE = "Filter Toolbar";
	private JComboBox trackChooser;
	private JComboBox fieldChooser;
	private JSlider slider;
	private JTextField filterTextField;
	private ExternalAPI api;
	private ToggleFilterToolBarAction action;
	private boolean suppressTrackChooserEvents;
	private boolean suppressFieldChooserEvents;
	private JButton applyButton;
	private boolean applied = false;


	public FilterToolBar() {
		setBorder(BorderFactory.createRaisedBevelBorder());
		setMargin(new Insets(1,1,1,1));
		setFloatable(false);
		setVisible(false);
		setLayout(new BorderLayout());

		trackChooser = new JComboBox();
		trackChooser.setToolTipText("Select the track or group of tracks you'd like to filter");
		trackChooser.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (!suppressTrackChooserEvents)
					selectTrack((TrackListItem)e.getItem());
			}
		});

		fieldChooser = new JComboBox();
		fieldChooser.setToolTipText("Select the field whose value you'd like to filter on");
		fieldChooser.addItem("value");
		fieldChooser.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (!suppressFieldChooserEvents)
					selectField(e.getItem().toString());
			}
		});
		
		filterTextField = new JTextField(3);
		filterTextField.setAlignmentX(RIGHT_ALIGNMENT);

		slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		Dimension d = slider.getSize();
		slider.setSize(200,d.height);
		slider.setMajorTickSpacing(20);
		slider.setMinorTickSpacing(10);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				filterTextField.setText(String.format("%3.2f", scaleSliderValue(slider.getValue())));
				if (!slider.getValueIsAdjusting() && applied)
					update();
			}
		});

		applyButton = new JButton("Apply");
		applyButton.setToolTipText("Apply filter, update tracks and redraw");
		applyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				update();
			}
		});

		JButton removeFilterButton = new JButton("Remove Filter");
		removeFilterButton.setToolTipText("Remove filter");
		removeFilterButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeFilter();
			}
		});

		JButton removeAllButton = new JButton("Remove All");
		removeAllButton.setToolTipText("Remove all filters on all tracks");
		removeAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeAllFilters();
			}
		});

		JButton button = new JButton(FileUtils.getIconOrBlank("filter.icon.32.png"));
		button.setBorderPainted(false);
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setToolTipText("Hide filter toolbar");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dismiss();
			}
		});

		JPanel westPanel = new JPanel();
		westPanel.add(button);
		westPanel.add(new Separator());
		westPanel.add(trackChooser);
		westPanel.add(fieldChooser);
		
		JPanel eastPanel = new JPanel();
		eastPanel.add(filterTextField);
		eastPanel.add(applyButton);
		eastPanel.add(removeFilterButton);
		eastPanel.add(removeAllButton);

		add(westPanel, BorderLayout.WEST);
		add(slider, BorderLayout.CENTER);
		add(eastPanel, BorderLayout.EAST);
	}

	protected void adjustAppliedButton() {
		if (applied)
			applyButton.setForeground(Color.DARK_GRAY);
		else
			applyButton.setForeground(Color.BLACK);
	}

	protected void selectField(String field) {
		TrackListItem item = (TrackListItem)trackChooser.getSelectedItem();
		if (item==null) return;
		if (item.group) {
			List<Track<Feature>> tracks = TrackUtils.getTracksByGroup(item.name, api.getDataset().getTracks());
			double filterPercent = 0.0;
			double pvalueCutoff = 0.0;
			int value=0, pvalue=0;
			for (Track<Feature> track : tracks) {
				if (track.getAttributes().containsKey("filter.percent")) {
					value++;
					filterPercent = Math.max(filterPercent, track.getAttributes().getDouble("filter.percent", filterPercent));
				}
				if (track.getAttributes().containsKey("p.value.cutoff")) {
					pvalue++;
					pvalueCutoff = Math.max(pvalueCutoff, track.getAttributes().getDouble("p.value.cutoff", pvalueCutoff));
				}
			}
			if ("value".equals(field)) {
				slider.setValue(filterPercentToSliderValue(filterPercent));
				if (value==tracks.size())
					applied = true;
			}
			else if ("p value".equals(field)) {
				slider.setValue(filterPercentToSliderValue(pvalueCutoff));
				if (pvalue==tracks.size())
					applied = true;
			}
			else {
				slider.setValue(0);
			}
		}
		else {
			Track<? extends Feature> track = api.getDataset().getTrack(item.name);
			if (track != null) {
				if (track.getAttributes().containsKey("filter.percent") && "value".equals(field)) {
					double filterPercent = track.getAttributes().getDouble("filter.percent", 0.0);
					slider.setValue(filterPercentToSliderValue(filterPercent));
					applied = true;
				}
				else if (track.getAttributes().containsKey("p.value.cutoff") && "p value".equals(field)) {
					fieldChooser.setSelectedItem("p value");
					double pValueCutoff = track.getAttributes().getDouble("p.value.cutoff", 0.0);
					slider.setValue(filterPercentToSliderValue(pValueCutoff));
					applied = true;
				}
			}
		}
		adjustAppliedButton();
	}
	
	protected void selectTrack(TrackListItem item) {
		fieldChooser.removeAllItems();
		if (item==null) return;
		if (item.group) {
			// get tracks by group
			// if there are quantitative tracks, add value to field chooser
			// if there are QuantitativePvalue, add p value
			boolean hasValue = false;
			boolean hasPvalue = false;
			List<Track<Feature>> tracks = TrackUtils.getTracksByGroup(item.name, api.getDataset().getTracks());
			double filterPercent = 0.0;
			double pvalueCutoff = 0.0;
			int value=0, pvalue=0;
			for (Track<Feature> track : tracks) {
				if (track instanceof Track.Quantitative) {
					hasValue = true;
					if (Feature.QuantitativePvalue.class.isAssignableFrom(FeatureUtils.getFeatureClass(track)))
						hasPvalue = true;
				}
				if (track.getAttributes().containsKey("filter.percent")) {
					hasValue = true;
					value++;
					filterPercent = Math.max(filterPercent, track.getAttributes().getDouble("filter.percent", filterPercent));
				}
				if (track.getAttributes().containsKey("p.value.cutoff")) {
					hasPvalue = true;
					pvalue++;
					pvalueCutoff = Math.max(pvalueCutoff, track.getAttributes().getDouble("p.value.cutoff", pvalueCutoff));
				}
			}
			if (hasValue) fieldChooser.addItem("value");
			if (hasPvalue) fieldChooser.addItem("p value");
			if (value==tracks.size()) {
				slider.setValue(filterPercentToSliderValue(filterPercent));
				fieldChooser.setSelectedItem("value");
				applied = true;
			}
			else if (pvalue==tracks.size()) {
				slider.setValue(filterPercentToSliderValue(pvalueCutoff));
				fieldChooser.setSelectedItem("p value");
				applied = true;
			}
			else {
				slider.setValue(0);
			}
		}
		else {
			Track<? extends Feature> track = api.getDataset().getTrack(item.name);
			if (track != null) {
				if (track instanceof Track.Quantitative) {
					fieldChooser.addItem("value");
					if (Feature.QuantitativePvalue.class.isAssignableFrom(FeatureUtils.getFeatureClass(track))) {
						fieldChooser.addItem("p value");
					}
				}
				if (track.getAttributes().containsKey("filter.percent")) {
					fieldChooser.setSelectedItem("value");
					double filterPercent = track.getAttributes().getDouble("filter.percent", 0.0);
					slider.setValue((int)Math.round(filterPercent*100.0));
					applied = true;
				}
				else if (track.getAttributes().containsKey("p.value.cutoff")) {
					fieldChooser.setSelectedItem("p value");
					double pValueCutoff = track.getAttributes().getDouble("p.value.cutoff", 0.0);
					slider.setValue((int)Math.round(pValueCutoff*100.0));
					applied = true;
				}
			}
		}
		adjustAppliedButton();
	}

	public void update() {
		TrackListItem item = (TrackListItem)trackChooser.getSelectedItem();
		log.info("filter toolbar updating: " + item);
		if (item==null) return;
		if (item.group) {
			List<Track<Feature>> tracks = TrackUtils.getTracksByGroup(item.name, api.getDataset().getTracks());
			log.debug("updating tracks: " + TrackUtils.toString(tracks));
			String key = "filter.percent";
			if ("p value".equals(fieldChooser.getSelectedItem()))
				key = "p.value.cutoff";
			double filterPercent = ((double)slider.getValue())/100.0;
			for (Track<Feature> track : tracks) {
				track.getAttributes().put(key, filterPercent);
				api.updateTrack(track);
			}
		}
		else {
			Track<Feature> track = api.getDataset().getTrack(item.name);
			if (track ==null) return;
			String key = "filter.percent";
			if ("p value".equals(fieldChooser.getSelectedItem()))
				key = "p.value.cutoff";
			track.getAttributes().put(key, ((double)slider.getValue())/100.0);
			api.updateTrack(track);
		}
		applied = true;
		adjustAppliedButton();
		api.refresh();
	}

	public void removeFilter() {
		TrackListItem item = (TrackListItem)trackChooser.getSelectedItem();
		if (item==null) return;
		if (item.group) {
			List<Track<Feature>> tracks = TrackUtils.getTracksByGroup(item.name, api.getDataset().getTracks());
			for (Track<Feature> track : tracks) {
				if ("value".equals(fieldChooser.getSelectedItem())) {
					track.getAttributes().remove("filter.percent");
				}
				else if ("p value".equals(fieldChooser.getSelectedItem())) {
					track.getAttributes().remove("p.value.cutoff");
				}
				api.updateTrack(track);
			}
		}
		else {
			Track<Feature> track = api.getDataset().getTrack(item.name);
			if (track ==null) return;
			if ("value".equals(fieldChooser.getSelectedItem())) {
				track.getAttributes().remove("filter.percent");
			}
			else if ("p value".equals(fieldChooser.getSelectedItem())) {
				track.getAttributes().remove("p.value.cutoff");
			}
			api.updateTrack(track);
		}
		applied = false;
		adjustAppliedButton();
		api.refresh();
	}

	public void removeAllFilters() {
		for (Track<Feature> track : api.getDataset().getTracks()) {
			if (track.getAttributes().containsKey("filter.percent") || track.getAttributes().containsKey("p.value.cutoff")) {
				track.getAttributes().remove("filter.percent");
				track.getAttributes().remove("p.value.cutoff");
				api.updateTrack(track);
			}
		}
		applied = false;
		adjustAppliedButton();
		api.refresh();
	}

	protected double scaleSliderValue(int sliderValue) {
		return ( (double)sliderValue/100.0 );
	}
	
	protected int filterPercentToSliderValue(double filterPercent) {
		return (int)Math.round(filterPercent * 100.0);
	}

	public void dismiss() {
		action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dismiss"));
	}

	public void setExternalApi(ExternalAPI api) {
		this.api = api;
	}

	public void init() {
		api.addEventListener(this);
		action = new ToggleFilterToolBarAction(this, api);
		api.addToolbar(TITLE, this, action);
	}

	public void setDataset(Dataset dataset) {
		try {
			suppressTrackChooserEvents = true;
			trackChooser.removeAllItems();
			// add an entry for each quantitative track
			for (Track<Feature> track : dataset.getTracks()) {
				// only broadcast quantitative tracks, for now.
				if (track instanceof Track.Quantitative)
					trackChooser.addItem(new TrackListItem(track.getName(), track.getUuid(), false));
			}
			for (String group : TrackUtils.getGroups(dataset.getTracks())) {
				trackChooser.addItem(new TrackListItem(group, null, true));
			}
		}
		finally {
			suppressTrackChooserEvents = false;
		}
	}

	public void receiveEvent(Event event) {
		if ("set dataset".equals(event.getAction())) {
			setDataset((Dataset)event.getData());
		}
	}

	private static class TrackListItem {
		final String name;
		@SuppressWarnings("unused")
		final UUID uuid;
		final boolean group;
		public TrackListItem(String name, UUID uuid, boolean group) {
			this.name = name;
			this.uuid = uuid;
			this.group = group;
		}
		public String toString() {
			if (group) return name + " (group)";
			return name;
		}
	}

	public static class ToggleFilterToolBarAction extends AbstractAction {
		ExternalAPI api;
		FilterToolBar toolbar;

		public ToggleFilterToolBarAction(FilterToolBar toolbar, ExternalAPI api) {
			super("Show Filter Toolbar");
			this.api = api;
			this.toolbar = toolbar;
			putValue(AbstractAction.SHORT_DESCRIPTION, "Show or hide the Filter Toolbar.");
//			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() + InputEvent.SHIFT_MASK));
			putValue(AbstractAction.SMALL_ICON, FileUtils.getIconOrBlank("filter.16.png"));
		}

		public void actionPerformed(ActionEvent e) {
			boolean visible = !toolbar.isVisible();
			api.setVisibleToolbar(FilterToolBar.TITLE, visible);
			putValue(AbstractAction.NAME, getName(visible));
		}

		private String getName(boolean visible) {
			if (visible) {
				return "Hide Filter Toolbar";
			}
			else {
				return "Show Filter Toolbar";
			}
		}
	}
}
