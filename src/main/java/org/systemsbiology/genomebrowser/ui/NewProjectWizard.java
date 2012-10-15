package org.systemsbiology.genomebrowser.ui;

import static org.systemsbiology.util.StringUtils.isNullOrEmpty;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.*;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.Application;
import org.systemsbiology.genomebrowser.event.Event;
import org.systemsbiology.genomebrowser.event.EventListener;
import org.systemsbiology.genomebrowser.event.EventSupport;
import org.systemsbiology.genomebrowser.app.Options;
import org.systemsbiology.genomebrowser.app.ProjectDescription;
import org.systemsbiology.genomebrowser.app.ProjectDescription.SequenceDescription;
import org.systemsbiology.genomebrowser.model.Topology;
import org.systemsbiology.ucscgb.Genome;
import org.systemsbiology.ucscgb.UCSCGB;
import org.systemsbiology.util.DialogListener;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.swing.Dialogs;
import org.systemsbiology.util.swing.SwingGadgets;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;


/**
 * Walk to user through the process of creating a new dataset, defaulting
 * everything after specifying the organism.
 * 
 * @author cbare
 */
public class NewProjectWizard extends JDialog {
	private static String INSTRUCTIONS_HTML = "<html><body><h1>New project wizard</h1></body></html>";
	private Set<DialogListener> listeners = new CopyOnWriteArraySet<DialogListener>();
	private ProjectDescription projectDescription;
	private SpeciesPanel speciesPanel;
	private ProjectPropertiesPanel projectPropertiesPanel;
	private DataSourcePanel dataSourcePanel;
	private SequencesPanel sequencesPanel;
	private LoadGenomePanel loadGenomePanel;
	private NewProjectWizardPanel currentPanel;
	private AbstractAction nextAction;
	private AbstractAction backAction;
	private Application app;
	private JButton okButton;


	public NewProjectWizard(JFrame parent, Application app) {
		super(parent, "New Project");
		this.app = app;

		projectDescription = new ProjectDescription();
		speciesPanel = new SpeciesPanel();
		projectPropertiesPanel = new ProjectPropertiesPanel(app, projectDescription);
		dataSourcePanel = new DataSourcePanel(projectDescription);
		sequencesPanel = new SequencesPanel(app);
		loadGenomePanel = new LoadGenomePanel(app);

		initGui();
		setLocationRelativeTo(parent);
	}


	private void initGui() {
		setSize(540, 600); //it was (540,520) but was hidding the input dialog form  (dmartinez-dec.2011)
		setPreferredSize(new Dimension(540, 600)); // it was 540, 520
		
		AbstractAction ok = new AbstractAction("OK") {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		};
		AbstractAction cancelAction = new AbstractAction("Cancel") {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		};
		nextAction = new AbstractAction("next", FileUtils.getIconOrBlank("go-next.png")) {
			public void actionPerformed(ActionEvent e) {
				next();
			}
		};
		backAction = new AbstractAction("back", FileUtils.getIconOrBlank("go-previous.png")) {
			public void actionPerformed(ActionEvent e) {
				back();
			}
		};

//		nextAction.setEnabled(false);
//		backAction.setEnabled(false);

		// map escape and command-w to cancel the dialog
		this.getRootPane().getActionMap().put("close-window-on-escape", cancelAction);
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);

		JButton helpButton = new JButton(FileUtils.getIconOrBlank("Help_24x24.png"));
		helpButton.setToolTipText("Help on creating new genome browser projects");
		helpButton.setBorderPainted(false);
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
            Desktop.getDesktop().browse(new java.net.URI("http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/new/"));
				}
				catch (Exception e1) {
					showErrorMessage(e1.getMessage(), "Error opening help");
				}
			}
		});

		JButton backButton = new JButton(backAction);
		backButton.setToolTipText("More details - back");
		backButton.setBorder(BorderFactory.createEtchedBorder());
		backButton.setText(null);

		JButton nextButton = new JButton(nextAction);
		nextButton.setToolTipText("More details - next");
		nextButton.setText(null);
		nextButton.setBorder(BorderFactory.createEtchedBorder());

		okButton = new JButton(ok);
		okButton.getActionMap().put("press-OK", ok);
		im = okButton.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "press-OK");

		JButton cancelButton = new JButton(cancelAction);

		JPanel nextBackPanel = new JPanel();
		nextBackPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		nextBackPanel.add(backButton);
		nextBackPanel.add(nextButton);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(nextBackPanel);
		buttonPanel.add(Box.createHorizontalStrut(40));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);


		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(8,8,2,8);
		this.add(instructions, c);

		c.gridx = 2;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(0,0,0,0);
		this.add(helpButton, c);
		
