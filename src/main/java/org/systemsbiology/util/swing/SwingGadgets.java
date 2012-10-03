package org.systemsbiology.util.swing;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JEditorPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.DTDConstants;

import org.apache.log4j.Logger;

public class SwingGadgets {
	public static final int COMPUTE_FROM_SCREEN = -1001;
	private static final Logger log = Logger.getLogger(SwingGadgets.class);
	private static StyleSheet styleSheet;


	// stolen from http://software.jessies.org/svn/salma-hayek/trunk/src/e/gui/HtmlPane.java
    static {
        try {
            // Ensure Swing has set up its HTML 3.2 DTD...
        	class ParserGetter extends HTMLEditorKit {
      		  // purely to make this method public
      		  public HTMLEditorKit.Parser getParser() {
      		    return super.getParser();
      		  }
      		} 
        	new ParserGetter().getParser();

            // ...so we can add a couple of HTML 4 character entity references (Sun bug 6632959).
            DTD html32 = DTD.getDTD("html32");
            html32.defEntity("ndash", DTDConstants.CDATA | DTDConstants.GENERAL, '\u2013');
            html32.defEntity("mdash", DTDConstants.CDATA | DTDConstants.GENERAL, '\u2014');
            html32.defEntity("lsquo", DTDConstants.CDATA | DTDConstants.GENERAL, '\u2018');
            html32.defEntity("rsquo", DTDConstants.CDATA | DTDConstants.GENERAL, '\u2019');
            html32.defEntity("ldquo", DTDConstants.CDATA | DTDConstants.GENERAL, '\u201c');
            html32.defEntity("rdquo", DTDConstants.CDATA | DTDConstants.GENERAL, '\u201d');
            html32.defEntity("lsaquo", DTDConstants.CDATA | DTDConstants.GENERAL, '\u2039');
            html32.defEntity("rsaquo", DTDConstants.CDATA | DTDConstants.GENERAL, '\u203a');
            html32.defEntity("trade", DTDConstants.CDATA | DTDConstants.GENERAL, '\u2122');
        } catch (Exception ex) {
            log.warn(ex);
        }
    }

	public static JTextPane createHtmlTextPane(Component parent, String html, StyleSheet styleSheet) {
		return createHtmlTextPane(parent, html, styleSheet, -1);
	}

	/**
     * Create a non-editable JTextPane initialized with the given HTML and stylesheet. 
     * @param parent
     * @param html
     * @param styleSheet
     * @param preferredWidth tell the pane to wrap its contents to the specified width
     */
	public static JTextPane createHtmlTextPane(Component parent, String html, StyleSheet styleSheet, int preferredWidth) {

		JTextPane textPane = new JTextPane() {
			@Override
			public boolean getScrollableTracksViewportWidth() {
				return true;
			}
		};
		
		HTMLEditorKit kit = (HTMLEditorKit)JEditorPane.createEditorKitForContentType("text/html");
		if (styleSheet != null)
			kit.setStyleSheet(styleSheet);
		textPane.setEditorKit(kit);
		textPane.setEditable(false);
		textPane.setBackground(parent.getBackground());


		textPane.setText(html);

		// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4765285
		if (preferredWidth>0) {
			textPane.setSize(new Dimension(preferredWidth,Short.MAX_VALUE));
			Dimension p = textPane.getPreferredSize();
			textPane.setPreferredSize(new Dimension(preferredWidth,p.height));
		}
		else if (preferredWidth==COMPUTE_FROM_SCREEN) {
			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			int w = d.width/4;
			textPane.setSize(new Dimension(w,Short.MAX_VALUE));
			Dimension p = textPane.getPreferredSize();
			int h = Math.min(p.height, d.height/2);
			textPane.setPreferredSize(new Dimension(w,h));
		}

		textPane.addHyperlinkListener(new HyperlinkListener() {
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
		return textPane;
	}


	public static StyleSheet getStyleSheet() {
		if (styleSheet == null) {
			styleSheet = new StyleSheet();
			styleSheet.addRule("body p h1 h2 h3 { font-family: helvetica, arial, sans-serif };");
			styleSheet.addRule("h1 { font-size: 120%; font-weight: bold; margin-bottom: 12px; };");
			styleSheet.addRule("h2 { font-size: 110%; font-weight: bold; };");
			styleSheet.addRule("h3 { font-size: 100%; font-weight: bold; };");
			styleSheet.addRule("a { color: #336699; text-decoration: underline;};");
			styleSheet.addRule("p { color: #111111; margin: 4px 0px 4px 0px};");
			styleSheet.addRule("li { margin: 4px 8px 4px 8px; text-indent: -8px; };");
			styleSheet.addRule("blockquote { margin: 6px 8px 6px 8px; color: #aaaaaa; };");
		}
		return styleSheet;
	}

}
