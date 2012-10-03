package org.systemsbiology.genomebrowser.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.log4j.Logger;
import org.systemsbiology.util.FileUtils;


public class ExtendedErrorDialog extends JDialog {
	private static final Logger log = Logger.getLogger(ExtendedErrorDialog.class);
	private JScrollPane exceptionScrollPane;
	private JButton detailButton;

	public ExtendedErrorDialog(Frame owner, String title, String html, Exception e) throws HeadlessException {
		super(owner, "Error");
		init(title, html, e);
	}

	private void init(String title, String html, Exception e) {
		JLabel titleLabel = new JLabel(title, FileUtils.getIconOrBlank("error_icon.png"), SwingConstants.LEADING);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

		Box box = new Box(BoxLayout.Y_AXIS);

		JEditorPane editorPane = new JEditorPane();
		editorPane.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
		editorPane.setEditable(false);
		editorPane.setBackground(getBackground());
		editorPane.setText(html);
		editorPane.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent event) {
				if (event.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
					try {
              Desktop.getDesktop().browse(new java.net.URI(event.getURL().toString()));
					} catch (Exception e) {
						log.error("Failed to open browser: " + event, e);
					}
				}
			}
		});

		JScrollPane editorScrollPane = new JScrollPane(editorPane);
		editorScrollPane.setPreferredSize(new Dimension(250, 145));
		editorScrollPane.setMinimumSize(new Dimension(10, 10));
		box.add(editorScrollPane);

		StringWriter sw = new StringWriter();
		if (e!=null)
			e.printStackTrace(new PrintWriter(sw));
		else
			sw.append("no exception");
		JTextArea textArea = new JTextArea(sw.toString());
		textArea.setEditable(false);
		textArea.setBackground(getBackground());
		textArea.setFont(new Font("Arial", Font.PLAIN, 10));
		exceptionScrollPane = new JScrollPane(textArea);
		exceptionScrollPane.setPreferredSize(new Dimension(250, 200));
		exceptionScrollPane.setVisible(false);
		box.add(exceptionScrollPane);

		detailButton = new JButton("Show detail");
		detailButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (exceptionScrollPane.isVisible()) {
					exceptionScrollPane.setVisible(false);
					detailButton.setText("Show detail");
				}
				else {
					exceptionScrollPane.setVisible(true);
					detailButton.setText("Hide detail");
				}
				pack();
			}
		});

		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(12,12,6,12));
		buttonPanel.add(detailButton);
		buttonPanel.add(ok);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
		panel.add(titleLabel, BorderLayout.NORTH);
		panel.add(box, BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);

		add(panel);

		pack();

		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation((d.width - this.getWidth()) / 2, (d.height - this.getHeight()) / 2 );
	}

}
