package org.systemsbiology.genomebrowser.transcript;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.bookmarks.Bookmark;
import org.systemsbiology.genomebrowser.model.*;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.swing.ArrowKeyListener.IntArrowKeyListener;

/**
 * Dialog for annotating transcription start and stop sites, based on the new-bookmark dialog.
 */
@SuppressWarnings("serial")
public class TranscriptBoundaryDialog extends JDialog implements TranscriptBoundaryPlugin.TranscriptBoundaryListener {
	private static final Logger log = Logger.getLogger(TranscriptBoundaryDialog.class);
	TranscriptBoundaryPlugin transcriptBoundaryPlugin;
//	private BookmarkDataSource bookmarkDataSource;
//	private Bookmark bookmark;
	private JTextField nameTextField;
	private JTextField chromosomeTextField;
	private JTextField startTextField;
	private JTextField endTextField;
	private JTextArea annotation;
	private JComboBox strandSelector;
	private JCheckBox ok5Prime;
	private JCheckBox ok3Prime;
	private JCheckBox okOverall;
	
	private JButton snapStartLeftButton;
	private JButton snapStartRightButton;
	private JButton snapEndLeftButton;
	private JButton snapEndRightButton;

	private Action snapStartLeftAction;
	private Action snapStartRightAction;
	private Action snapEndLeftAction;
	private Action snapEndRightAction;
	
	private boolean suppressEvents;



	public TranscriptBoundaryDialog(Frame owner, TranscriptBoundaryPlugin transcriptBoundaryPlugin) {
		super(owner, "Transcription Boundaries");
		this.transcriptBoundaryPlugin = transcriptBoundaryPlugin;

		snapStartLeftAction = new SnapStartLeftAction();
		snapStartRightAction = new SnapStartRightAction();
		snapEndLeftAction = new SnapEndLeftAction();
		snapEndRightAction = new SnapEndRightAction();

		this.getContentPane().add(createMainPanel());
		this.setSize(440,360);
		this.setMinimumSize(new Dimension(300,200));
		
		populateFormFields(transcriptBoundaryPlugin.getBookmark());

		// position the dialog
		Point p = owner.getLocation();
		int overlap = 30;
		if (p.x > 50) {
			this.setLocation(Math.max(overlap, p.x - this.getWidth() + overlap), p.y + 80);
		}
		else {
			int w = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
			this.setLocation(Math.min(w - this.getWidth() - overlap, p.x + owner.getWidth() - overlap), p.y + 80);
		}
	}

	private Component createMainPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

		panel.add(createBookmarkEditor(), BorderLayout.CENTER);
		panel.add(createButtonPanel(), BorderLayout.SOUTH);