//		c.gridx = 0;
//		c.gridy = 1;
//		c.gridheight = 1;
//		c.gridwidth = 3;
//		c.weightx = 1.0;
//		c.weighty = 1.0;
//		c.anchor = GridBagConstraints.NORTHWEST;
//		c.fill = GridBagConstraints.BOTH;
//		c.insets = new Insets(8,8,12,8);
//		this.add(speciesPanel, c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(12,12,24,2);
		this.add(buttonPanel, c);

		setCurrentPanel(speciesPanel);
		addWindowFocusListener(speciesPanel);

		// sequencesPanel and loadGenomePanel are used only when the data source
		// is set to "My own data". We want to get events from dataSourcePanel so
		// we know whether to enable or disable those panels.
		dataSourcePanel.addEventListener(new EventListener() {
			public void receiveEvent(Event event) {
				if ("select-datasource".equals(event.getAction()) && currentPanel==dataSourcePanel) {
					nextAction.setEnabled(ProjectDescription.LOCAL_DATA_LABEL.equals(event.getData()));
				}
			}
		});

		// handle when the user types a species and hits enter
		speciesPanel.addEventListener(new EventListener() {
			public void receiveEvent(Event event) {
				if ("species-OK".equals(event.getAction())) {
					okButton.requestFocusInWindow();
				}
			}
		});
	}

	private void next() {
		if (!updateProjectDescription()) return;
		if (currentPanel==speciesPanel) {
			setCurrentPanel(projectPropertiesPanel);
		}
		else if (currentPanel==projectPropertiesPanel)
			setCurrentPanel(dataSourcePanel);
		else if (currentPanel==dataSourcePanel) {
			// if dataSource == local
			setCurrentPanel(sequencesPanel);
		}
		else if (currentPanel==sequencesPanel) {
			setCurrentPanel(loadGenomePanel);
		}
	}

	private void back() {
		if (!updateProjectDescription()) return;
		if (currentPanel==loadGenomePanel)
			setCurrentPanel(sequencesPanel);
		else if (currentPanel==sequencesPanel)
			setCurrentPanel(dataSourcePanel);
		else if (currentPanel==dataSourcePanel)
			setCurrentPanel(projectPropertiesPanel);
		else if (currentPanel==projectPropertiesPanel)
			setCurrentPanel(speciesPanel);
	}

	private void setCurrentPanel(NewProjectWizardPanel panel) {
		if (currentPanel!=null)
			this.remove(currentPanel);
		currentPanel = panel;

		backAction.setEnabled(currentPanel!=speciesPanel);

		if (currentPanel==dataSourcePanel) {
			nextAction.setEnabled(ProjectDescription.LOCAL_DATA_LABEL.equals(dataSourcePanel.getDataSource()));
		}
		else {
			nextAction.setEnabled(currentPanel!=loadGenomePanel);
		}

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(8,8,12,8);
		this.add(currentPanel, c);

		currentPanel.init();

		validate();
		repaint();
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

	/**
	 * @return true if it's OK to proceed, false if an error occurred.
	 */
	private boolean updateProjectDescription() {
		try {
			// organism -> project name, filename. data source
			if (currentPanel==speciesPanel) {
				String species = speciesPanel.getSpecies();
				if (!species.equals(projectDescription.getOrganism())) {
	
					// if user hasn't modified these settings then overwrite them
					projectDescription.resetDefaults();
	
					// update project description
					projectDescription.setOrganism(species);
	
					// apply default setting from organism name
					app.getProjectDefaults().apply(projectDescription);
				}
			}
			else if (currentPanel==projectPropertiesPanel) {
				projectDescription.setProjectName(projectPropertiesPanel.getProjectName());
				projectDescription.setFile(projectPropertiesPanel.getFilename());
			}
			else if (currentPanel==dataSourcePanel) {
				projectDescription.setDataSource(dataSourcePanel.getDataSource());
				projectDescription.setRemoveUnassembledFragments(dataSourcePanel.getRemoveUnassembledFragments());
			}
			else if (currentPanel==sequencesPanel) {
				projectDescription.setSequences(sequencesPanel.getSequences());
			}
			else if (currentPanel==loadGenomePanel) {
				projectDescription.setGenomeFile(loadGenomePanel.getFile());
			}
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	public boolean validateInput() {
		// organism can't be blank
		if (isNullOrEmpty(projectDescription.getOrganism())) {
			showErrorMessage("Specify an organism to continue.", "Organism required");
			setCurrentPanel(speciesPanel);
			return false;
		}

		// project name can't be blank
		if (isNullOrEmpty(projectDescription.getProjectName())) {
			showErrorMessage("Specify a name for your project.", "Project name required");
			setCurrentPanel(projectPropertiesPanel);
			return false;
		}

		// filename has to point to a writable file
		if (projectDescription.getFile()==null) {
			showErrorMessage("Specify a file for your project. The genome browser will store use " +
					"this file to locally store a project description, sequences that make up the " +
					"genome, and your data tracks.", "Filename required");
			setCurrentPanel(projectPropertiesPanel);
			return false;
		}

		// if (data-source == my-own-data) sequences are required
		if (ProjectDescription.LOCAL_DATA_LABEL.equals(projectDescription.getDataSource())) {
			if (projectDescription.getSequenceDescriptions().size() < 1) {
				if (projectDescription.defaultDataSource()) {
					showHtmlErrorMessage("<html><body><p>At least one sequence definition (name and " +
							"length of chromosomes and plasmids, for example) is required to create a project. For many organisms, " +
							"this data can be downloaded automatically, but in this case, " +
							"<b>\"" + projectDescription.getOrganism() + 
							"\"</b>, isn't a recognized organism and can't be downloaded automatically.</p>" +
							"<p>Sequence information can be manually specified in the Sequences panel, or you may want to verify " +
							"the organism name you're using. You will be taken to the Sequences panel now.</p>" +
							"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/new\">More help on creating new projects</a></p>" +
							"</body></html>",
							"Sequence information required");
				}
				else {
					showHtmlErrorMessage("<html><body><p>At least one sequence definition (name and " +
							"length of sequence) is required to create a project. You selected " +
							"\"My own data\" as your data source, so you need to supply a list of " +
							"sequence names and lengths.</p>" +
							"<p>The sequences you're working with are " +
							"usually the chromosomes and plasmids of the organism you're working " +
							"with, but may be any related set of sequences.</p>" +
							"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/new/\">More help on creating new projects</a></p>" +
							"</body></html>",
							"Sequence information required");
				}
				setCurrentPanel(sequencesPanel);
				return false;
			}
		}

		return true;
	}


	public void ok() {
		try {
			if (!updateProjectDescription()) return;
			if (!validateInput()) return; 
			close();
			for (DialogListener listener: listeners) {
				listener.ok("ok", projectDescription);
			}
		}
		catch (Exception e) {
			showErrorMessage(e.getMessage(), "Not a valid project");
		}
	}

	private void showErrorMessage(String message, String title) {
		String html = "<html><body><p>" + message + "</p></body></html>";
		Dialogs.showHtmlMessageDialog(this, html, title);
	}

	private void showHtmlErrorMessage(String html, String title) {
		Dialogs.showHtmlMessageDialog(this, html, title);
	}

	public void addDialogListener(DialogListener listener) {
		listeners.add(listener);
	}

	public void removeDialogListener(DialogListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Entry point for testing
	 */
	public static void main(String[] args) {
		final JFrame frame = new JFrame("test");
		frame.pack();
		frame.setVisible(true);

		Options options = new Options();
		options.dataDirectory = new File(System.getProperty("user.home"));
		Application app = new Application(options);

		NewProjectWizard dialog = new NewProjectWizard(frame, app);
		dialog.addDialogListener(new DialogListener() {
			public void ok(String action, Object result) {
				System.out.println("action=" + action);
				System.out.println("results=" + result);
				System.exit(0);
			}
			public void cancel() {
				System.out.println("cancel");
				System.exit(0);
			}
			public void error(String message, Exception e) {
				System.out.println("error: " + message);
				e.printStackTrace();
				System.exit(-1);
			}
		});
		dialog.setVisible(true);		
	}
}



abstract class NewProjectWizardPanel extends JPanel {
	/**
	 * called when panel becomes visible
	 */
	public abstract void init();
}


class SpeciesPanel extends NewProjectWizardPanel implements WindowFocusListener {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h2>Select organism species</h2>" +
	"<p>A genome browser project usually starts with the genome of an organism. This defines the " +
	"sequences (chromosomes, plasmids, etc.) you'll be working with and the locations of <b>genes</b> " +
	"and other important features.</p>" +
	"<p><b>Select an " +
	"organism</b> from the list below by typing it's name and pressing <i>enter</i> or <i>double-clicking</i> " +
	"in the list. " +
	"For most common organisms, the program can fetch all necessary data from public sources. Otherwise, " +
	"it might be necessary to supply some addition data.</p>" +
	"<p>For simplest operation, <b>select species and press OK</b>.</p>" +
	"</body></html>";

	private JTextField organismTextField;
	private JList organismList;
	private FilterList<Genome> filteredGenomes;
	private Genome selectedGenome;
	private EventList<Genome> genomes;
	private EventSupport eventSupport = new EventSupport();

	public SpeciesPanel() {
		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);

		JLabel organismLabel = new JLabel("Organism:");
		organismTextField = new JTextField();

		organismTextField.getActionMap().put("focus-to-list", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				organismList.requestFocusInWindow();
			}
		});
		InputMap im = organismTextField.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "focus-to-list");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0, false), "focus-to-list");

		organismTextField.getActionMap().put("species-OK", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				eventSupport.fireEvent(SpeciesPanel.this, "species-OK");
			}
		});
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "species-OK");
		organismList = new JList();
		organismList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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
		c.insets = new Insets(8,8,2,8);
		this.add(instructions, c);

		c.gridx = 0;
		c.gridy++;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(8,8,2,2);
		this.add(organismLabel, c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(8,2,2,8);
		this.add(organismTextField, c);

		c.gridx = 1;
		c.gridy++;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(2,5,12,11);
		this.add(new JScrollPane(organismList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), c);		

		loadGenomes();
	}

	public String getSpecies() {
		return organismTextField.getText().trim();
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

	public void init() {
		organismTextField.requestFocusInWindow();
	}

	public void windowGainedFocus(WindowEvent e) {
		organismTextField.requestFocusInWindow();
	}

	public void windowLostFocus(WindowEvent e) {}

	public void addEventListener(EventListener listener) {
		eventSupport.addEventListener(listener);
	}

	public void removeEventListener(EventListener listener) {
		eventSupport.removeEventListener(listener);
	}
}



