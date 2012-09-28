package org.systemsbiology.genomebrowser.ui.importtrackwizard;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.*;

import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.util.swing.SwingGadgets;


public class LoadFeaturesProgressPanel extends JPanel implements WizardPanel, ProgressListener {
	private WizardMainWindow parent;
	private ImportTrackWizard wiz;
	private JProgressBar progressBar;
	private JTextArea status;
	private StringBuilder statusText = new StringBuilder();
	private static final String INSTRUCTIONS_HTML = "<html><body>" +
			"<h1>Importing Features</h1>" +
			"<p>The file is now being read into the dataset. This can take some " +
			"time, especially for large files.</p></body></html>";


	public LoadFeaturesProgressPanel(WizardMainWindow parent, ImportTrackWizard wiz) {
		this.parent = parent;
		this.wiz = wiz;
		initGui();
	}


	private void initGui() {
		setOpaque(false);

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);

		progressBar = new JProgressBar(JProgressBar.HORIZONTAL);

		status = new JTextArea(4,30);
		status.setEditable(false);
		status.setFont(status.getFont().deriveFont(10.0f));
		status.setLineWrap(true);

		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(8,8,12,8);
		this.add(instructions, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(12,48,12,48);
		this.add(progressBar, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,24,2,12);
		this.add(new JLabel("Status:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(4,24,48,48);
		this.add(new JScrollPane(status), c);
	}

	public boolean getEnableDone() {
		return wiz.isLoaded();
	}

	public boolean getEnableNext() {
		return false;
	}

	public boolean getEnableBack() {
		return !wiz.isLoadedOrLoading();
	}

	public void onLoad() {
		if (!wiz.isLoadedOrLoading()) {
			statusText.setLength(0);
			wiz.importTrackIfReady(this);
			parent.updateStatus();
		}
	}

	public void onUnload() {
	}

	public void windowGainedFocus() {
	}


	// progress listener methods
	// these methods call invokeLater so may be called from off of the swing event thread

	public void done() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressBar.setValue(progressBar.getMaximum());
				parent.updateStatus();
			}
		});
	}

	public void incrementProgress(final int amount) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressBar.setValue(progressBar.getValue() + amount);
				statusText.append(".");
				status.setText(statusText.toString());
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

	public void setMessage(final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (statusText.length() > 0)
					statusText.append("\n");
				statusText.append(message);
				status.setText(statusText.toString());
			}
		});
	}

	public void setProgress(final int progress) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progressBar.setValue(progress);
				statusText.append(".");
				status.setText(statusText.toString());
			}
		});
	}
}
