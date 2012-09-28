package org.systemsbiology.genomebrowser.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.*;

import org.systemsbiology.genomebrowser.app.Application;
import org.systemsbiology.genomebrowser.app.ProjectDescription;
import org.systemsbiology.ucscgb.Genome;
import org.systemsbiology.ucscgb.UCSCGB;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.FileUtils;
import static org.systemsbiology.util.StringUtils.isNullOrEmpty;

import org.systemsbiology.util.swing.SwingGadgets;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;



// TODO implement NCBI import
// TODO implement file import
// TODO handle unknown organism


/**
 * This dialog asks the user to select an organism to create a new project. By default,
 * we'll make up a filename and put it in a standard location, and we'll pick a data source
 * (if possible) from which to import genome information. The Options subdialog can be used
 * to manually specify the path, filename, and data source.
 * 
 * The dialog returns a ProjectDescription which may be populated with the optional settings
 * or those settings may be blank, relying on defaulting to be applied later. At minimum the
 * organism name will be filled in.
 * 
 * @author cbare
 */
public class NewProjectDialog extends JDialog {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>New Project</h1>" +
	"<p>A project usually starts with a genome, which <b>defines the sequences</b> " +
	"(chromosomes, plasmids, etc.) you'll be working with. Select an organism below by" +
	"pressing <b>enter</b> or <b>double-clicking</b> and the program will automatically" +
	"download its genome.</p>" +
	"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/new/\">Help</a></p>" +
	"</body></html>";

	private Application app;
	private JTextField organismTextField;
	private JList organismList;
	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();
	private JButton okButton;
	private FilterList<Genome> filteredGenomes;
	private Genome selectedGenome;
	private ProjectDescription projectDescription;
	private EventList<Genome> genomes;


	public NewProjectDialog(JFrame parent, Application app) {
		super(parent, "New Project", true);
		this.projectDescription = new ProjectDescription();
		this.app = app;
		initGui();
		setLocationRelativeTo(parent);
	}

