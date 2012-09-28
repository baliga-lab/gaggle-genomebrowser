package org.systemsbiology.genomebrowser.ui;

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

import javax.swing.*;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.Options;
import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.SequenceFetcher;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.swing.SwingGadgets;


public class ImportFastaDialog extends JDialog implements ProgressListener {
	private static final Logger log = Logger.getLogger(ImportFastaDialog.class);
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>Load Sequence from FASTA file</h1>" +
	"<p>Loads sequence for one chromosome or plasmid at a time from a Fasta " +
	"file. Select the sequence you intend to load first, then select the file " +
	"and click OK.</p>" +
	"</body></html>";

	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();
	private JTextField filenameTextField;
	private JComboBox sequenceChooser;
	private Options options;
	private List<Sequence> sequences;
	private SequenceFetcher sequenceFetcher;
	private JProgressBar progressBar;


	public ImportFastaDialog(JFrame parent, Options options, List<Sequence> sequences, SequenceFetcher sequenceFetcher) {
		super(parent, "Load Sequence from FASTA file", true);
		this.options = options;
		this.sequences = sequences;
		this.sequenceFetcher = sequenceFetcher;
		initGui();
		setLocationRelativeTo(parent);
	}

	public ImportFastaDialog(JDialog parent, Options options, List<Sequence> sequences, SequenceFetcher sequenceFetcher) {
		super(parent, "Load Sequence from FASTA file", true);
		this.options = options;
		this.sequences = sequences;
		this.sequenceFetcher = sequenceFetcher;
		initGui();
		setLocationRelativeTo(parent);
	}

	private void initGui() {
		setSize(500, 360);
		setPreferredSize(new Dimension(500, 360));

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

		Sequence[] sequencesArray = new Sequence[sequences.size()];
		sequencesArray = sequences.toArray(sequencesArray);
		sequenceChooser = new JComboBox(sequencesArray);

		filenameTextField = new JTextField();
		filenameTextField.getActionMap().put("enter-key-handler", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				File file = FileUtils.resolveRelativePath(filenameTextField.getText(), options.dataDirectory);
				if (file.isDirectory())
					selectFile();
				else if (file.exists())
					ok();
			}
		});
		im = filenameTextField.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "enter-key-handler");

		addWindowFocusListener(new WindowAdapter() {
		    @Override
			public void windowGainedFocus(WindowEvent e) {
		    	filenameTextField.requestFocusInWindow();
		    }
		});

		JButton browseButton = new JButton("Browse");
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectFile();
			}
		});

		JLabel workingDirLabel = new JLabel(String.format("(data dir: %s)", options.dataDirectory.getAbsolutePath()));
		workingDirLabel.setFont(workingDirLabel.getFont().deriveFont(9.0f));

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
		
		progressBar = new JProgressBar(SwingConstants.HORIZONTAL);

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
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2,12,2,2);
		this.add(sequenceChooser, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.EAST;
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
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,2,2,8);
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
		c.insets = new Insets(8,8,12,8);
		this.add(progressBar, c);

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

	private boolean selectFile() {
		// get filename through file dialog
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select coordinate mapping file");

		File file = FileUtils.resolveRelativePath(filenameTextField.getText(), options.dataDirectory);
		chooser.setCurrentDirectory(file);

		int returnVal = chooser.showOpenDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			file = chooser.getSelectedFile();

			// update working directory
//			options.workingDirectory = file.getParentFile();

			// put filename in text box
			filenameTextField.setText(file.getAbsolutePath());
			return true;
		}
		else {
			file = null;
			return false;
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
		try {
			File file = validateFile();
			
			// report progress
			sequenceFetcher.addProgressListener(this);
			
			// import fasta file in a separate thread
			ImportFastaRunnable runnable = new ImportFastaRunnable();
			runnable.setFile(file);
			runnable.setSequence((Sequence)sequenceChooser.getSelectedItem());
			(new Thread(runnable)).start();

			for (DialogListener listener: listeners) {
				listener.ok("ok", file);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			showErrorMessage(e.getClass().getSimpleName() + e.getMessage());
		}
	}

	private File validateFile() {
		String filename = filenameTextField.getText().trim();
		if ("".equals(filename))
			throw new RuntimeException("Select a FASTA file.");
		File file = new File(filename);
		if (!file.exists())
			throw new RuntimeException("File " + filename + " doesn't exist.");
		if (!file.canRead())
			throw new RuntimeException("Can't read file " + filename);
		if (file.isDirectory())
			throw new RuntimeException(filename + " is a directory.");
		return file;
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
	}

	public void addDialogListener(DialogListener listener) {
		listeners.add(listener);
	}

	public void removeDialogListener(DialogListener listener) {
		listeners.remove(listener);
	}


	// ---- progress listener methods -----------------------------------------

	public void incrementProgress(final int amount) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressBar.setValue(progressBar.getValue() + amount);
			}
		});
	}

	public void setProgress(final int progress) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				log.info("progress = " + progress);
				progressBar.setValue(progress);
			}
		});
	}

	
	public void setExpectedProgress(final int expected) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressBar.setMaximum(expected);
			}
		});
	}

	
	public void setMessage(String message) {
		log.info(message);
	}

	
	public void done() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				close();
			}
		});
	}


	private class ImportFastaRunnable implements Runnable {
		private boolean done = false;
		private File file;
		private Sequence sequence;

		public void setFile(File file) {
			this.file = file;
		}

		public void setSequence(Sequence sequence) {
			this.sequence = sequence;
		}

		public void run() {
			log.info("ImportFastaRunnable: starting to load fasta file \"" + file + "\".");
			try {
				sequenceFetcher.readFastaFile(file, sequence);
				done = true;
				log.info("ImportFastaRunnable: finished loading fasta file.");
			}
			catch (Exception e) {
				final Exception exception = e;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						showErrorMessage(exception.getClass().getSimpleName() + ": " + exception.getMessage());
					}
				});
			}
		}
	}
}
