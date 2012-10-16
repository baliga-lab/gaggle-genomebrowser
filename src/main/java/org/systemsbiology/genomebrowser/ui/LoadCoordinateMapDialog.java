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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.*;

import org.systemsbiology.genomebrowser.Options;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.swing.SwingGadgets;


public class LoadCoordinateMapDialog extends JDialog {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>Load Coordinate Mapping</h1>" +
	"<p>A <b>coordinate mapping</b> is a mapping from a set of names to coordinates on the genome. Genome coordinates are " +
	"a tuple consisting of (sequence, strand, start, and end) for example, the <i>yeast</i> " +
	"gene <i>YGL026C</i> has coordinates <b>(chr7, -, 446416, 448540)</b>. " +
	"The program accepts mappings as tab-delimited text files where " +
	"each line holds a name, sequence, strand, start, and end.</p>" +
	"<p>The program can make some reasonable guesses, but it's best if feature names and " +
	"names of chromosomes or other sequences <b>match exactly</b>.</p>" +
	"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/coordinatemap/\">Help</a> on loading coordinate maps.</p>" +
	"</body></html>";

	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();
	private JTextField filenameTextField;
	private Options options;


	public LoadCoordinateMapDialog(JFrame parent, Options options) {
		super(parent, "Load Coordinate Mapping", true);
		this.options = options;
		initGui();
		setLocationRelativeTo(parent);
	}

	public LoadCoordinateMapDialog(JDialog parent, Options options) {
		super(parent, "Load Coordinate Mapping", true);
		this.options = options;
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
			close();
			for (DialogListener listener: listeners) {
				listener.ok("ok", file);
			}
		}
		catch (Exception e) {
			showErrorMessage(e.getMessage());
		}
	}

	private File validateFile() {
		String filename = filenameTextField.getText().trim();
		if ("".equals(filename))
			throw new RuntimeException("Select a file.");
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


	public static void main(String[] args) {
		final JFrame frame = new JFrame("test");
		frame.pack();
		frame.setVisible(true);

		Options options = new Options();
		options.workingDirectory = new File(System.getProperty("user.home"));
		options.dataDirectory = new File(System.getProperty("user.home"));

		final LoadCoordinateMapDialog dialog = new LoadCoordinateMapDialog(frame, options);
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