	private void initGui() {
		Box hbox;

		this.setLayout(new BorderLayout());

		// map escape and command-w to cancel the dialog
		this.getRootPane().getActionMap().put("close-window-on-escape", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		this.getRootPane().getActionMap().put("options", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				options();
			}
		});
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "options");		

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);
		instructions.setPreferredSize(new Dimension(405,160));

		Box vbox = Box.createVerticalBox();
		vbox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		vbox.setOpaque(false);
		this.add(vbox, BorderLayout.CENTER);

		vbox.add(instructions);

		hbox = Box.createHorizontalBox();
		JLabel organismLabel = new JLabel("Organism:");
		hbox.add(organismLabel);
		organismTextField = new JTextField();
		hbox.add(organismTextField);
		vbox.add(hbox);

		// TODO make extra space go to organismList rather organismTextField

		organismTextField.getActionMap().put("focus-to-list", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				organismList.requestFocusInWindow();
			}
		});
		im = organismTextField.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "focus-to-list");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0, false), "focus-to-list");

		organismTextField.getActionMap().put("focus-to-OK", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				okButton.requestFocusInWindow();
			}
		});
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "focus-to-OK");

		hbox = Box.createHorizontalBox();
		hbox.add(Box.createHorizontalStrut(68));
		organismList = new JList();
		organismList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		hbox.add(new JScrollPane(organismList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		vbox.add(hbox);

		organismList.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				if (organismList.getSelectedIndex()<0 || organismList.getSelectedIndex()>=filteredGenomes.size())
					organismList.setSelectedIndex(0);
			}
			public void focusLost(FocusEvent e) {}
		});
		organismList.getActionMap().put("selection-to-text-field", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				selectedGenome = (Genome)organismList.getSelectedValue();
				String name = selectedGenome.getScientificName();
				String currentText = organismTextField.getText();
				organismTextField.setText(name);
				if (name.startsWith(currentText)) {
					organismTextField.setSelectionStart(currentText.length());
					organismTextField.setSelectionEnd(name.length());
				}
				else {
					organismTextField.setSelectionStart(0);
					organismTextField.setSelectionEnd(name.length());
				}
				//organismTextField.getCaret().setSelectionVisible(true);
				organismTextField.requestFocusInWindow();
			}
		});
		im = organismList.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "selection-to-text-field");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "selection-to-text-field");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0, false), "selection-to-text-field");

		organismList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount()==2) {
					Action action = organismList.getActionMap().get("selection-to-text-field");
					if (action != null)
						action.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "double-click"));
				}
			}
		});

		loadGenomes();

		JButton optionsButton = new JButton("Options");
		optionsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				options();
			}
		});

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
		buttonPanel.add(optionsButton);
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		this.add(buttonPanel, BorderLayout.SOUTH);

		this.pack();
		organismTextField.requestFocusInWindow();
	}

	private void loadGenomes() {
		UCSCGB ucsc = new UCSCGB();
		List<Genome> genomeSource = ucsc.loadGenomes(null);
		Collections.sort(genomeSource, Genome.comparator);
		genomes = GlazedLists.eventList(genomeSource);
		TextFilterator<Genome> filterator = new TextFilterator<Genome>() {
			public void getFilterStrings(List<String> keywords, Genome g) {
				keywords.add(g.getDomain());
				keywords.add(g.getClade());
				if (g.getScientificName() != null) {
					keywords.add(g.getScientificName());
					String[] words = g.getScientificName().split(" ");
					if (words.length > 1)
						for (String keyword : words)
							keywords.add(keyword);
				}
				if (g.getGenome() != null) {
					keywords.add(g.getGenome());
					String[] words = g.getGenome().split(" ");
					if (words.length > 1)
						for (String keyword : words)
							keywords.add(keyword);
				}
			}
		};
		TextComponentMatcherEditor<Genome> tme = new TextComponentMatcherEditor<Genome>(organismTextField, filterator);
		tme.setMode(TextMatcherEditor.STARTS_WITH);
		filteredGenomes = new FilterList<Genome>(genomes, tme);

		organismList.setModel(new EventListModel<Genome>(filteredGenomes));
	}

	/**
	 * copy organism name and genome details, if available to the projectDescription
	 */
	private void updateProjectDescription() {
		String species = organismTextField.getText();
		projectDescription.setOrganism(species);
	}

	private void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.WARNING_MESSAGE, FileUtils.getIconOrBlank("error_icon.png"));
	}

	@SuppressWarnings("unused")
	private boolean showQuestion(String message) {
		return (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(this, message, "OK?", JOptionPane.YES_NO_OPTION));
	}

	public void close() {
		setVisible(false);
		dispose();
	}

	public void addDialogListener(DialogListener listener) {
		listeners.add(listener);
	}

	public void removeDialogListener(DialogListener listener) {
		listeners.remove(listener);
	}

	public void options() {
		updateProjectDescription();
		final NewProjectOptionsDialog optionsDialog = new NewProjectOptionsDialog(this, app, projectDescription);
		optionsDialog.addDialogListener(new DialogListener() {
			public void cancel() {}

			public void error(String message, Exception e) {
				showErrorMessage(message);
			}

			public void ok(String action, Object result) {
				projectDescription.transfer((ProjectDescription)result);
			}
		});
		optionsDialog.setVisible(true);
	}

	public void cancel() {
		close();
		for (DialogListener listener: listeners) {
			listener.cancel();
		}
	}

	public void ok() {
		String organism = organismTextField.getText().trim();
		Genome genome = findGenomeBySpeciesName(organism);
		if (genome==null) {
			String message = "The organism you selected \"" + String.valueOf(organism) + "\" \n" +
					"is not in the list of known species. You'll have to import local files or \n" +
					"manually define the chromosomes or sequences you want to work with. Do you \n" +
					"want to continue anyway?";
			int result = JOptionPane.showConfirmDialog(this, message, "Unknown Organism", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, FileUtils.getIconOrBlank("Help_48x48.png"));
			if (result==JOptionPane.NO_OPTION) return;
			updateProjectDescription();
			projectDescription.setDataSource("local files");
		}
		else {
			updateProjectDescription();
		}
		close();
		for (DialogListener listener: listeners) {
			listener.ok("ok", projectDescription);
		}
	}

	private Genome findGenomeBySpeciesName(String organism) {
		if (organism==null) return null;
		for (Genome genome: genomes) {
			if (organism.equals(genome.getScientificName())) {
				return genome;
			}
		}
		return null;
	}

	public static void main(String[] args) {
		final JFrame frame = new JFrame("test");
		frame.pack();
		frame.setVisible(true);

		final NewProjectDialog dialog = new NewProjectDialog(frame, null);
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
				System.out.println("action= " + action);
				System.out.println(result);
				System.exit(0);
			}
		});
		dialog.setVisible(true);
	}
}



/**
 * Set options for creating a new dataset:
 * 
 * <ul>
 * <li> set filename/location
 * <li> set dataset name
 * <li> import genome
 * <li> select data source (NCBI/UCSC/etc.)
 * </ul>
 * 
 * @author cbare
 */
class NewProjectOptionsDialog extends JDialog {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h1>New Project Options</h1>" +
	"<p>Genome browser <b>projects</b> are stored in local files (a " +
	"<a href=\"http://www.sqlite.org/\">Sqlite</a> database file). By default, the file is stored " +
	"in /hbgb under the user's document directory (depending on platform) with a name generated " +
	"from the species. Alternaively, you can select a <b>path</b> and <b>filename</b> here.</p>" +
	"<p>Genomes can be automatically imported from <a href=\"http://www.ncbi.nlm.nih.gov/\">NCBI</a>, " +
	"<a href=\"http://genome.ucsc.edu/\">UCSC genome browser</a> or " +
	"the <a href=\"http://microbes.ucsc.edu/\">UCSC archaeal genome browser</a>. Alternatively, " +
	"chromosome and track information can be imported from local files.</p>" +
	"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/new/\">Help</a></p>" +
	"</body></html>";

