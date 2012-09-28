package org.systemsbiology.genomebrowser.ui.importtrackwizard;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.ui.ColorIcon;
import org.systemsbiology.util.StringUtils;
import org.systemsbiology.util.swing.SwingGadgets;

import com.bric.swing.ColorPicker;


public class TrackAttributesPanel extends JPanel implements WizardPanel {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(TrackAttributesPanel.class);
	private JTextField trackNameTextField;
	private WizardMainWindow parent;
	private ImportTrackWizard wiz;
	private ColorIcon colorIcon;
	private JButton colorButton;
	private JComboBox rendererChooser;
	private JComboBox overlayChooser;

	private static String INSTRUCTIONS_HTML = 
		"<html><body>" +
		"<h1>Visual Properties</h1>" +
		"<p>Now, set a few visual properties for the new track. The <b>Track Editor</b>" +
		"can be used to fully control track visual properties. " +
		"<a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/import/help/track_visual_properties/\">Help</a>.</p>" +
		"</body></html>";


	public TrackAttributesPanel(WizardMainWindow parent, ImportTrackWizard wiz) {
		this.parent = parent;
		this.wiz = wiz;
		initUI();
	}

	private void initUI() {
		setOpaque(false);

		// create important UI controls
		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);

		trackNameTextField = new JTextField();

		colorIcon = new ColorIcon(20,20);
		colorIcon.color = new Color(0xC8336699, true);
		colorButton = new JButton(colorIcon);
		colorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Color originalColor = colorIcon.color==null ? Color.BLUE : colorIcon.color;
				Color result = ColorPicker.showDialog(parent, originalColor, true);
				if (result != null) {
					colorIcon.color = result;
					colorButton.repaint();
				}
			}
		});

		rendererChooser = new JComboBox();
		overlayChooser = new JComboBox();
		overlayChooser.setEditable(true);

		// create panel's lay out
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(8,8,12,8);
		add(instructions, c);

		c.gridy = 1;
		c.gridx = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 0;
		c.insets = new Insets(0,16,0,8);
		add(new JLabel("Track Name:"), c);

		c.gridy++;
		add(new JLabel("Color:"), c);

		c.gridy++;
		add(new JLabel("Renderer:"), c);

		c.gridy++;
		add(new JLabel("Overlay:"), c);

		c.gridx = 1;
		c.gridy = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1;
		c.insets = new Insets(0,0,0,16);
		c.fill = GridBagConstraints.HORIZONTAL;
		add(trackNameTextField, c);

		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		add(colorButton, c);

		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(rendererChooser, c);

		c.gridy++;
		add(overlayChooser, c);

		// checkbox to autoposition track?
	}

	public boolean getEnableDone() {
		return false;
	}

	public boolean getEnableNext() {
		return !StringUtils.isNullOrEmpty(trackNameTextField.getText());
	}

	public boolean getEnableBack() {
		return true;
	}

	public void onLoad() {
		if (StringUtils.isNullOrEmpty(trackNameTextField.getText()))
			trackNameTextField.setText(wiz.getTrackName());
		populateRendererChooser(wiz.getTrackType());
		parent.updateStatus();
	}

	/**
	 * given the track type, set the available renderers
	 */
	private void populateRendererChooser(String trackType) {
		Object selected = rendererChooser.getSelectedItem();
		rendererChooser.removeAllItems();
		List<String> renderers = wiz.getRenderersForTrackType(trackType);
		for (String name : renderers) {
			rendererChooser.addItem(name);
		}
		if (selected != null)
			rendererChooser.setSelectedItem(selected);
		else
			rendererChooser.setSelectedItem("Scaling");
	}

	public void onUnload() {
		wiz.setTrackName(trackNameTextField.getText().trim());
		wiz.addTrackAttribute("color", colorIcon.color);
		wiz.addTrackAttribute("viewer", (String)rendererChooser.getSelectedItem());
		// overlay is optional
		if (StringUtils.isNullOrEmpty((String)overlayChooser.getSelectedItem()))
			wiz.removeTrackAttribute("overlay");
		else
			wiz.addTrackAttribute("overlay", (String)overlayChooser.getSelectedItem());
	}

	public void windowGainedFocus() {
		
	}
}
