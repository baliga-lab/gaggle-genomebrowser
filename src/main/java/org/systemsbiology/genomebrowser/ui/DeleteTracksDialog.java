package org.systemsbiology.genomebrowser.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.swing.SwingGadgets;


public class DeleteTracksDialog extends JDialog {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>Delete Tracks</h1>" +
	"<p>Note that there is no <i>undo</i> so deleting tracks is permanent. Select" +
	"the tracks to be deleted and click <i>OK</i>.</p>" +
	"</body></html>";

	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();

	private DefaultListModel trackListModel;

	private JList trackList;
	public DeleteTracksDialog(JFrame parent) {
		super(parent, "Delete Tracks", true);
		initGui();
		setLocationRelativeTo(parent);
	}

	public DeleteTracksDialog(JDialog parent) {
		super(parent, "Load Coordinate Mapping", true);
		initGui();
		setLocationRelativeTo(parent);
	}

	private void initGui() {
		// map escape and command-w to cancel the dialog
		this.getRootPane().getActionMap().put("close-window-on-escape", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);
		instructions.setPreferredSize(new Dimension(405, 90));

		trackListModel = new DefaultListModel();
		trackList = new JList(trackListModel);
		JScrollPane trackScrollPane = new JScrollPane(trackList);
		trackScrollPane.setPreferredSize(new Dimension(405, 260));

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(8,8,12,8);
		this.add(instructions, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(8,12,8,8);
		this.add(trackScrollPane, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,24,2);
		this.add(buttonPanel, c);

		pack();
	}

	public void setTracks(List<Track<? extends Feature>> tracks) {
		trackListModel.clear();

		for (Track<? extends Feature> track: tracks) {
			trackListModel.addElement(new TrackListItem(track.getName(), track.getUuid()));
		}
	}

	class TrackListItem {
		final String name;
		final UUID uuid;
		public TrackListItem(String name, UUID uuid) {
			this.name = name;
			this.uuid = uuid;
		}
		public String toString() {
			return name;
		}
	}

	public void close() {
		this.setVisible(false);
		this.dispose();
	}

	public void cancel() {
		close();
		for (DialogListener listener: listeners) {
			listener.cancel();
		}
	}

	public void ok() {
		List<UUID> uuids = new ArrayList<UUID>();
		for (Object item: trackList.getSelectedValues()) {
			uuids.add(((TrackListItem)item).uuid);
		}
		close();
		for (DialogListener listener: listeners) {
			listener.ok("delete", uuids);
		}
	}

	public void addDialogListener(DialogListener listener) {
		listeners.add(listener);
	}

	public void removeDialogListener(DialogListener listener) {
		listeners.remove(listener);
	}
}