	private Application app;
	private JTextField filenameTextField;
	private JTextField nameTextField;
	private JCheckBox importGenomeCheckBox;
	private JComboBox dataSourceChooser;
	private ProjectDescription projectDescription;
	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();


	public NewProjectOptionsDialog(JDialog parent, Application app, ProjectDescription desc) {
		super(parent, "New Project Options", true);
		this.app = app;
		this.projectDescription = desc;
		initGui();
		setLocationRelativeTo(parent);
	}

	private void initGui() {

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
		instructions.setPreferredSize(new Dimension(405,200));

		AbstractAction browseAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				browse();
			}
		};

		filenameTextField = new JTextField();
		filenameTextField.getActionMap().put("enter-key-handler", browseAction);
		im = filenameTextField.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "enter-key-handler");

		// set initial value of file field
		if (projectDescription.getFile()!=null)
			filenameTextField.setText(projectDescription.getFile().getAbsolutePath());
		else if (!isNullOrEmpty(projectDescription.getOrganism())) {
			if (app != null) {
				File file = app.getProjectDefaults().getDefaultFile(projectDescription.getOrganism());
				filenameTextField.setText(file.getAbsolutePath());
			}
			else {
				File file = new File(System.getProperty("user.home"), FileUtils.toValidFilename(projectDescription.getOrganism())+".hbgb");
				filenameTextField.setText(file.getAbsolutePath());
			}
		}

		JButton browseButton = new JButton("Browse");
		browseButton.addActionListener(browseAction);

		File dir = (app==null) ? new File(System.getProperty("user.home")) : app.options.dataDirectory;
		JLabel workingDirLabel = new JLabel(String.format("(data dir: %s)", dir.getAbsolutePath()));
		workingDirLabel.setFont(workingDirLabel.getFont().deriveFont(9.0f));

		nameTextField = new JTextField();
		if (!isNullOrEmpty(projectDescription.getProjectName())) {
			nameTextField.setText(projectDescription.getProjectName());
		}
		else if (!isNullOrEmpty(projectDescription.getOrganism())) {
			nameTextField.setText(projectDescription.getOrganism());
		}

		importGenomeCheckBox = new JCheckBox("Import Genome: ", true);
		importGenomeCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
		dataSourceChooser = new JComboBox(new String[] {"NCBI", "UCSC", "local files"});
		dataSourceChooser.setSelectedIndex(1);
		dataSourceChooser.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED) {
					if (!"UCSC".equals(e.getItem())) {
						JOptionPane.showMessageDialog(NewProjectOptionsDialog.this, String.valueOf(e.getItem()) + " not implemented yet!", "Doh!", JOptionPane.WARNING_MESSAGE);
					}
				}
			}
		});

		JButton doneButton = new JButton("OK");
		doneButton.addActionListener(new ActionListener() {
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
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(2,2,2,2);
		this.add(browseButton, c);

		c.gridx = 1;
		c.gridy++;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(4,12,2,8);
		this.add(workingDirLabel, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,2,2);
		this.add(new JLabel("Project name:"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(12,2,2,2);
		this.add(nameTextField, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,2,2);
		this.add(importGenomeCheckBox, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.insets = new Insets(12,12,2,8);
		this.add(new JLabel("Data source:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(12,2,2,8);
		this.add(dataSourceChooser, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(16,2,16,2);
		this.add(buttonPanel, c);		

		this.pack();
		this.setSize(Math.max(500, this.getWidth()), this.getHeight());
	}
	
	public void browse() {
		String path = filenameTextField.getText();

		JFileChooser chooser = DatasetFileChooser.getNewDatasetFileChooser();
		chooser.setDialogTitle("New Project file");
		File file = null;
		if (isNullOrEmpty(path)) {
			file = new File(System.getProperty("user.home"));
		}
		else {
			file = new File(path);
			if (!file.isAbsolute()) {
				if (app != null)
					file = new File(app.options.dataDirectory, path);
				else
					file = new File(System.getProperty("user.home"), path);
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
			String f = chooser.getSelectedFile().getAbsolutePath();
			if ("".equals(FileUtils.extension(f)))
				f += ".hbgb";
			filenameTextField.setText(f);
		}
	}

	public void ok() {
		close();
		for (DialogListener listener: listeners) {
			ProjectDescription description = new ProjectDescription(
					null,
					nameTextField.getText(),
					new File(filenameTextField.getText()),
					(String)dataSourceChooser.getSelectedItem());

			listener.ok("ok", description);
		}
	}

	public void cancel() {
		close();
		for (DialogListener listener: listeners) {
			listener.cancel();
		}
	}

	private void close() {
		this.setVisible(false);
		dispose();
	}

	public void addDialogListener(DialogListener listener) {
		listeners.add(listener);
	}

	public void removeDialogListener(DialogListener listener) {
		listeners.remove(listener);
	}
}