/**
 * Set the name of the dataset and its location in the file system.
 */
class ProjectPropertiesPanel extends NewProjectWizardPanel {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h2>Project properties</h2>" +
	"<p>Genome browser <b>projects</b> are stored in local files (a " +
	"<a href=\"http://www.sqlite.org/\">Sqlite</a> database). By default, the file is stored " +
	"in /hbgb (for HeebieGB, the nickname of the genome browser) under the user's document " +
	"directory (depending on platform) with a name generated " +
	"from the species. Alternatively, you can specify a <b>path</b> and <b>filename</b> here.</p>" +
	"<p>Separate from the filename, a dataset has a name, which can be specified here as well. This " +
	"name is meant to be a user-friendly name for display purposes.</p>" +
	"</body></html>";
	private JTextField filenameTextField;
	private JTextField nameTextField;
	private ProjectDescription projectDescription;
	private File directory;

	
	public ProjectPropertiesPanel(Application app, ProjectDescription desc) {
		this.projectDescription = desc;

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);

		AbstractAction browseAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				browse();
			}
		};

		filenameTextField = new JTextField();
		filenameTextField.getActionMap().put("enter-key-handler", browseAction);
		InputMap im = filenameTextField.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "enter-key-handler");

		JButton browseButton = new JButton("Browse");
		browseButton.addActionListener(browseAction);

		directory = (app==null) ? new File(System.getProperty("user.home")) : app.options.dataDirectory;
		JLabel workingDirLabel = new JLabel(String.format("(data dir: %s)", directory.getAbsolutePath()));
		workingDirLabel.setFont(workingDirLabel.getFont().deriveFont(9.0f));

		nameTextField = new JTextField();
		if (!isNullOrEmpty(projectDescription.getProjectName())) {
			nameTextField.setText(projectDescription.getProjectName());
		}
		else if (!isNullOrEmpty(projectDescription.getOrganism())) {
			nameTextField.setText(projectDescription.getOrganism());
		}


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
		c.insets = new Insets(8,8,2,8);
		this.add(instructions, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,2,2);
		this.add(new JLabel("Filename:"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(12,2,2,2);
		this.add(filenameTextField, c);

		c.gridx = 2;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,2,2,2);
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
	}

	public void init() {
		if (projectDescription.getProjectName()!=null)
			nameTextField.setText(projectDescription.getProjectName());
		if (projectDescription.getFile()!=null)
			filenameTextField.setText(projectDescription.getFile().getAbsolutePath());
		filenameTextField.requestFocusInWindow();
//		else if (!isNullOrEmpty(projectDescription.getOrganism())) {
//			if (app != null) {
//				File file = app.getProjectDefaults().getDefaultFile(projectDescription.getOrganism());
//				filenameTextField.setText(file.getAbsolutePath());
//			}
//			else {
//				File file = new File(System.getProperty("user.home"), FileUtils.toValidFilename(projectDescription.getOrganism())+".hbgb");
//				filenameTextField.setText(file.getAbsolutePath());
//			}
//		}
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
				file = new File(directory, path);
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
			directory = chooser.getSelectedFile().getParentFile();
			String f = chooser.getSelectedFile().getAbsolutePath();
			if ("".equals(FileUtils.extension(f)))
				f += ".hbgb";
			filenameTextField.setText(f);
		}
	}

	public String getFilename() {
		return filenameTextField.getText().trim();
	}

	public String getProjectName() {
		return nameTextField.getText().trim();
	}
}



