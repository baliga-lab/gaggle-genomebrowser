package org.systemsbiology.genomebrowser.ui.importtrackwizard;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.*;

import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.swing.SwingGadgets;


public class SelectFilePanel extends JPanel implements WizardPanel {
	private JTextField filenameTextField;
	private WizardMainWindow parent;
	private ImportTrackWizard wiz;

	private static String INSTRUCTIONS_HTML = "<html><body>" +
		"<h1>Select File</h1>" +
		"<p>This wizard will guide you through the process of importing " +
		"track data into the genome browser. To start with, select a file you " +
		"would like to read. The import wizard accepts GFF files.</p>" +
		"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/import/\">Help</a></p>" +
		"</body></html>";


	public SelectFilePanel(WizardMainWindow parent, ImportTrackWizard wiz) {
		this.parent = parent;
		this.wiz = wiz;
		initGui();
	}

	// TODO display working directory

	private void initGui() {
		setOpaque(false);

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);

		filenameTextField = new JTextField();
		filenameTextField.getActionMap().put("enter-key-handler", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				File file = FileUtils.resolveRelativePath(filenameTextField.getText(), wiz.getOptions().workingDirectory);
				if (file.isDirectory())
					selectFile();
				else if (file.exists())
					parent.next();
			}
		});
		InputMap im = filenameTextField.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "enter-key-handler");

		JButton browseButton = new JButton("Browse");
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectFile();
			}
		});

		JLabel workingDirLabel = new JLabel(String.format("(working dir: %s)", wiz.getOptions().workingDirectory.getAbsolutePath()));
		workingDirLabel.setFont(workingDirLabel.getFont().deriveFont(9.0f));

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
	}

	public boolean selectFile() {
		// get filename through file dialog
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select track data file");

		File file = FileUtils.resolveRelativePath(filenameTextField.getText(), wiz.getOptions().workingDirectory);
		chooser.setCurrentDirectory(file);

		int returnVal = chooser.showOpenDialog(parent);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			file = chooser.getSelectedFile();

			// update working directory
			wiz.getOptions().workingDirectory = file.getParentFile();
			
			// put filename in text box
			filenameTextField.setText(file.getPath());
			return true;
		}
		else {
			file = null;
			return false;
		}
	}

	public void onLoad() {
	}

	public void onUnload() {
		wiz.setFilename(filenameTextField.getText().trim());
		wiz.setScanned(false);
		wiz.setLoaded(false);
	}

	public boolean getEnableDone() {
		return false;
	}

	public boolean getEnableNext() {
		return true;
	}

	public boolean getEnableBack() {
		return true;
	}

	public void windowGainedFocus() {
		filenameTextField.requestFocusInWindow();
	}
}