		return panel;
	}

	private Component createBookmarkEditor() {

		// map escape and command-w to cancel the dialog
		this.getRootPane().getActionMap().put("close-window-on-escape", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				done();
			}
		});
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(3,0,3,5);
		panel.add(new JLabel("Name:"), c);

		c.gridy++;
		panel.add(new JLabel("Chromosome:"), c);

		c.gridy++;
		panel.add(new JLabel("Start:"), c);

		c.gridy++;
		panel.add(new JLabel("End:"), c);

		c.gridy++;
		panel.add(new JLabel("Strand:"), c);

		c.gridy++;
		JLabel okLabel = new JLabel("OK:");
		okLabel.setToolTipText("Indicate clarity or uncertainty of transcript boundaries.");
		panel.add(okLabel, c);

		c.gridy++;
		panel.add(new JLabel("Annotation:"), c);

		nameTextField = new JTextField();
		chromosomeTextField = new JTextField(12);

		// This runnable is used to pass a function to the IntArrowKeyListeners below.
		// It gets run in the UI dispatch thread.
		Runnable update = new Runnable() {
			public void run() {
				if (validateFormQuietly()) {
					update();
				}
			}
		};

		startTextField = new JTextField(8);
		startTextField.addKeyListener(new IntArrowKeyListener(startTextField, update));

		endTextField = new JTextField(8);
		endTextField.addKeyListener(new IntArrowKeyListener(endTextField, update));

		snapStartLeftButton = new JButton(snapStartLeftAction);
		snapStartRightButton = new JButton(snapStartRightAction);
		snapEndLeftButton = new JButton(snapEndLeftAction);
		snapEndRightButton = new JButton(snapEndRightAction);
		
		ItemListener updateItemListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED) {
					update();
				}
			}		
		};
		
		ok5Prime = new JCheckBox("5'");
		ok5Prime.addItemListener(updateItemListener);
		ok5Prime.setToolTipText("Check if 5' end of transcript appears well defined.");
		ok3Prime = new JCheckBox("3'");
		ok3Prime.addItemListener(updateItemListener);
		ok3Prime.setToolTipText("Check if 3' end of transcript appears well defined.");
		okOverall = new JCheckBox("overall");
		okOverall.addItemListener(updateItemListener);
		okOverall.setToolTipText("Check if probability of transcription is consistently high over the transcript.");

		strandSelector = new JComboBox(new String[] {"none", "forward", "reverse"});
		strandSelector.addItemListener(updateItemListener);

		annotation = new JTextArea();
		annotation.setLineWrap(true);
		annotation.setWrapStyleWord(true);
		annotation.setFocusTraversalKeysEnabled(true);
		Set<KeyStroke> keys = new HashSet<KeyStroke>();
		keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		annotation.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);
		keys.clear();
		keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK));
		annotation.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);

		// hack to work around strange behavior when resizing the dialog
		startTextField.setMinimumSize(startTextField.getPreferredSize());
		endTextField.setMinimumSize(endTextField.getPreferredSize());
		chromosomeTextField.setMinimumSize(chromosomeTextField.getPreferredSize());

		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1;
		c.insets = new Insets(3,0,3,0);
		panel.add(nameTextField, c);

		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		panel.add(chromosomeTextField, c);

		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		panel.add(startTextField, c);

		c.gridx = 2;
		panel.add(snapStartLeftButton, c);

		c.gridx = 3;
		panel.add(snapStartRightButton, c);

		c.gridx = 1;
		c.gridy++;
		panel.add(endTextField, c);

		c.gridx = 2;
		panel.add(snapEndLeftButton, c);

		c.gridx = 3;
		panel.add(snapEndRightButton, c);

		c.gridx = 1;
		c.gridy++;
		c.gridwidth = 3;
		panel.add(strandSelector, c);

		c.gridx = 1;
		c.gridy++;
		JPanel p = new JPanel();
		p.add(ok5Prime);
		p.add(ok3Prime);
		p.add(okOverall);
		panel.add(p, c);

		c.gridy++;
		c.gridx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 4;
		c.gridheight = 2;
		c.weightx = 1.0;
		c.weighty = 1.0;
		panel.add(new JScrollPane(annotation), c);

		return panel;
	}

	private Component createButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JButton okButton = new JButton("Save & Next");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveAndNextBookmark();
			}
		});
		// default button doesn't work well w/ having return save numeric text fields
//		this.getRootPane().setDefaultButton(okButton);
		okButton.setToolTipText("Save the bookmark and go to next transcript.");
		panel.add(okButton);

		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});
		saveButton.setToolTipText("Save the bookmark.");
		panel.add(saveButton);

		JButton nextButton = new JButton("Next");
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nextBookmark();
			}
		});
		nextButton.setToolTipText("Find to next transcript.");
		panel.add(nextButton);

		JButton cancelButton = new JButton("Done");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				done();
			}
		});
		panel.add(cancelButton);

		return panel;
	}

	private void validateForm() {
		if (isBlank(nameTextField.getText())) {
			throw new RuntimeException("Name can't be left blank.");
		}
		try {
			Integer.parseInt(startTextField.getText().trim());
			Integer.parseInt(endTextField.getText().trim());
		}
		catch (NumberFormatException e) {
			throw new RuntimeException("Start and End fields must be integers.");
		}
	}

	private boolean validateFormQuietly() {
		try {
			validateForm();
			return true;
		}
		catch (Exception e) {
			log.info(e);
			return false;
		}
	}

	private boolean isBlank(String s) {
		return s==null || "".equals(s.trim());
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE);
	}

	public void saveAndNextBookmark() {
		try {
			validateForm();

			Attributes attributes = new Attributes();
			attributes.put("ok5Prime", ok5Prime.isSelected());
			attributes.put("ok3Prime", ok3Prime.isSelected());
			attributes.put("okOverall", okOverall.isSelected());

			transcriptBoundaryPlugin.saveAndGotoNextTranscript(
					chromosomeTextField.getText().trim(),
					Strand.fromString(String.valueOf(strandSelector.getSelectedItem())),
					Integer.parseInt(startTextField.getText().trim()),
					Integer.parseInt(endTextField.getText().trim()),
					nameTextField.getText().trim(),
					annotation.getText().trim(),
					attributes.toAttributesString());
		}
		catch (Exception e) {
			showErrorMessage(e.getMessage());
		}
	}

	private void nextBookmark() {
		transcriptBoundaryPlugin.gotoNextTranscript();
	}

	private void update() {
		if (!suppressEvents) {
			log.info("Updating bookmark...");

			Attributes attributes = new Attributes();
			attributes.put("ok5Prime", ok5Prime.isSelected());
			attributes.put("ok3Prime", ok3Prime.isSelected());
			attributes.put("okOverall", okOverall.isSelected());

			transcriptBoundaryPlugin.update(
					chromosomeTextField.getText().trim(),
					Strand.fromString(String.valueOf(strandSelector.getSelectedItem())),
					Integer.parseInt(startTextField.getText().trim()),
					Integer.parseInt(endTextField.getText().trim()),
					nameTextField.getText().trim(),
					annotation.getText().trim(),
					attributes.toAttributesString());
		}
	}
	
	private void save() {
		update();
		transcriptBoundaryPlugin.save();
	}

	public void done() {
		// hack: this will have been added as a listener in TranscripBoundaryPlugin.showTranscriptBoundaryDialog()
		transcriptBoundaryPlugin.done(this);
		this.setVisible(false);
		this.dispose();
	}

	private void populateFormFields(Bookmark bookmark) {
		suppressEvents = true;
		if (bookmark==null) {
			startTextField.setText("");
			chromosomeTextField.setText("");
			endTextField.setText("");
			nameTextField.setText("");
			strandSelector.setSelectedItem("");
			annotation.setText("");
			ok5Prime.setSelected(false);
			ok3Prime.setSelected(false);
			okOverall.setSelected(false);
		}
		else {
			startTextField.setText(String.valueOf(bookmark.getStart()));
			chromosomeTextField.setText(String.valueOf(bookmark.getSeqId()));
			endTextField.setText(String.valueOf(bookmark.getEnd()));
			nameTextField.setText(bookmark.getLabel());
			strandSelector.setSelectedItem(String.valueOf(bookmark.getStrand()));
			annotation.setText(String.valueOf(bookmark.getAnnotation()));
			
			Attributes a = bookmark.getAttributes();
			ok5Prime.setSelected(a.getBoolean("ok5Prime", false));
			ok3Prime.setSelected(a.getBoolean("ok3Prime", false));
			okOverall.setSelected(a.getBoolean("okOverall", false));
		}
		suppressEvents = false;
	}

