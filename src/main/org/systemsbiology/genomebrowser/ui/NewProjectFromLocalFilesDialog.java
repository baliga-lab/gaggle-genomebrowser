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
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.*;

import org.systemsbiology.genomebrowser.model.Topology;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.swing.Separator;
import org.systemsbiology.util.swing.SwingGadgets;


/**
 * Allow the user to create a new dataset using local files as sources for sequence
 * definitions and genome information.
 * 
 * @author cbare
 */
public class NewProjectFromLocalFilesDialog  extends JDialog {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>New project from local files</h1>" +
	"<p>A new genome browser project requires that at least one sequence be defined. It's " +
	"likely that you'll also want to plot the locations of protein coding genes, rna genes, " +
	"and other features along the genome. One way to get started is to load data into the " +
	"genome browser from local tab-delimited files.</p>" +
	"<p>Sequences can be specified by typing or pasting sequences into the sequences dialog " +
	"or read from a file. The genome can be loaded from a file.</p>" +
	"</body></html>";

	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();
	private JTextArea sequencesTextArea;
	private JButton okButton;
	private ButtonGroup topologyButtonGroup;
	private JTextField sequenceFileTextField;
	private JTextField genomeFileTextField;

	private JCheckBox sequencesFromFile;


	public NewProjectFromLocalFilesDialog(JFrame owner) {
		super(owner, "Edit Sequences");
		initGui();
		setLocationRelativeTo(owner);
	}

	public void initGui() {
		setSize(400, 520);
		setPreferredSize(new Dimension(400, 520));

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

		JRadioButton topoLinear = new JRadioButton("linear", true);
		topoLinear.setActionCommand("linear");
		JRadioButton topoCircular = new JRadioButton("circular");
		topoCircular.setActionCommand("circular");
		JRadioButton topoUnknown = new JRadioButton("unknown");
		topoUnknown.setActionCommand("unknown");

		topologyButtonGroup = new ButtonGroup();
		topologyButtonGroup.add(topoLinear);
		topologyButtonGroup.add(topoCircular);
		topologyButtonGroup.add(topoUnknown);

		Box topologyBox = Box.createVerticalBox();
		topologyBox.add(topoLinear);
		topologyBox.add(topoCircular);
		topologyBox.add(topoUnknown);
		
		sequencesFromFile = new JCheckBox("Load sequences from file");
		Box seqBox = Box.createHorizontalBox();
		seqBox.add(new JLabel("Sequences:"));
		seqBox.add(Box.createHorizontalStrut(40));
		seqBox.add(sequencesFromFile);

		sequencesTextArea = new JTextArea();

		sequenceFileTextField = new JTextField();
		JButton sequenceBrowseButton = new JButton("Browse");

		genomeFileTextField = new JTextField();
		JButton genomeBrowseButton = new JButton("Browse");

		okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});
		okButton.getActionMap().put("press-OK", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});
		im = okButton.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "press-OK");

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));
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
		c.gridwidth = 3;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,12,2,2);
		this.add(seqBox, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(new JLabel("Topology:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(topologyBox, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(4,12,4,12);
		this.add(new JScrollPane(sequencesTextArea), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(4,12,4,12);
		this.add(new JLabel("Filename"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,12,2,2);
		this.add(sequenceFileTextField, c);

		c.gridx = 2;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(sequenceBrowseButton, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,12,2,2);
		this.add(new Separator(), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(4,12,4,12);
		this.add(new JLabel("Genome:"), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(4,12,4,12);
		this.add(new JLabel("Filename"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,12,2,2);
		this.add(genomeFileTextField, c);

		c.gridx = 2;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,12,2,2);
		this.add(genomeBrowseButton, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,24,2);
		this.add(buttonPanel, c);
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
	}

	public List<SequenceDescription> parseSequences() {
		String text = sequencesTextArea.getText();
		List<SequenceDescription> results = new ArrayList<SequenceDescription>();
		Topology topology = Topology.fromString(topologyButtonGroup.getSelection().getActionCommand());
		for (String line : text.split("\n")) {
			line = line.trim();
			if (line.length()>0) {
				String[] fields = line.split("\\s*[\\t,;:]\\s*");
				if (fields.length==2) {
					int len = Integer.parseInt(fields[1]);
					results.add(new SequenceDescription(fields[0], len, topology));
				}
				else {
					throw new RuntimeException("Can't parse: \"" + line + "\".");
				}
			}
		}
		if (results.size()==0) {
			throw new RuntimeException("Enter at least one sequence.");
		}
		return results;
	}

	public void addDialogListener(DialogListener listener) {
		listeners.add(listener);
	}

	public void removeDialogListener(DialogListener listener) {
		listeners.remove(listener);
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
		try {
			Result results = new Result(parseSequences(), sequenceFileTextField.getText().trim(), genomeFileTextField.getText().trim(), sequencesFromFile.isSelected());
			close();
			for (DialogListener listener: listeners) {
				listener.ok("ok", results);
			}
		}
		catch (Exception e) {
			showErrorMessage(e.getMessage());
		}
	}

	public static void main(String[] args) {
		final JFrame frame = new JFrame("test");
		frame.pack();
		frame.setVisible(true);
		
		NewProjectFromLocalFilesDialog dialog = new NewProjectFromLocalFilesDialog(frame);
		dialog.addDialogListener(new DialogListener() {
			public void ok(String action, Object result) {
				System.out.println("action=" + action);
				System.out.println("results=" + result);
			}
			public void cancel() {
				System.out.println("cancel");
			}
			public void error(String message, Exception e) {
				System.out.println("error: " + message);
				e.printStackTrace();
			}
		});
		dialog.setVisible(true);
	}


	public static class SequenceDescription {
		public final String name;
		public final int length;
		public final Topology topology;

		public SequenceDescription(String name, int length, Topology topology) {
			this.name = name;
			this.length = length;
			this.topology = topology;
		}

		@Override
		public String toString() {
			return "(" + name + ", " + length + ", " + topology + ")";
		}
	}

	public static class Result {
		public final List<SequenceDescription> sequences;
		public final String sequenceFilename;
		public final String genomeFilename;
		public final boolean sequencesFromFile;

		public Result(List<SequenceDescription> sequences, String sequenceFilename, String genomeFilename, boolean sequencesFromFile) {
			this.sequences = sequences;
			this.sequenceFilename = sequenceFilename;
			this.genomeFilename = genomeFilename;
			this.sequencesFromFile = sequencesFromFile;
		}
	}
}
