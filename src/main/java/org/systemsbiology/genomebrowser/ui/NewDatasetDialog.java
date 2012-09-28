package org.systemsbiology.genomebrowser.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.*;

import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.StringUtils;
import org.systemsbiology.util.swing.SwingGadgets;


public class NewDatasetDialog extends JDialog {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>New Dataset</h1>" +
	"<p>To create a new dataset, first specify a <b>filename</b> where the program can store your data. " +
	"Next you'll need to <b>define the sequences</b> (chromosomes,	plasmids, etc.) with which you'll be working. " +
	"This can be accomplished by automatically importing data from " +
	"<a href=\"http://www.ncbi.nlm.nih.gov/\">NCBI</a>, " +
	"<a href=\"http://genome.ucsc.edu/\">UCSC genome browser</a> or " +
	"the <a href=\"http://microbes.ucsc.edu/\">UCSC archaeal genome browser</a>. Alternatiely, " +
	"chromosome and track information can be imported from local files.</p>" +
	"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/new/\">Help</a></p>" +
	"</body></html>";

	private File workingDirectory;
	private JTextField filenameTextField;
	private JRadioButton fileButton;
	private JRadioButton ncbiButton;
	private JRadioButton ucscButton;


	public NewDatasetDialog(JFrame owner, File workingDirectory) {
		super(owner, "New Dataset");
		this.workingDirectory = workingDirectory==null ? new File(System.getProperty("user.home")) : workingDirectory;
		initGui();
	}

	public void initGui() {
		setSize(500, 380);
		setPreferredSize(new Dimension(500, 350));

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

		fileButton = new JRadioButton("Local file(s)", true);
		fileButton.setToolTipText("Import genome data from local files.");
		ncbiButton = new JRadioButton("NCBI", false);
		ncbiButton.setToolTipText("Import genome data from NCBI. Works well for microbes.");
		ucscButton = new JRadioButton("UCSC genome browser", false);
		ucscButton.setToolTipText("Import genome data from UCSC. Works well for model organisms");
		ButtonGroup bg = new ButtonGroup();
		bg.add(fileButton);
		bg.add(ncbiButton);
		bg.add(ucscButton);

		JPanel dataSourcePanel = new JPanel();
		dataSourcePanel.add(fileButton);
		dataSourcePanel.add(ncbiButton);
		dataSourcePanel.add(ucscButton);

		JButton genomeButton = new JButton("Import Genome");
		genomeButton.setToolTipText("Import genome information from the selected source");
		genomeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				importGenome();
			}
		});

		JButton doneButton = new JButton("Done");
		doneButton.setToolTipText("Creates an empty dataset; experts only");
		doneButton.addActionListener(new ActionListener() {
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
		buttonPanel.add(genomeButton);
		buttonPanel.add(doneButton);
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
		this.add(new JLabel("Filename:"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		this.add(filenameTextField, c);

		c.gridx = 2;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,2,2,2);
		this.add(browseButton, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(4,12,2,8);
		this.add(workingDirLabel, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(12,12,2,8);
		this.add(new JLabel("Select data source:"), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0,12,2,8);
		this.add(dataSourcePanel, c);

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

	private void browse() {
		String path = filenameTextField.getText();

		JFileChooser chooser = DatasetFileChooser.getNewDatasetFileChooser();
		chooser.setDialogTitle("New Dataset file");
		File file = null;
		if (StringUtils.isNullOrEmpty(path)) {
			file = new File(System.getProperty("user.home"));
		}
		else {
			file = new File(path);
			if (!file.isAbsolute()) {
				if (workingDirectory == null)
					workingDirectory = new File(System.getProperty("user.home"));
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
				if (!name.endsWith(".hbgb") && !name.endsWith(".dataset")) {
					name = name + ".hbgb";
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

	/**
	 * check that the entered filename is OK
	 */
	public boolean validateFile() {
		String path = filenameTextField.getText().trim();
		if (path.length()==0) {
			showErrorMessage("Filename must be filled in");
			return false;
		}

		File file = new File(path);
		if (!file.isAbsolute()) {
			if (workingDirectory == null)
				workingDirectory = new File(System.getProperty("user.home"));
			file = new File(workingDirectory, path);
		}

		if (file.isDirectory()) {
			showErrorMessage("You need to select a file, not just a directory.");
			return false;
		}

		if (file.exists()) {
			if (!file.canWrite()) {
				showErrorMessage("Selected file is not writable.");
				return false;
			}

			if (!showQuestion("Selected file already exists. OK to add new dataset to the existing file?"))
				return false;
		}

		return true;
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
	}

	private boolean showQuestion(String message) {
		return (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(this, message, "OK?", JOptionPane.YES_NO_OPTION));
	}

	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();

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

	public void done() {
		if (!validateFile()) return;
		close();
		for (DialogListener listener: listeners) {
			listener.ok("done", filenameTextField.getText());
		}
	}

	public void importGenome() {
		if (!validateFile()) return;
		close();
		for (DialogListener listener: listeners) {
			String action = null;
			if (fileButton.isSelected())
				action = "import.file";
			else if (ncbiButton.isSelected())
				action = "import.ncbi";
			else if (ucscButton.isSelected())
				action = "import.ucsc";
			listener.ok(action, filenameTextField.getText());
		}
	}


	public static void main(String[] args) {
		final JFrame frame = new JFrame("test");
		frame.pack();
		frame.setVisible(true);

		final NewDatasetDialog dialog = new NewDatasetDialog(frame, null);
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
				String filename = (String)result;
				System.out.println(action + " -> " + filename);
				System.exit(0);
			}
		});
		dialog.setVisible(true);
	}
}
