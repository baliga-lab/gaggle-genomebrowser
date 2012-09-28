package org.systemsbiology.util.swing;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;

import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.StringUtils;


/**
 * Stock dialogs that are a little less defective that the ones that come with Swing
 */
public class Dialogs {
	
	public static void showMessageDialog(Component parentComponent, String message, String title) {
		showHtmlMessageDialog(parentComponent, toHtml(message), title);
	}

	public static void showMessageDialog(Component parentComponent, String message, String title, Icon icon) {
		showHtmlMessageDialog(parentComponent, toHtml(message), title, icon);
	}

	public static void showHtmlMessageDialog(Component parentComponent, String htmlMessage, String title) {
		showHtmlMessageDialog(parentComponent, htmlMessage, title, FileUtils.getIconOrBlank("warning_icon.png"));
	}

	public static void showHtmlMessageDialog(Component parentComponent, String htmlMessage, String title, Icon icon) {
		final JDialog dialog = createDialog(parentComponent, title);

		AbstractAction dismissAction = new AbstractAction("OK") {
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
				dialog.dispose();
			}
		};

		// map escape and command-w to cancel the dialog
		dialog.getRootPane().getActionMap().put("close-window-on-escape", dismissAction);
		InputMap im = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "close-window-on-escape");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), false), "close-window-on-escape");

		JEditorPane editorPane = SwingGadgets.createHtmlTextPane(dialog, htmlMessage, SwingGadgets.getStyleSheet(), SwingGadgets.COMPUTE_FROM_SCREEN);
		editorPane.setOpaque(false);

		JLabel iconLabel = new JLabel(icon);
		JButton okButton = new JButton(dismissAction);

		dialog.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.VERTICAL;
		c.insets = new Insets(4,12,4,12);
		dialog.add(iconLabel, c);

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(4,12,4,12);
		JScrollPane scrollPane = new JScrollPane(editorPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		dialog.add(scrollPane, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(4,12,4,12);
		dialog.add(okButton, c);

		dialog.pack();
//		if (dialog.getWidth()>maxWidth) {
//			dialog.setSize(new Dimension(maxWidth, d.height/3));
//		}
		dialog.setLocationRelativeTo(parentComponent);
		okButton.requestFocusInWindow();
		dialog.setVisible(true);
	}
	
	private static JDialog createDialog(Component parentComponent, String title) {
		Window window = getWindowForComponent(parentComponent);
		if (window instanceof Frame) {
			return new JDialog((Frame)window, title, true);	
		}
		else {
			return new JDialog((Dialog)window, title, true);
		}
	}

	private static String toHtml(String message) {
		StringBuilder sb = new StringBuilder("<html><body><p>");
		message = StringUtils.htmlEscape(message); 
		message = message.replace("\n", "</p><p>");
		sb.append(message);
		sb.append("</p></body></html>");
		return sb.toString();
	}

	private static Window getWindowForComponent(Component parentComponent) throws HeadlessException {
		if (parentComponent == null) {
			return new JFrame("Dummy Frame for testing only");
		}
		if (parentComponent instanceof Frame || parentComponent instanceof Dialog)
			return (Window)parentComponent;
		return getWindowForComponent(parentComponent.getParent());
	}

	public static void main(String[] args) {
		String html = "<html><body><h1>Hey you!</h1>" +
				"<p>This is a paragraph about <b>snugglebunnies</b>. Blither blather bonk. " +
				"Blah blah blabber boo wacka dacka doo, gibber. <i>Bungle bork</i> slop splat thud." +
				"Wiggle jiggle pickle goo, flapdoodle pliffer wobble.</p>" +
				"<p>This is another paragraph. It's about " +
				"<span style=\"font-weight:bold; color:#996699;\">snugglebunnies</span>, too. " +
				"But, it doesn't get into the good stuff.</p></body></html>";
		Dialogs.showHtmlMessageDialog(null, html, "Snugglebunnies");
	}
}
