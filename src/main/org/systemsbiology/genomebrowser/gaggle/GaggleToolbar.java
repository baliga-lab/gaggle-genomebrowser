package org.systemsbiology.genomebrowser.gaggle;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import static javax.swing.SwingConstants.VERTICAL;
import static javax.swing.SwingConstants.RIGHT;

import org.systemsbiology.gaggle.core.Boss;
import org.systemsbiology.gaggle.geese.common.GaggleConnectionListener;
import org.systemsbiology.gaggle.geese.common.GaggleUtil;
import org.systemsbiology.util.FileUtils;
import static org.systemsbiology.util.StringUtils.isNullOrEmpty;


@SuppressWarnings("unused")
public class GaggleToolbar extends JToolBar implements GaggleConnectionListener {
	public static final GaggleBroadcastData SELECTED_GENES = new GaggleBroadcastData("Selected Genes", null, "selected.genes");
	public static final String TITLE = "Gaggle Toolbar";
	private JComboBox messageChooser;
	private JComboBox targetChooser;
	private JButton broadcastButton;
	private JButton statusButton;
	private JButton showButton, hideButton;
	private Icon onIcon = FileUtils.getIconOrBlank("greenled.png");
	private Icon offIcon = FileUtils.getIconOrBlank("offled.png");


	public GaggleToolbar() {
		setBorder(BorderFactory.createRaisedBevelBorder());
		setMargin(new Insets(1,1,1,1));
		setFloatable(false);
		setVisible(false);

		statusButton = new JButton(offIcon);
		statusButton.setBorderPainted(false);

		messageChooser = new JComboBox() {
			// try to prevent the toolbar's layout from resizing the chooser
			// to be too fat (actually have a larger height than necessary).
			public Dimension getMaximumSize() {
				return new Dimension(Short.MAX_VALUE, super.getPreferredSize().height);
			}
		};
		targetChooser = new JComboBox() {
			// try to prevent the toolbar's layout from resizing the chooser
			// to be too fat (actually have a larger height than necessary).
			public Dimension getMaximumSize() {
				return new Dimension(Short.MAX_VALUE, super.getPreferredSize().height);
			}
		};
		showButton = new JButton(FileUtils.getIconOrBlank("arrow_up.png"));
		showButton.setBorderPainted(false);
		hideButton = new JButton(FileUtils.getIconOrBlank("arrow_down.png"));
		hideButton.setBorderPainted(false);
		broadcastButton = new JButton(new ImageIcon(GaggleToolbar.class.getResource("/icons/broadcast.png")));
		broadcastButton.setBorderPainted(false);

		statusButton.setToolTipText("Displays the state of the connection to the Gaggle Boss");
		messageChooser.setToolTipText("Select Gaggle data to be sent to the target application");
		targetChooser.setToolTipText("Select the Gaggle application to receive the broadcast");
		showButton.setToolTipText("Bring selected target goose to the front");
		hideButton.setToolTipText("Hide selected target goose");
		broadcastButton.setToolTipText("Send the selected message to the selected target");
		
		Box showHideBox = Box.createVerticalBox();
		showHideBox.add(showButton);
		showHideBox.add(hideButton);
		
		JToolBar showHideToolBar = new JToolBar(VERTICAL);
		showHideToolBar.setFloatable(false);
		showHideToolBar.add(showButton);
		showHideToolBar.add(hideButton);

		add(statusButton);
		add(new Separator(new Dimension(16,10)));
		add(new JLabel("Gaggle Data:", new ImageIcon(GaggleToolbar.class.getResource("/icons/data.png")), RIGHT));
		add(messageChooser);
		add(new Separator(new Dimension(16,10)));
		add(new JLabel("Target:", new ImageIcon(GaggleToolbar.class.getResource("/icons/target.png")), RIGHT));
		add(targetChooser);
		add(showHideToolBar);
		add(Box.createHorizontalStrut(4));
		add(broadcastButton);
		add(new Separator(new Dimension(16,10)));

		clearBroadcastMenu();
	}

	public void clearBroadcastMenu() {
		DefaultComboBoxModel model = (DefaultComboBoxModel)messageChooser.getModel();
		model.removeAllElements();
	}

	public void addGaggleData(String name, UUID trackUuid) {
		DefaultComboBoxModel model = (DefaultComboBoxModel)messageChooser.getModel();
		model.addElement(new GaggleBroadcastData(name, trackUuid));
	}

	public void addGaggleData(GaggleBroadcastData data) {
		DefaultComboBoxModel model = (DefaultComboBoxModel)messageChooser.getModel();
		model.addElement(data);
	}

	public GaggleBroadcastData getSelectedBroadcastData() {
		return (GaggleBroadcastData)messageChooser.getSelectedItem();
	}

	public void addBroadcastActionListener(ActionListener action) {
		broadcastButton.addActionListener(action);
	}

	public void addShowActionListener(ActionListener actionListener) {
		showButton.addActionListener(actionListener);
	}

	public void addHideActionListener(ActionListener actionListener) {
		hideButton.addActionListener(actionListener);
	}

	public void addStatusButtonActionListener(ActionListener actionListener) {
		statusButton.addActionListener(actionListener);
	}

	public String getTarget() {
		if (!isNullOrEmpty((String)targetChooser.getSelectedItem())) {
			return (String)targetChooser.getSelectedItem();
		}
		else {
			return null;
		}
	}
	

	public void setConnected(boolean connected, Boss boss) {
		if (connected) {
			statusButton.setIcon(onIcon);
		}
		else {
			statusButton.setIcon(offIcon);
		}
		broadcastButton.setEnabled(connected);
		targetChooser.setEnabled(connected);
		messageChooser.setEnabled(connected);
		showButton.setEnabled(connected);
		hideButton.setEnabled(connected);
	}

	public void update(String myGooseName, String[] gooseNames) {
		GaggleUtil.updateGooseChooser(targetChooser, myGooseName, gooseNames);
	}

	/**
	 * Represent the various types of data in the list of possible
	 * broadcasts. SELECTED_GENES is a special case.
	 */
	public static class GaggleBroadcastData {
		final String name;
		final UUID trackUuid;
		final String type;

		public GaggleBroadcastData(String name, UUID trackUuid) {
			this.name = name;
			this.trackUuid = trackUuid;
			this.type = "track.data";
		}

		public GaggleBroadcastData(String name, String type) {
			this.name = name;
			this.trackUuid = null;
			this.type = type;
		}

		public GaggleBroadcastData(String name, UUID trackUuid, String type) {
			this.name = name;
			this.trackUuid = trackUuid;
			this.type = type;
		}

		public String toString() {
			return name;
		}
	}
}
