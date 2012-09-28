package org.systemsbiology.genomebrowser.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.Event;
import org.systemsbiology.genomebrowser.app.EventListener;
import org.systemsbiology.genomebrowser.app.EventSupport;
import org.systemsbiology.util.swing.SwingGadgets;



/**
 * Notify user if we're using a cached hbgb dataset file and confirm or replace.
 */
public class ConfirmUseCachedFile extends JFrame {
	private static final Logger log = Logger.getLogger(ConfirmUseCachedFile.class);
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>Use cached data file?</h1>" +
	"<p>By default, <b>HBGB caches data files</b> in a folder in your user directory. This avoids "  + 
	"repeatedly downloading large files and lets you keep your previous work. If you'd like " +
	"to start fresh, you can either keep the old file and start a new one or overwrite it.</p>" +
	"<p><span style=\"color:red; font-weight:bold;\">Note</span> that if you choose to " +
	"overwrite a file, any local changes to that file will be deleted. Be careful not to loose" +
	"work.</p>" +
	"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/\">Help</a></p>" +
	"</body></html>";
	private EventSupport eventSupport = new EventSupport();
	private JRadioButton useCachedFileButton;
	private JRadioButton newFileButton;
	private JRadioButton overwriteButton;
	public static String CACHED_FILE = "cached-file";
	public static String NEW_FILE = "new-file";
	public static String OVERWRITE = "overwrite";
	private JTextArea filenameTextArea;


	public ConfirmUseCachedFile() {
		super("Use cached dataset?");
		initGui();
	}

	private void initGui() {
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				cancel();
			}
		});

		// map escape and command-w to cancel the dialog
		this.getRootPane().getActionMap().put("close-window-on-escape", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		this.getRootPane().getActionMap().put("ok", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});
		this.getRootPane().getActionMap().put("next-option", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				nextOption();
			}
		});
		this.getRootPane().getActionMap().put("prev-option", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				prevOption();
			}
		});
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "ok");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "next-option");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "prev-option");

		addWindowFocusListener(new WindowAdapter() {
		    public void windowGainedFocus(WindowEvent e) {
		    	useCachedFileButton.requestFocusInWindow();
		    }
		});

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);
		instructions.setPreferredSize(new Dimension(420,200));

		useCachedFileButton = new JRadioButton("Use cached file");
		useCachedFileButton.setSelected(true);
		newFileButton = new JRadioButton("Create new file");
		overwriteButton = new JRadioButton("Overwrite cached file");

		ButtonGroup group = new ButtonGroup();
		group.add(useCachedFileButton);
		group.add(newFileButton);
		group.add(overwriteButton);

		JPanel optionsPanel = new JPanel();
		optionsPanel.setLayout(new GridLayout(3,1));
		optionsPanel.add(useCachedFileButton);
		optionsPanel.add(newFileButton);
		optionsPanel.add(overwriteButton);
		
		filenameTextArea = new JTextArea();
		filenameTextArea.setFont(filenameTextArea.getFont().deriveFont(9.0f));
		filenameTextArea.setEditable(false);

		JPanel filenamePanel = new JPanel();
		filenamePanel.setLayout(new BorderLayout());
		filenamePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
		filenamePanel.add(new JLabel("Filename:"), BorderLayout.NORTH);
		filenamePanel.add(new JScrollPane(filenameTextArea), BorderLayout.CENTER);
		filenamePanel.setPreferredSize(new Dimension(200,100));

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BorderLayout());
		controlPanel.add(filenamePanel, BorderLayout.CENTER);
		controlPanel.add(optionsPanel, BorderLayout.SOUTH);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});
		this.getRootPane().setDefaultButton(okButton);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);

		// lay out frame
		JPanel mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(6,12,6,12));
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(instructions, BorderLayout.NORTH);
		mainPanel.add(controlPanel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		add(mainPanel);
		pack();

		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((d.width - getWidth()) / 2, (d.height - getHeight())/2);
}

	private void nextOption() {
		if (useCachedFileButton.isSelected()) newFileButton.setSelected(true);
		else if (newFileButton.isSelected()) overwriteButton.setSelected(true);
		else if (overwriteButton.isSelected()) useCachedFileButton.setSelected(true);
	}

	private void prevOption() {
		if (useCachedFileButton.isSelected()) overwriteButton.setSelected(true);
		else if (newFileButton.isSelected()) useCachedFileButton.setSelected(true);
		else if (overwriteButton.isSelected()) newFileButton.setSelected(true);
	}

	public String getSelection() {
		if (useCachedFileButton.isSelected()) return CACHED_FILE;
		if (newFileButton.isSelected()) return NEW_FILE;
		if (overwriteButton.isSelected()) return OVERWRITE;
		return CACHED_FILE;
	}

	public void close() {
		this.setVisible(false);
		this.dispose();
	}

	protected void cancel() {
		eventSupport.fireEvent(this, "cancel", getSelection());
	}

	private void ok() {
		eventSupport.fireEvent(this, "ok", getSelection());
	}

	public void setFilename(String filename) {
		filenameTextArea.setText(filename);
	}

	public void addEventListener(EventListener listener) {
		eventSupport.addEventListener(listener);
	}

	public void removeEventListener(EventListener listener) {
		eventSupport.removeEventListener(listener);
	}




	public static String confirmUseCachedFile(String filename) {
		final ConfirmTask confirmTask = new ConfirmTask();
		confirmTask.filename = filename;

		// create confirm dialog on swing thread
		if (SwingUtilities.isEventDispatchThread()) {
			throw new RuntimeException("don't run this on the swing event thread!");
		}
		
		SwingUtilities.invokeLater(confirmTask);

		// wait for user to make selection
		try {
			synchronized (confirmTask.monitor) {
				if (!confirmTask.done)
					confirmTask.monitor.wait();
			}
		}
		catch (InterruptedException e) {
			log.warn(e);
		}

		return confirmTask.result;
	}

	private static class ConfirmTask implements Runnable {
		volatile Object monitor = new Object();
		volatile String result;
		volatile boolean done;
		volatile String filename;
		
		// on close window?

		public void run() {
			final ConfirmUseCachedFile dialog = new ConfirmUseCachedFile();
			dialog.setFilename(filename);
			dialog.addEventListener(new EventListener() {
				public void receiveEvent(Event event) {
					log.info("confirm use cached file returned: " + event.getData());
					result = (String)event.getData();
					dialog.close();
					synchronized (monitor) {
						done = true;
						monitor.notifyAll();
					}
				}
			});
			dialog.setVisible(true);
		}
	}



	public static void main(String[] args) {
		final ConfirmUseCachedFile frame = new ConfirmUseCachedFile();
		frame.addEventListener(new EventListener() {
			public void receiveEvent(Event event) {
				if ("ok".equals(event.getAction())) {
					System.out.println("result = " + event.getData());
					frame.close();
				}
				else if ("cancel".equals(event.getAction())) {
					frame.close();
				}
				else {
					System.out.println("Unknown action: " + event.getAction());
				}
			}
		});
		frame.setVisible(true);
	}
}
