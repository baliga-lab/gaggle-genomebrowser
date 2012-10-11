package org.systemsbiology.genomebrowser.ui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.*;

import org.systemsbiology.genomebrowser.io.SequenceInfoReader;
import org.systemsbiology.genomebrowser.io.SequenceInfoReader.SequenceInfo;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.DatasetBuilder;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Topology;
import org.systemsbiology.genomebrowser.model.FeatureSource;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.StringUtils;
import org.systemsbiology.util.swing.SwingGadgets;


public class ImportFileGenome extends JDialog {
	private static final String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>Create a new dataset</h1>" +
	"<p>Specify the sequences using the text-box below or a tab-delimited file. In either<br>" +
	"case, the expected format is one sequence, chromosome or replicon per line. Each line<br>" +
	"should consist of a name and length separated by a tab.</p>" +
	"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/import/\">Help</a></p>" +
	"</body></html>";

	private File workingDirectory;
	private JTextField filenameTextField;
	private JTextField nameTextField;
	private JTextField speciesTextField;
	private JComboBox topologyChooser;
	private DatasetBuilder datasetBuilder;


	public ImportFileGenome(JFrame parent, File workingDirectory) {
		super(parent, "Create new Genome");
		this.workingDirectory = workingDirectory==null ? new File(System.getProperty("user.home")) : workingDirectory;
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

		Box vbox = Box.createVerticalBox();
		vbox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		vbox.setOpaque(false);
		add(vbox);

		// instructions
		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);
		vbox.add(instructions);
		vbox.add(Box.createVerticalStrut(24));

		nameTextField = new JTextField();

		Box hbox = Box.createHorizontalBox();
		hbox.add(new JLabel("Dataset name:"));
		hbox.add(nameTextField);
		hbox.add(Box.createHorizontalStrut(120));
		vbox.add(hbox);
		vbox.add(Box.createVerticalStrut(16));

		speciesTextField = new JTextField();

		hbox = Box.createHorizontalBox();
		hbox.add(new JLabel("Species:"));
		hbox.add(speciesTextField);
		hbox.add(Box.createHorizontalStrut(120));
		vbox.add(hbox);
		vbox.add(Box.createVerticalStrut(16));
		
		topologyChooser = new JComboBox();
		topologyChooser.addItem("Circular");
		topologyChooser.addItem("Linear");
		topologyChooser.addItem("Mixed");

		hbox = Box.createHorizontalBox();
		hbox.add(new JLabel("Topology:"));
		hbox.add(topologyChooser);
		hbox.add(Box.createHorizontalStrut(120));
		vbox.add(hbox);
		vbox.add(Box.createVerticalStrut(16));

