package org.systemsbiology.genomebrowser.gaggle;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.*;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.event.Event;
import org.systemsbiology.genomebrowser.event.EventListener;
import org.systemsbiology.genomebrowser.app.ExternalAPI;
import org.systemsbiology.genomebrowser.Options;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.io.track.TrackBuilder;
import org.systemsbiology.genomebrowser.model.CoordinateMap;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.model.TrackImporter;
import org.systemsbiology.genomebrowser.model.Feature.NamedFeature;
import org.systemsbiology.genomebrowser.util.CoordinateMapSelection;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.swing.SwingGadgets;


public class ReceiveTrackDialog extends JDialog {
	private static final Logger log = Logger.getLogger(ReceiveTrackDialog.class);
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>Received Track</h1>" +
	"<p>Receive a track... instructions...</p>" +
	"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/gaggle#track/\">Help</a></p>" +
	"</body></html>";
	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();
	private JTextField nameTextField;
	private ExternalAPI api;
	private JComboBox trackTypeChooser;
//	private JTextArea messageBox;


	public ReceiveTrackDialog(ExternalAPI api) {
		super(api.getMainWindow(), "Received Track", true);
		this.api = api;
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

		nameTextField = new JTextField();

		addWindowFocusListener(new WindowAdapter() {
		    public void windowGainedFocus(WindowEvent e) {
		    	nameTextField.requestFocusInWindow();
		    }
		});

		trackTypeChooser = new JComboBox();
		for (String type : api.getTrackTypes()) {
			trackTypeChooser.addItem(type);
		}
		trackTypeChooser.setSelectedItem("quantitative.segment");
		trackTypeChooser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				log.info("selected track type: " + trackTypeChooser.getSelectedItem());
			}
		});

//		messageBox = new JTextArea();

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
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(new JLabel("Track Name:"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		this.add(nameTextField, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(new JLabel("Track Type:"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		this.add(trackTypeChooser, c);

//		c.gridx = 0;
//		c.gridy++;
//		c.gridwidth = 3;
//		c.gridheight = 2;
//		c.weightx = 0.0;
//		c.weighty = 0.0;
//		c.anchor = GridBagConstraints.NORTHEAST;
//		c.fill = GridBagConstraints.BOTH;
//		c.insets = new Insets(2,12,2,2);
//		this.add(messageBox, c);

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

	public void setTrackName(String name) {
		nameTextField.setText(name);
	}

	public void setTrackType(String type) {
		trackTypeChooser.setSelectedItem(type);
	}

//	@SuppressWarnings("unused")
//	private void showErrorMessage(String message) {
//		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
//	}

	public void close() {
		setVisible(false);
		dispose();
	}

	public void cancel() {
		for (DialogListener listener: listeners) {
			listener.cancel();
		}
	}

	public void ok() {
		Result result = new Result(
				nameTextField.getText().trim(),
				(String)trackTypeChooser.getSelectedItem());
		for (DialogListener listener: listeners) {
			listener.ok("ok", result);
		}
	}

	public void addDialogListener(DialogListener listener) {
		listeners.add(listener);
	}

	public void removeDialogListener(DialogListener listener) {
		listeners.remove(listener);
	}

	
	public static void main(String[] args) {
		ExternalAPI api = new TestExternalAPI();
		ReceiveTrackDialog dialog = new ReceiveTrackDialog(api);
		dialog.setTrackName("Fred");
		dialog.setTrackType("quantitative.segment");
		dialog.addDialogListener(new DialogListener() {

			public void cancel() {
				System.out.println("canceled");
				System.exit(0);
			}

			public void error(String message, Exception e) {
				System.out.println("Error: " + message);
				e.printStackTrace();
				System.exit(0);
			}

			public void ok(String action, Object result) {
				System.out.println(action + " -> " + String.valueOf(result));
				System.exit(0);
			}
		});
		dialog.setVisible(true);
	}


	public static class Result {
		public final String name;
		public final String type;

		public Result(String name, String type) {
			this.name = name;
			this.type = type;
		}
	}

	private static class TestExternalAPI implements ExternalAPI {

		public void addEventListener(EventListener listener) {
		}

		public void addMenu(String title, Action[] actions) {
		}

		public void addToolbar(String title, JToolBar toolbar, Action action) {
		}

		public void addTrack(Track<? extends Feature> track) {
		}

		public void bringToFront() {
		}

		public void createCoordinateMapping(UUID datasetUuid, String name, Iterable<NamedFeature> mappings) {
		}

		public List<CoordinateMapSelection> findCoordinateMaps(String[] rowTitles) {
			return null;
		}

		public Dataset getDataset() {
			return null;
		}

		public UUID getDatasetUuid() {
			return null;
		}

		public JFrame getMainWindow() {
			final JFrame frame = new JFrame("test");
			frame.pack();
			frame.setVisible(true);
			return frame;
		}

		public Options getOptions() {
			return null;
		}

		public Collection<Feature> getSelectedFeatures() {
			return null;
		}

		public Segment getSelectedSegment() {
			return null;
		}

		public List<Segment> getSelectedSegments() {
			return null;
		}

		public String getSpecies() {
			return null;
		}

		public TrackImporter getTrackImporter() {
			return null;
		}

		public Segment getVisibleSegment() {
			return null;
		}

		public CoordinateMap loadCoordinateMap(String table) {
			return null;
		}

		public void minimize() {
		}

		public void publishEvent(Event event) {
		}

		public void refresh() {
		}

		public void showErrorMessage(String message, Exception e) {
		}

		public void requestShutdown() {
		}

		public void selectFeaturesByName(List<String> names) {
		}

		public void setVisibleToolbar(String title, boolean visible) {
		}

		public void showMessage(String message) {
			log.warn(message);
		}

		public List<String> getTrackTypes() {
			List<String> result = new ArrayList<String>();
			result.add("quantitative.segment");
			result.add("quantitative.positional");
			result.add("genes");
			return result;
		}

		public TrackBuilder getTrackBuilder(String type) {
			// TODO Auto-generated method stub
			return null;
		}

		public Strand getSelectionStrandHint() {
			return Strand.none;
		}

		public BookmarkDataSource getOrCreateBookmarkDataSource(String string) {
			// TODO Auto-generated method stub
			return null;
		}

		public BookmarkDataSource getSelectedBookmarkDataSource() {
			// TODO Auto-generated method stub
			return null;
		}

		public List<GeneFeature> getGenesIn(Sequence sequence, Strand strand, int start, int end) {
			// TODO Auto-generated method stub
			return null;
		}

		public void updateTrack(Track<Feature> track) {
			// TODO Auto-generated method stub
			
		}
	}
}
