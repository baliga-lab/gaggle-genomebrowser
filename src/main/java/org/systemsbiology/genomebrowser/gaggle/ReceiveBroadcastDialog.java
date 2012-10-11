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
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.apache.log4j.Logger;
import org.systemsbiology.gaggle.core.datatypes.DataMatrix;
import org.systemsbiology.gaggle.core.datatypes.GaggleData;
import org.systemsbiology.gaggle.core.datatypes.GaggleTuple;
import org.systemsbiology.gaggle.core.datatypes.Namelist;
import org.systemsbiology.genomebrowser.app.ExternalAPI;
import org.systemsbiology.genomebrowser.app.Options;
import org.systemsbiology.genomebrowser.io.CoordinateMapFileIterator;
import org.systemsbiology.genomebrowser.ui.LoadCoordinateMapDialog;
import org.systemsbiology.genomebrowser.util.CoordinateMapSelection;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.swing.SwingGadgets;

public class ReceiveBroadcastDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(ReceiveBroadcastDialog.class);
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>Received Matrix Broadcast</h1>" +
	"<p>In order to transform a gaggle matrix into a track, rows need to be mapped to genome coordinates in " +
	"one of several possible ways.</p>" +
	"<ul><li>&#8226; Row identifiers that match gene identifiers can be mapped to gene coordinates.</li>" +
	"<li>&#8226; Row identifiers can encode position <i>&lt;sequence&gt;[+/-]:&lt;start&gt;-&lt;end&gt;</i>. Strand " +
	"can optionally be indicated by appending a + or - to the sequence name or inferred from" +
	"whether start or end is greater.</li>" +
	"<li>&#8226; An explicit mapping from identifiers to coordinates can be loaded.</li>" +
	"<li>&#8226; An existing mapping can be used.</li></ul>" +
	"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/gaggle#matrix/\">Help</a></p>" +
	"</body></html>";
	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();
	private JTextField nameTextField;
	private JComboBox mappingChooser;
	private JLabel broadcastInfo;
	private Options options;
	private ExternalAPI api;
	private DataMatrix matrix;
	private JRadioButton matrixButton;
	private JRadioButton singleTrackButton;


	public ReceiveBroadcastDialog(ExternalAPI api) {
		this(api.getMainWindow(), api.getOptions());
		this.api = api;
	}

	public ReceiveBroadcastDialog(JFrame owner, Options options) {
		super(owner, "Received Matrix Broadcast", true);
		this.options = options;
		initGui();
	}

	private void initGui() {
//		setSize(500, 440);
//		setPreferredSize(new Dimension(500, 440));

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
		instructions.setPreferredSize(new Dimension(420,240));

		nameTextField = new JTextField();

		addWindowFocusListener(new WindowAdapter() {
		    public void windowGainedFocus(WindowEvent e) {
		    	nameTextField.requestFocusInWindow();
		    }
		});

		broadcastInfo = new JLabel();
		broadcastInfo.setFont(broadcastInfo.getFont().deriveFont(9.0f));
		
		mappingChooser = new JComboBox();
		mappingChooser.addItem(CoordinateMapSelection.NO_MAPPINGS);
		
		JButton loadCoordinateMapButton = new JButton("Load");
		loadCoordinateMapButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadCoordinateMapping();
			}
		});

		matrixButton = new JRadioButton("Import as Matrix");
		singleTrackButton = new JRadioButton("Import as individual track");
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(matrixButton);
		buttonGroup.add(singleTrackButton);

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
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,12,2,2);
		this.add(broadcastInfo, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,24,2,2);
		this.add(matrixButton, c);

		c.gridy++;
		c.insets = new Insets(0,24,2,2);
		this.add(singleTrackButton, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,2,2);
		this.add(new JLabel("Coordinate Mapping:"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(12,2,2,2);
		this.add(mappingChooser, c);

		c.gridx = 2;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,2,2,2);
		this.add(loadCoordinateMapButton, c);

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

	public void setGaggleData(String source, DataMatrix matrix) {
		this.matrix = matrix;
		nameTextField.setText(matrix.getName());
		broadcastInfo.setText(String.format("Matrix from %s (size=%s, species=%s).", source, getSize(matrix), matrix.getSpecies()));
		if (matrix.getColumnCount()==1) {
			singleTrackButton.setSelected(true);
			matrixButton.setEnabled(false);
		}
		else {
			matrixButton.setSelected(true);
		}
		populateMapChooser();
	}

	private String getSize(GaggleData data) {
		if (data instanceof Namelist)
			return String.valueOf( ((Namelist)data).getNames().length );
		else if (data instanceof DataMatrix) {
			DataMatrix m = (DataMatrix)data;
			return m.getRowCount() + "x" + m.getColumnCount();
		}
		else if (data instanceof GaggleTuple) {
			GaggleTuple t = (GaggleTuple)data;
			return String.valueOf(t.getData().getSingleList().size());
		}
		else
			return "";
	}

	public void setCoordinateMaps(List<CoordinateMapSelection> maps) {
		mappingChooser.removeAllItems();
		if (maps==null || maps.size()==0) {
			mappingChooser.addItem(CoordinateMapSelection.NO_MAPPINGS);
		}
		else {
			for (CoordinateMapSelection map : maps)
				mappingChooser.addItem(map);
		}
	}

	public void populateMapChooser() {
		setCoordinateMaps(api.findCoordinateMaps(matrix.getRowTitles()));
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
	}

	@SuppressWarnings("unused")
	private boolean showQuestion(String message) {
		return (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(this, message, "OK?", JOptionPane.YES_NO_OPTION));
	}

	public void addDialogListener(DialogListener listener) {
		listeners.add(listener);
	}

	public void removeDialogListener(DialogListener listener) {
		listeners.remove(listener);
	}

	private void loadCoordinateMapping() {
		LoadCoordinateMapDialog dialog = new LoadCoordinateMapDialog(this, options);
		dialog.addDialogListener(new DialogListener() {

			public void cancel() {}

			public void error(String message, Exception e) {
				showErrorMessage(message);
			}

			public void ok(String action, Object result) {
				log.info(action + " -> " + String.valueOf(result));
				loadMappingFile((File)result);
				populateMapChooser();
			}
		});
		dialog.setVisible(true);
	}

	private void loadMappingFile(File file) {
		// TODO what if dataset is not loaded and there is no dataset UUID?
		CoordinateMapFileIterator cmfi = null;
		try {
			cmfi = new CoordinateMapFileIterator(file);
			api.createCoordinateMapping(api.getDatasetUuid(), FileUtils.stripExtension(file.getName()), cmfi);
		}
		catch (Exception e) {
			showErrorMessage("Error loading file " + file.getName() + ": " + e.getMessage());
		}
		finally {
			if (cmfi!=null) {
				cmfi.cleanup();
			}
		}
	}

	public void close() {
		setVisible(false);
		dispose();
	}

	public void cancel() {
		close();
		for (DialogListener listener: listeners) {
			listener.cancel();
		}
	}

	public void ok() {
		CoordinateMapSelection selectedItem = (CoordinateMapSelection) mappingChooser.getSelectedItem();
		if (selectedItem==null || CoordinateMapSelection.NO_MAPPINGS == selectedItem) {
			showErrorMessage("You need to select a coordinate mapping in order to connect the broadcasted data with locations on the genome");
			return;
		}
		Result result = new Result(nameTextField.getText().trim(), selectedItem.getName(), matrixButton.isSelected());
		close();
		for (DialogListener listener: listeners) {
			listener.ok("ok", result);
		}
	}

	public static class Result {
		public final String coordinateMap;
		public final String name;
		public final boolean isMatrix;

		public Result(String name, String coordinateMap, boolean isMatrix) {
			this.name = name;
			this.coordinateMap = coordinateMap;
			this.isMatrix = isMatrix;
		}
		public String toString() {
			return "(" + name + ", " + coordinateMap + ")";
		}
	}

	public static void main(String[] args) {
		final JFrame frame = new JFrame("test");
		frame.pack();
		frame.setVisible(true);

		Options options = new Options();
		options.workingDirectory = new File(System.getProperty("user.home"));
		options.dataDirectory = new File(System.getProperty("user.home"));
		final ReceiveBroadcastDialog dialog = new ReceiveBroadcastDialog(frame, options);
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
}