//	public void changeEvent(int start, int end, Strand targetStrand) {
//		startTextField.setText(String.valueOf(start));
//		endTextField.setText(String.valueOf(end));
//	}

	public void bookmarkUpdateEvent(Bookmark bookmark) {
		log.info("received bookmark update event: " + String.format("bookmark = %s %s%s:%d-%d", bookmark.getLabel(), bookmark.getSeqId(), bookmark.getStrand().toAbbreviatedString(), bookmark.getStart(), bookmark.getEnd()));
		populateFormFields(bookmark);
	}

	public void nextTranscriptEvent(Bookmark bookmark) {
		// TODO Auto-generated method stub
	}


	class SnapStartLeftAction extends AbstractAction {

		public SnapStartLeftAction() {
			super();
			putValue(Action.SHORT_DESCRIPTION, "Snap start to the nearest break point to the left.");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			putValue(Action.SMALL_ICON, new ImageIcon(SnapStartLeftAction.class.getResource("/icons/go-previous.png")));
		}

		public void actionPerformed(ActionEvent e) {
			transcriptBoundaryPlugin.snapStartLeft();
		}
	}

	class SnapStartRightAction extends AbstractAction {

		public SnapStartRightAction() {
			super();
			putValue(Action.SHORT_DESCRIPTION, "Snap start to the nearest break point to the right.");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			putValue(Action.SMALL_ICON, new ImageIcon(SnapStartRightAction.class.getResource("/icons/go-next.png")));
		}

		public void actionPerformed(ActionEvent e) {
			transcriptBoundaryPlugin.snapStartRight();
		}
	}

	class SnapEndLeftAction extends AbstractAction {

		public SnapEndLeftAction() {
			super();
			putValue(Action.SHORT_DESCRIPTION, "Snap end to the nearest break point to the left.");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_BRACELEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			putValue(Action.SMALL_ICON, new ImageIcon(SnapEndLeftAction.class.getResource("/icons/go-previous.png")));
		}

		public void actionPerformed(ActionEvent e) {
			transcriptBoundaryPlugin.snapEndLeft();
		}
	}

	class SnapEndRightAction extends AbstractAction {

		public SnapEndRightAction() {
			super();
			putValue(Action.SHORT_DESCRIPTION, "Snap end to the nearest break point to the right.");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_BRACERIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			putValue(Action.SMALL_ICON, new ImageIcon(SnapEndRightAction.class.getResource("/icons/go-next.png")));
		}

		public void actionPerformed(ActionEvent e) {
			transcriptBoundaryPlugin.snapEndRight();
		}
	}
}