/**
 * Panel for selecting data source. Fires events when data source is changed.
 */
class DataSourcePanel extends NewProjectWizardPanel {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h2>Data source</h2>" +
	"<p>Genome browser <b>projects</b> require a set of sequences (chromosomes, plasmids, or " +
	"other sequences) and usually start with the genes of the organism involved. For common model " +
	"organisms we can automatically acquire the needed data from the " +
	"<a href=\"http://genome.ucsc.edu/\">UCSC genome browser</a> or from NCBI's " +
	"<a href=\"http://www.ncbi.nlm.nih.gov/sites/entrez?db=genome\">Entrez Genome</a> database. Occasionally, manually loading this data will be necessary.</p>" +
	"<p>Please <b>select the data source</b> you would like to use below.</p>" +
	"</body></html>";
	private JComboBox dataSourceChooser;
	private EventSupport eventSupport = new EventSupport();
	private ProjectDescription projectDescription;
	private JCheckBox removeUnassembledFragmentsCheckBox;

	public DataSourcePanel(ProjectDescription desc) {
		this.projectDescription=desc;
		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);

		dataSourceChooser = new JComboBox(new String[] {"NCBI", "UCSC", ProjectDescription.LOCAL_DATA_LABEL});
		dataSourceChooser.setSelectedIndex(1);
		dataSourceChooser.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange()==ItemEvent.SELECTED) {
					if ("NCBI".equals(e.getItem())) {
						JOptionPane.showMessageDialog(DataSourcePanel.this, String.valueOf(e.getItem()) + " not implemented yet!", "Doh!", JOptionPane.WARNING_MESSAGE);
					}
					eventSupport.fireEvent(DataSourcePanel.this, "select-datasource", (String)e.getItem());
				}
			}
		});
		
		removeUnassembledFragmentsCheckBox = new JCheckBox("Remove unassembled fragments");
		removeUnassembledFragmentsCheckBox.setSelected(true);

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
		c.insets = new Insets(8,8,2,8);
		this.add(instructions, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,2,2);
		this.add(removeUnassembledFragmentsCheckBox, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,2,2);
		this.add(new JLabel("Data Source:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,2,2,2);
		this.add(dataSourceChooser, c);
	}

	public String getDataSource() {
		return (String)dataSourceChooser.getSelectedItem();
	}

	public boolean getRemoveUnassembledFragments() {
		return removeUnassembledFragmentsCheckBox.isSelected();
	}

	public void addEventListener(EventListener listener) {
		eventSupport.addEventListener(listener);
	}

	public void removeEventListener(EventListener listener) {
		eventSupport.removeEventListener(listener);
	}

	public void init() {
		if (projectDescription.getDataSource()!=null)
			dataSourceChooser.setSelectedItem(projectDescription.getDataSource());
		dataSourceChooser.requestFocusInWindow();
	}
}



