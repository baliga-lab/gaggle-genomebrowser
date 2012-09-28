package org.systemsbiology.genomebrowser.ui.importtrackwizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.util.ActionListenerSupport;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.swing.GradientPanel;


/**
 * A JFrame subclass which represents the main window of the track import
 * wizard. Each step in the wizard is a JPanel contained in this frame.
 * @author cbare
 */
public class WizardMainWindow extends JFrame implements ExceptionReporter {
	private static final Logger log = Logger.getLogger(WizardMainWindow.class);
	private ActionListenerSupport actionListeners = new ActionListenerSupport();

	private ImportTrackWizard wiz;

	private int panelIndex;
	private List<JPanel> panels = new ArrayList<JPanel>();

	private JButton doneButton;
	private JButton nextButton;
	private JButton backButton;


	public WizardMainWindow(JFrame parent, ImportTrackWizard wiz) {
		super("Import Track(s)");
		this.wiz = wiz;
		initGui(parent);
	}

	public void initGui(JFrame parent) {
		setContentPane(new GradientPanel());

		JPanel iconPanel = new JPanel();
		iconPanel.setLayout(new BorderLayout());
		iconPanel.setOpaque(false);
		iconPanel.setBorder(BorderFactory.createEmptyBorder(2, 12, 0, 12));
		try {
			iconPanel.add(new JLabel(FileUtils.getIcon("import_icon.png")), BorderLayout.EAST);
		}
		catch (IOException e) {
		}
		JLabel title = new JLabel("Import Track Wizard");
		Font font = title.getFont().deriveFont(Font.BOLD, 18.0f);
		title.setFont(font);
		title.setForeground(new Color(0xCC336699, true));
		iconPanel.add(title, BorderLayout.WEST);

		createWizardPanels();

		JPanel buttonPanel = createButtonPanel();

		setSize(new Dimension(500,600));
		setLayout(new BorderLayout());
		
		add(iconPanel, BorderLayout.NORTH);

		if (panels.size() > 0)
			add(panels.get(0), BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
		update();

		// Notify currently active wizard panel whenever frame is activated.
		addWindowFocusListener(new WindowAdapter() {
		    public void windowGainedFocus(WindowEvent e) {
		        WizardPanel panel = (WizardPanel)panels.get(panelIndex);
		        if (panel != null)
		        	panel.windowGainedFocus();
		    }
		});

		// map escape and command-w to cancel the dialog
		this.getRootPane().getActionMap().put("close-window-on-escape", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		// map enter to select done if enables
		this.getRootPane().getActionMap().put("enter-to-done", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (doneButton.isEnabled())
					done();
			}
		});
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "enter-to-done");

		setLocationRelativeTo(parent);
		setVisible(true);
		load();
	}

	private void createWizardPanels() {
		panels.add(new SelectFilePanel(this, wiz));
		panels.add(new PreviewPanel(this, wiz));
		panels.add(new TrackAttributesPanel(this, wiz));
		panels.add(new LoadFeaturesProgressPanel(this, wiz));
	}

	private JPanel createButtonPanel() {

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});

		nextButton = new JButton("Next\u2192");
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				next();
			}
		});

		backButton = new JButton("\u2190Back");
		backButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				back();
			}
		});

		doneButton = new JButton("Done");
		doneButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				done();
			}
		});
		doneButton.setEnabled(false);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
		buttonPanel.add(backButton);
		buttonPanel.add(nextButton);
		buttonPanel.add(doneButton);
		buttonPanel.add(cancelButton);
		
		buttonPanel.setOpaque(false);

		return buttonPanel;
	}


	/**
	 * @return return the newly loaded track(s).
	 */
	public List<Track<Feature>> getImportedTracks() {
		return null;
	}


	public void updateStatus() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				update();
			}
		});
	}

	private void update() {
		backButton.setEnabled(panelIndex>0 && currentPanel().getEnableBack());
		nextButton.setEnabled(panelIndex<(panels.size()-1) && currentPanel().getEnableNext());
		doneButton.setEnabled(currentPanel().getEnableDone());
	}

	public WizardPanel currentPanel() {
		return (WizardPanel)panels.get(panelIndex);
	}

	// ------------------------------------------------------------------------

	public void next() {
		if (panelIndex < panels.size()-1) {
			JPanel oldPanel = panels.get(panelIndex);
			unload();
			remove(oldPanel);
			panelIndex++;
			JPanel newPanel = panels.get(panelIndex);
			add(newPanel, BorderLayout.CENTER);
			update();
			validate();
			repaint();
			load();
		}
	}

	public void back() {
		if (panelIndex > 0) {
			JPanel oldPanel = panels.get(panelIndex);
			unload();
			remove(oldPanel);
			panelIndex--;
			JPanel newPanel = panels.get(panelIndex);
			add(newPanel, BorderLayout.CENTER);
			update();
			validate();
			repaint();
			load();
		}
	}

	private void load() {
		try {
			currentPanel().onLoad();
		}
		catch (Exception e) {
			showErrorMessage("Import Error", e);
		}
	}

	private void unload() {
		try {
			currentPanel().onUnload();
		}
		catch (Exception e) {
			showErrorMessage("Import Error", e);
		}
	}

	public void done() {
		unload();
		setVisible(false);
		dispose();
		fireActionEvent("ok");
	}

	public void cancel() {
		wiz.cancel();
		setVisible(false);
		dispose();
		fireActionEvent("cancel");
	}

	void showErrorMessage(String message, Exception e) {
		if (message == null)
			message = "Error";
		log.warn(message, e);
		JOptionPane.showMessageDialog(this, message + "\n" + 
				e.getClass().getName() + "\n" + e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
	}

	public void reportException(final String message, final Exception e) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				showErrorMessage(message, e);
				update();
			}
		});
	}

	// ---- ActionListener support --------------------------------------------

	public void addActionListener(ActionListener listener) {
		actionListeners.addActionListener(listener);
	}

	public void removeActionListener(ActionListener listener) {
		actionListeners.removeActionListener(listener);
	}

	private void fireActionEvent(String command) {
		actionListeners.fireActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command, System.currentTimeMillis(), 0));
	}
}
