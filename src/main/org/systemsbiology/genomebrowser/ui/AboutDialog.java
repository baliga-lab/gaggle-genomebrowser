package org.systemsbiology.genomebrowser.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;


/**
 * About box. Focus of major development effort.
 *
 * @author cbare
 */
@SuppressWarnings("serial")
public class AboutDialog extends JDialog {
	private UI ui;

	public AboutDialog(Frame owner, boolean modal, UI ui) throws HeadlessException {
		super(owner, "About the Genome Browser", modal);

		this.ui = ui;

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.add(new JButton(ui.actions.logMemoryUsageAction));
		toolbar.add(new JButton(ui.actions.logFeatureCountAction));

		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.NORTH);
		add(createGui(), BorderLayout.CENTER);
		pack();
	}

	private Component createGui() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(10, 10, 10, 10);

		JLabel title = new JLabel("Genome Browser");
		title.setFont(new Font("Arial", Font.BOLD, 24));

		c.gridy++;
		panel.add(title, c);

		c.gridy++;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(10, 20, 2, 5);
		c.gridwidth = 1;
		c.gridx=0;
		panel.add(new JLabel("Software Engineer: "), c);
		c.gridx=1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(10, 5, 2, 20);
		panel.add(new JLabel("J. Christopher Bare"), c);

		c.gridy++;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(2, 20, 2, 5);
		c.gridwidth = 1;
		c.gridx=0;
		panel.add(new JLabel("Principle Investigator: "), c);
		c.gridx=1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 5, 2, 20);
		panel.add(new JLabel("Nitin S. Baliga"), c);

		c.gridy++;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(10, 20, 2, 5);
		c.gridwidth = 1;
		c.gridx=0;
		panel.add(new JLabel("Version: "), c);
		c.gridx=1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(10, 5, 2, 20);
		panel.add(new JLabel(ui.getOptions().version), c);

		c.gridy++;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(10, 20, 2, 5);
		c.gridwidth = 1;
		c.gridx=0;
		panel.add(new JLabel("Build Number: "), c);
		c.gridx=1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(10, 5, 2, 20);
		panel.add(new JLabel(ui.getOptions().buildNumber), c);

		c.gridy++;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(2, 20, 10, 5);
		c.gridx=0;
		panel.add(new JLabel("Build Date: "), c);
		c.gridx=1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 5, 10, 20);
		panel.add(new JLabel(ui.getOptions().buildDate), c);

		JLabel url = new JLabel("http://gaggle.systemsbiology.net/docs/geese/genomebrowser/");
		url.setFont(new Font("Arial", Font.ITALIC, 10));
		url.setForeground(Color.BLUE);
		url.addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				ui.showHelp();
			}

			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}

		});
		url.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		c.gridy++;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(10, 10, 10, 10);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridx=0;
		panel.add(url, c);

		JLabel isb;
		try {
			isb = new JLabel(new ImageIcon(getClass().getResource("/icons/isblogo.gif")));
		}
		catch (Exception e) {
			isb = new JLabel("Institute for Systems Biology");
		}
		isb.addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				ui.showIsbWebsite();
			}

			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}

		});
		isb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		c.gridy++;
		panel.add(isb, c);

		c.gridy++;
		panel.add(sysInfoPanel(), c);

		Box buttonBox = Box.createHorizontalBox();
		panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

		buttonBox.add(new JButton(new OkAction()));

		c.gridy++;
		panel.add(buttonBox, c);

		return panel;
	}

	JComponent sysInfoPanel() {
		JTextArea textArea = new JTextArea();
		textArea.setFont(new Font("Arial", Font.ITALIC, 10));
		textArea.setBackground(new Color(0xEEEEEE));
		textArea.setEditable(false);

		textArea.setText(
				"java.version:\t" + System.getProperty("java.version") + "\n" +
				"java.vendor:\t" + System.getProperty("java.vendor") + "\n" +
				"java.home:\t" + System.getProperty("java.home") + "\n" +
				"os.name:\t" + System.getProperty("os.name") + "\n" +
				"os.arch:\t" + System.getProperty("os.arch") + "\n" +
				"os.version\t" + System.getProperty("os.version") + "\n" +
				"user.name:\t" + System.getProperty("user.name") + "\n" +
				"user.home:\t" + System.getProperty("user.home") + "\n" +
				"user.dir:\t" + System.getProperty("user.dir") + "\n" +
				"db.version:\t" + System.getProperty("db.version") + "\n" +
				"db.driver:\t" + System.getProperty("db.driver"));

		return new JScrollPane(textArea);
	}

//	Map<String, String> getSqliteInfo() {
//		try {
//			return SqliteDataSource.getDatabaseInfo();
//		}
//		catch (Throwable e) {
//			Map<String, String> errors = new HashMap<String, String>();
//			errors.put("database.info.error", e.getClass().getName() + " " + e.toString());
//			return errors;
//		}
//	}


	class OkAction extends AbstractAction {
		public OkAction() {
			super("OK");
		}
		public void actionPerformed(ActionEvent e) {
			AboutDialog.this.setVisible(false);
		}
	}

}
