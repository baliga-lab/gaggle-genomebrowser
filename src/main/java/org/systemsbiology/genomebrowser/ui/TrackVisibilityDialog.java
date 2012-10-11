package org.systemsbiology.genomebrowser.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import org.systemsbiology.genomebrowser.app.Event;
import org.systemsbiology.genomebrowser.app.EventListener;
import org.systemsbiology.genomebrowser.app.EventSupport;
import org.systemsbiology.genomebrowser.model.BasicTrack;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.util.StringUtils;
import org.systemsbiology.util.swing.SwingGadgets;


public class TrackVisibilityDialog extends JDialog {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>Track Visibility</h1>" +
	"<p>Toggle track visibility... instructions...</p>" +
	"</body></html>";
	private EventSupport eventSupport = new EventSupport();
	private List<Track<Feature>> tracks;


	public TrackVisibilityDialog(JFrame parent, List<Track<Feature>> tracks) {
		super(parent, "Track Visibility", false);
		this.tracks = tracks;
		initGui();
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
		instructions.setPreferredSize(new Dimension(420,100));

		JPanel tracksTab = new JPanel();
		JTable trackTable = new JTable(new TrackTableModel(), new TrackTableColumnModel());
		tracksTab.add(new JScrollPane(trackTable));


		JPanel groupsTab = new JPanel();
		JTable groupTable = new JTable(new GroupTableModel("groups"), new TrackTableColumnModel());
		groupsTab.add(new JScrollPane(groupTable));

		JPanel overlaysTab = new JPanel();
		JTable overlaysTable = new JTable(new GroupTableModel("overlay"), new TrackTableColumnModel());
		overlaysTab.add(new JScrollPane(overlaysTable));


		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Tracks", tracksTab);
		tabbedPane.add("Groups", groupsTab);
		tabbedPane.add("Overlays", overlaysTab);

		JButton okButton = new JButton("Dismiss");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);

		setLayout(new BorderLayout());
		add(instructions, BorderLayout.NORTH);
		add(tabbedPane, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		pack();
	}


	public void close() {
		this.setVisible(false);
		this.dispose();
	}

	protected void cancel() {
		// TODO restore old visibility settings?
		eventSupport.fireEvent(this, "cancel");
	}

	private void ok() {
		eventSupport.fireEvent(this, "ok");
	}

	private void update() {
		eventSupport.fireEvent(this, "update");
	}

	public void addEventListener(EventListener listener) {
		eventSupport.addEventListener(listener);
	}

	public void removeEventListener(EventListener listener) {
		eventSupport.removeEventListener(listener);
	}

	private class TrackTableModel extends AbstractTableModel {

		public int getColumnCount() {
			return 2;
		}

		public int getRowCount() {
			return tracks.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex==0) {
				return tracks.get(rowIndex).getAttributes().getBoolean("visible", true);
			}
			else if (columnIndex==1) {
				return tracks.get(rowIndex).getName();
			}
			else {
				return "??";
			}
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (columnIndex==0)
				tracks.get(rowIndex).getAttributes().put("visible", value);
			update();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0:
				return Boolean.class;
			case 1:
				return String.class;
			default:
				return Object.class;
			}
		}
		
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex==0;
		}
	}

	private static class TrackTableColumnModel extends DefaultTableColumnModel {
		public TrackTableColumnModel() {
			TableColumn col = new TableColumn(0);
			col.setHeaderValue("Visible");
			col.setPreferredWidth(50);
			col.setMinWidth(50);
			this.addColumn(col);

			col = new TableColumn(1);
			col.setHeaderValue("Track Name");
			col.setPreferredWidth(500);
			this.addColumn(col);
		}
	}


	private class GroupTableModel extends AbstractTableModel {
		private List<String> groups = new ArrayList<String>();
		private String attributeName;

		public GroupTableModel(String attributeName) {
			this.attributeName = attributeName;
			Set<String> groups = new TreeSet<String>();
			for (Track<? extends Feature> track : tracks) {
				String groupList = track.getAttributes().getString(attributeName);
				if (groupList!=null) {
					String[] groupArray = groupList.split(",");
					for (String group : groupArray) {
						groups.add(group.trim());
					}
				}
			}
			this.groups.addAll(groups);
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex==0) {
				String group = groups.get(rowIndex);
				int t=0, f=0;
				for (Track<Feature> track : tracks) {
					if (isMember(track, group)) {
						if (track.getAttributes().getBoolean("visible", true))
							t++;
						else
							f++;
					}
				}
				if (t==0 && f > 0) {
					return false;
				}
				if (f==0 && t > 0) {
					return true;
				}
				return null;
			}
			else {
				return groups.get(rowIndex);
			}
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (columnIndex==0) {
				String group = groups.get(rowIndex);
				for (Track<Feature> track : tracks) {
					if (isMember(track, group)) {
						track.getAttributes().put("visible", value);
					}
				}
				update();
			}
		}

		private boolean isMember(Track<Feature> track, String group) {
			String groups = track.getAttributes().getString(attributeName);
			if (groups==null) return false;
			return StringUtils.in(group, StringUtils.trim(groups.split(",")));
		}

		public int getColumnCount() {
			return 2;
		}

		public int getRowCount() {
			return groups.size();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0:
				return Boolean.class;
			case 1:
				return String.class;
			default:
				return super.getColumnClass(columnIndex);
			}
		}
		
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex==0;
		}
	}

	public static void main(String[] args) {
		final JFrame frame = new JFrame("test");
		frame.pack();
		frame.setVisible(true);
		
		BasicTrack<Feature> t1, t2, t3, t4;
		List<Track<Feature>> tracks = new ArrayList<Track<Feature>>();
		tracks.add(t1 = new BasicTrack<Feature>(UUID.randomUUID(), "asdf"));
		tracks.add(t2 = new BasicTrack<Feature>(UUID.randomUUID(), "qwer"));
		tracks.add(t3 = new BasicTrack<Feature>(UUID.randomUUID(), "zzzx"));
		tracks.add(t4 = new BasicTrack<Feature>(UUID.randomUUID(), "jklj"));
		
		t1.getAttributes().put("groups", "x, y, z");
		t2.getAttributes().put("groups", "x, a");
		t3.getAttributes().put("groups", "x, y");
		t4.getAttributes().put("groups", "y");

		TrackVisibilityDialog dialog = new TrackVisibilityDialog(frame, tracks);
		dialog.addEventListener(new EventListener() {
			public void receiveEvent(Event event) {
				if ("cancel".equals(event.getAction())) {
					System.out.println("canceled");
					System.exit(0);
				}
				else if ("error".equals(event.getAction())) {
					System.out.println("Error: " + String.valueOf(event.getData()));
					System.exit(0);
				}
				else if ("ok".equals(event.getAction())) {
					System.out.println("ok");
					System.exit(0);
				}
			}
		});

		dialog.setVisible(true);
	}
}