class SequencesPanel extends NewProjectWizardPanel {
	private static final Logger log = Logger.getLogger(SequencesPanel.class);
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h2>Edit Sequences</h2>" +
	"<p>The Genome Browser plots data against coordinates on the genome or some other sequence. You can " +
	"manually define the <a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/sequence_descriptions/\"> " +
	"sequences</a> to be plotted against. Typically, the sequences are the chromosomes, plasmids, or " +
	"other genomic sequences that make up the genome.</p>" +
	"<p>Paste or type a list of sequences and sizes in base-pairs, separated by a semicolon (or tab or colon) - one" +
	"sequence per line. Optionally, include the topology of the sequence as a third field.</p>" +
	"<h3>Examples:</h3>" +
	"<pre>" +
	"chromosome; 2,014,239; circular\n" +
	"pNRC200; 365,425; circular\n" +
	"pNRC100; 191346; circular" +
	"</pre></body></html>";
	private JTextArea sequencesTextArea;
	private ButtonGroup topologyButtonGroup;
	@SuppressWarnings("unused")
	private Application app;


	public SequencesPanel(Application app) {
		this.app = app;

		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);

		sequencesTextArea = new JTextArea();

		JRadioButton topoCircular = new JRadioButton("circular", true);
		topoCircular.setActionCommand("circular");
		JRadioButton topoLinear = new JRadioButton("linear");
		topoLinear.setActionCommand("linear");
		JRadioButton topoUnknown = new JRadioButton("unknown");
		topoUnknown.setActionCommand("unknown");

