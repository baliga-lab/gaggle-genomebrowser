package org.systemsbiology.genomebrowser.ui;

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
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;

import org.systemsbiology.genomebrowser.bookmarks.Bookmark;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.InvertionUtils;
import org.systemsbiology.util.BrowserUtil;


/**
 * Dialog for defining and editing a bookmark.
 * 
 * @author cbare
 */
@SuppressWarnings("serial")
public class BookmarkDialog extends JDialog {
	private BookmarkDataSource bookmarkDataSource;
	private Bookmark bookmark;
	private JTextField nameTextField;
	private JTextField chromosomeTextField;
	private JTextField startTextField;
	private JTextField endTextField;
	private JTextArea sequence;
	private JTextArea annotation;
	private JComboBox strandSelector;


	public BookmarkDialog(Frame owner, BookmarkDataSource bookmarkDataSource) {
		super(owner, "Bookmarks");
		this.bookmarkDataSource = bookmarkDataSource;
		this.getContentPane().add(createMainPanel());
		this.setSize(440,460); // size of BookmarkDialog  original = 440,300
		this.setMinimumSize(new Dimension(300,200)); //original 300,200

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

	public BookmarkDialog(Frame owner, BookmarkDataSource bookmarkDataSource, Bookmark bookmark) {
		this(owner, bookmarkDataSource);
		this.bookmark = bookmark;
		if (bookmark != null)
			populateFormFields(bookmark);
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
		panel.add(new JLabel("Sequence:"), c);
		
		c.gridy++;
		panel.add(new JLabel("Annotation:"), c);

		nameTextField = new JTextField();
		chromosomeTextField = new JTextField(12);
		startTextField = new JTextField(8);
		endTextField = new JTextField(8);
		strandSelector = new JComboBox(new String[] {"none", "forward", "reverse"});
		strandSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//(dmartinez)
				changeSelector();
			}
		});
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

		
		// adding seqTextArea
		sequence = new JTextArea();
		sequence.setLineWrap(true);
		sequence.setWrapStyleWord(true);
		sequence.setFocusTraversalKeysEnabled(true);
		Set<KeyStroke> keys2 = new HashSet<KeyStroke>();
		keys2.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		sequence.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys2);
		keys2.clear();
		keys2.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK));
		sequence.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys2);

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

		c.gridy++;
		panel.add(endTextField, c);

		c.gridy++;
		panel.add(strandSelector, c);
		
		// adding a seqTextArea (dmartinez)
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.gridheight = 1; // for border 
		c.weightx = 1.0;
		c.weighty = 1.0;
		panel.add(new JScrollPane(sequence),c);

		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.gridheight = 2;
		c.weightx = 1.0;
		c.weighty = 1.0;
		panel.add(new JScrollPane(annotation), c);

		return panel;
	}

	private Component createButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		JButton okButton = new JButton("OK");
		okButton.setFocusable(isFocusable());
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveBookmark();
			}
		});
//		this.getRootPane().setDefaultButton(okButton);
		
		JButton blastButton = new JButton("BLAST");
		blastButton.setFocusable(isFocusable());
		blastButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//System.out.println("clicou no blast");
				//openBrowser open = new openBrowser();
				try {
					BrowserUtil.openUrl("http://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Web&PAGE=Nucleotides&DATABASE=nr&BLAST_PROGRAMS=discoMegablast&QUERY="+sequence.getText());
				} catch (Exception e1) {
					showErrorMessage("Can't start browser: " + e1.getMessage());
				}
			}
		});		
		
		panel.add(okButton); // adiciona botão ok
		
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				done();
			}
		});
		panel.add(cancelButton);
		panel.add(blastButton); // add Blast Buttom

		// map escape and command-w to cancel the dialog
		this.getRootPane().getActionMap().put("close-window-on-escape", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				done();
			}
		});
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		return panel;
	}

	private boolean validateForm() {
		if (isBlank(nameTextField.getText())) {
			showErrorMessage("Name can't be left blank.");
			return false;
		}
		try {
			Integer.parseInt(startTextField.getText().trim());
			Integer.parseInt(endTextField.getText().trim());
		}
		catch (NumberFormatException e) {
			showErrorMessage("Start and End fields must be integers.");
			return false;
		}
		return true;
	}

	private boolean isBlank(String s) {
		return s==null || "".equals(s.trim());
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE);
	}


	public void saveBookmark() {
		// updating a bookmark is done by replacing it with a new bookmark
		if (validateForm()) {
			Bookmark newBookmark = new Bookmark(
					chromosomeTextField.getText().trim(),
					Strand.fromString(String.valueOf(strandSelector.getSelectedItem())),
					Integer.parseInt(startTextField.getText().trim()),
					Integer.parseInt(endTextField.getText().trim()),
					nameTextField.getText().trim(),
					annotation.getText().trim(),
					null,
					sequence.getText().trim()
			);
			
			if (bookmark == null) {
				bookmarkDataSource.add(newBookmark);
			}
			else {
				newBookmark.setAssociatedFeatureNames(bookmark.getAssociatedFeatureNames());
				bookmarkDataSource.update((bookmark), newBookmark);

			}
		}
		done();
	}

	public void done() {
		this.setVisible(false);
		this.dispose();
	}

	public void changeSelector(){
		Strand newStrand = Strand.fromString(String.valueOf(strandSelector.getSelectedItem()));

		// if we're switching strands, reverse compliment the sequence
		// we'll use no strand as same as forward
		if ( (newStrand==Strand.reverse && (bookmark.getStrand()==Strand.forward || bookmark.getStrand()==Strand.none)) ||
			 (bookmark.getStrand()==Strand.reverse && (newStrand==Strand.forward || newStrand==Strand.none))	             ) {
			bookmark.setSequence(InvertionUtils.inversion(sequence.getText()));
		}
		bookmark.setStrand(newStrand);
		populateFormFields(bookmark);
	}

	private void populateFormFields(Feature bookmark) {
		startTextField.setText(String.valueOf(bookmark.getStart()));
		chromosomeTextField.setText(String.valueOf(bookmark.getSeqId()));
		endTextField.setText(String.valueOf(bookmark.getEnd()));
		nameTextField.setText( bookmark.getLabel() );
		if (bookmark instanceof Bookmark) {
			annotation.setText(((Bookmark)bookmark).getAnnotation());
			sequence.setText(((Bookmark) bookmark).getSequence());
			strandSelector.setSelectedItem(String.valueOf(bookmark.getStrand())); //?
		}
		else {
			annotation.setText("");
			sequence.setText("");
		}
	}
}