		filenameTextField = new JTextField();
		JButton browseButton = new JButton("Browse");
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browse();
			}
		});
		filenameTextField.getActionMap().put("enter-key-handler", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				File file = new File(filenameTextField.getText());
				if (!file.isAbsolute()) {
					if (workingDirectory == null)
						workingDirectory = new File(System.getProperty("user.home"));
					file = new File(workingDirectory, filenameTextField.getText());
				}
				if (file.isDirectory())
					browse();
			}
		});
		im = filenameTextField.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "enter-key-handler");

		JLabel workingDirLabel = new JLabel(String.format("(working dir: %s)", workingDirectory.getAbsolutePath()));
		workingDirLabel.setFont(workingDirLabel.getFont().deriveFont(9.0f));

		hbox = Box.createHorizontalBox();
		hbox.add(new JLabel("Sequence info file:"));
		hbox.add(filenameTextField);
		hbox.add(browseButton);
		vbox.add(hbox);
		vbox.add(Box.createVerticalStrut(16));

		sequenceTextArea = new JTextArea(10,40);
		sequenceTextArea.setTabSize(32);

		JPanel alignLeft = new JPanel();
		alignLeft.setLayout(new BorderLayout());
		alignLeft.add(new JLabel("Paste sequence information:"), BorderLayout.WEST);
		vbox.add(alignLeft);
		vbox.add(new JScrollPane(sequenceTextArea));
		vbox.add(Box.createVerticalGlue());

		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				done();
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
		vbox.add(Box.createVerticalStrut(16));
		vbox.add(buttonPanel);
		
		this.pack();
	}

	private void browse() {
		String path = filenameTextField.getText();

		if (workingDirectory == null)
			workingDirectory = new File(System.getProperty("user.home"));

		JFileChooser chooser = DatasetFileChooser.getNewDatasetFileChooser();
		chooser.setDialogTitle("Sequence information file");
		if (StringUtils.isNullOrEmpty(path)) {
			chooser.setSelectedFile(workingDirectory);
		}
		else {
			File file = new File(path);
			if (!file.isAbsolute()) {
				file = new File(workingDirectory, path);
			}

			if (file.isDirectory()) {
				// inset a generic placeholder filename if none given
				file = new File(file, "dataset.hbgb");
			}
			else {
				// add hbgb extension if no recognized extension is provided
				// is this more annoying than useful?
				String name = file.getName();
				if (name.endsWith(".hbgb") || name.endsWith(".dataset")) {
					name += ".hbgb";
					file = new File(file.getParentFile(), name);
				}
			}

			chooser.setSelectedFile(file);
		}

		int returnVal = chooser.showSaveDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			filenameTextField.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
	}

	public void close() {
		setVisible(false);
		dispose();
	}

	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();

	private JTextArea sequenceTextArea;

	public void addDialogListener(DialogListener listener) {
		listeners.add(listener);
	}

	public void removeDialogListener(DialogListener listener) {
		listeners.remove(listener);
	}

	public void cancel() {
		close();
		for (DialogListener listener: listeners) {
			listener.cancel();
		}
	}

	public void done() {
		if (sequenceTextArea.getText().trim().length()==0 && filenameTextField.getText().trim().length()==0) {
			showErrorMessage("The program needs sequence information, either from a file or pasted into the text box.");
			return;
		}

		List<SequenceInfo> sequences;
		try {
			SequenceInfoReader sir = new SequenceInfoReader();
			if (sequenceTextArea.getText().trim().length() > 0) {
				sequences = sir.read(new StringReader(sequenceTextArea.getText().trim()));
			}
			else {
				sequences = sir.read(new File(filenameTextField.getText().trim()));
			}
			String t = (String)topologyChooser.getSelectedItem();
			Topology topology = ("mixed".equals(t)) ? null : Topology.valueOf(t.toLowerCase());
			Dataset dataset = buildDataset(nameTextField.getText(), speciesTextField.getText(), topology, sequences);

			close();
			for (DialogListener listener: listeners) {
				listener.ok("done", dataset);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			showErrorMessage(e.getClass().getName() + ": " + e.getMessage());
		}
	}

	private Dataset buildDataset(String name, String species, Topology topology, List<SequenceInfo> sequences) {
		UUID dsuuid = datasetBuilder.beginNewDataset(name);
		datasetBuilder.setAttribute(dsuuid, "species", species);

		for (SequenceInfo info: sequences) {
			// default to circular 'cause this program is mostly for microbes
			Topology t = info.topology!=null ? info.topology :
				        	topology!=null ? topology :
				        		Topology.circular;
			datasetBuilder.addSequence(info.name, info.length, t);
		}
		return datasetBuilder.getDataset();
	}

	// dependency
	public void setDatasetBuilder(DatasetBuilder datasetBuilder) {
		this.datasetBuilder = datasetBuilder;
	}


	public static void main(String[] args) {
		ImportFileGenome dialog = new ImportFileGenome(new JFrame(), null);
		dialog.setDatasetBuilder(new DatasetBuilder() {

			public UUID addSequence(String seqId, int length, Topology topology) {
				return null;
			}

			public void addSequences(List<Sequence> sequences) {
			}

			public UUID addTrack(String trackType, String name, FeatureSource featureSource) {
				return null;
			}

			public UUID beginNewDataset(String name) {
				return null;
			}

			public Dataset getDataset() {
				return null;
			}

			public void setAttribute(UUID uuid, String key, Object value) {
			}
			
			public String toString() {
				return "Fake Dataset";
			}
		});
		dialog.addDialogListener(new DialogListener() {
			public void cancel() {
				System.out.println("canceled");
				System.exit(0);
			}
			public void error(String message, Exception e) {
				System.out.println(message);
				e.printStackTrace();
				System.exit(0);
			}
			public void ok(String action, Object result) {
				System.out.println(action + " -> " + result);
				System.exit(0);
			}
		});
		dialog.setVisible(true);
	}
}