		topologyButtonGroup = new ButtonGroup();
		topologyButtonGroup.add(topoLinear);
		topologyButtonGroup.add(topoCircular);
		topologyButtonGroup.add(topoUnknown);

		Box topologyBox = Box.createHorizontalBox();
		topologyBox.add(topoLinear);
		topologyBox.add(topoCircular);
		topologyBox.add(topoUnknown);

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
		c.insets = new Insets(8,12,12,8);
		this.add(instructions, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,2,2);
		this.add(new JLabel("Topology:"), c);

		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,2,2);
		this.add(topologyBox, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(4,12,2,2);
		this.add(new JLabel("Sequence Descriptions:"), c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(2,12,4,12);
		this.add(new JScrollPane(sequencesTextArea), c);
	}


	public List<SequenceDescription> getSequences() throws Exception {
		String line = null;
		try {
			String text = sequencesTextArea.getText();
			List<SequenceDescription> results = new ArrayList<SequenceDescription>();
			Topology topology = Topology.valueOf(topologyButtonGroup.getSelection().getActionCommand());
			for (String nextLine : text.split("\n")) {
				line = nextLine.trim();
				if (line.length()>0) {
					String[] fields = line.split("\\s*[\\t;:]\\s*");
					if (fields.length == 2) {
						int len = Integer.parseInt(fields[1].replace(",", ""));
						results.add(new SequenceDescription(fields[0], len, topology));
					}
					else if (fields.length == 3) {
						int len = Integer.parseInt(fields[1].replace(",", ""));
						results.add(new SequenceDescription(fields[0], len, Topology.valueOf(fields[2])));
					}
					else {
						throw new RuntimeException("Can't parse: \"" + line + "\".");
					}
				}
			}
			return results;
		}
		catch (Exception e) {
			log.warn(e);
			if (line==null) {
				Dialogs.showMessageDialog(this, e.getMessage(), "Error");
			}
			else if (e.getMessage().contains("No enum const class")) {
				Dialogs.showHtmlMessageDialog(this, "<html><body>" +
						"<p>Please check your sequence descriptions. Unrecognized topology type in line:</p>" +
						"<blockquote>" + line + "</blockquote>" +
						"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/sequence_descriptions/\">More help on entering sequence descriptions.</a></p>" + 
						"</body></html>", "Error");
			}
			else {
				Dialogs.showHtmlMessageDialog(this, "<html><body>" +
						"<p>Please check your sequence descriptions. An error occurred while trying to parse the line:</p>" +
						"<blockquote>" + line + "</blockquote>" +
						"<p><a href=\"http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/sequence_descriptions/\">More help on entering sequence descriptions.</a></p>" + 
						"</body></html>", "Error");
			}
			throw e;
		}
	}

	public void init() {
		sequencesTextArea.requestFocusInWindow();
	}
}



class LoadGenomePanel extends NewProjectWizardPanel {
	private static String INSTRUCTIONS_HTML = "<html><body>" +
	"<h2>Load genome features (OPTIONAL)</h2>" +
	"<p>You may want to import the locations of genes - primarily protein coding regions but also tRNAs, ribosomal " +
	"RNAs, ncRNAs etc. To do that, you'll need a <b>tab-delimited text file</b> with the following columns:</p>" +
	"<p><b>(Sequence, Strand, Start, End, Unique Name, Common Name, Gene Type)</b></p>" +
	"<p>Sequences must match sequences descriptions from the previous tab. Strand should be either <i>+</i>, " +
	"<i>-</i> or <i>.</i> (for no strand). Gene Type indicates coding sequence or other types of features, for example: " +
	"<i>cds</i>, <i>rna</i>, <i>trna</i> or <i>rrna</i></p>" +
	"<p>This type of file can be easily created with Excel, a script, or any text editor. Loading features is " +
	"<b>optional</b> and can be done later using the <i>import track</i> feature.</p>" +
	"</body></html>";
	private JTextField filenameTextField;
	private File directory;

	public LoadGenomePanel(Application app) {
		JEditorPane instructions = SwingGadgets.createHtmlTextPane(this, INSTRUCTIONS_HTML, SwingGadgets.getStyleSheet());
		instructions.setOpaque(false);

		AbstractAction browseAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				browse();
			}
		};

		filenameTextField = new JTextField();
		filenameTextField.getActionMap().put("enter-key-handler", browseAction);
		InputMap im = filenameTextField.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "enter-key-handler");

		JButton browseButton = new JButton("Browse");
		browseButton.addActionListener(browseAction);

		directory = (app==null) ? new File(System.getProperty("user.home")) : app.options.dataDirectory;
		JLabel workingDirLabel = new JLabel(String.format("(data dir: %s)", directory.getAbsolutePath()));
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
		c.insets = new Insets(8,12,2,8);
		this.add(instructions, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,2,2);
		this.add(new JLabel("Filename:"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(12,2,2,2);
		this.add(filenameTextField, c);

		c.gridx = 2;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12,12,2,2);
		this.add(browseButton, c);


		c.gridx = 1;
		c.gridy++;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(4,12,2,8);
		this.add(workingDirLabel, c);
	}

	public void browse() {
		String path = filenameTextField.getText();

		File file = null;
		if (isNullOrEmpty(path)) {
			file = directory;
		}
		else {
			file = new File(path);
			if (!file.isAbsolute()) {
				file = new File(directory, path);
			}
		}

		JFileChooser chooser = new JFileChooser(file);
		chooser.setDialogTitle("Load Genome File");

		int returnVal = chooser.showOpenDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			filenameTextField.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	public File getFile() {
		if (isNullOrEmpty(filenameTextField.getText().trim())) return null;
		return new File(filenameTextField.getText().trim());
	}

	public void init() {
		filenameTextField.requestFocusInWindow();
	}
}

