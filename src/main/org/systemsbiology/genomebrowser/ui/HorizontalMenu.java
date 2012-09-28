package org.systemsbiology.genomebrowser.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Stolen from the MenuLayoutDemoProject example in the Sun
 * swing tutorial.
 * 
 * @author cbare
 */
public class HorizontalMenu extends JMenu {

	public HorizontalMenu() {
		super();
		JPopupMenu pm = getPopupMenu();
		pm.setLayout(new BoxLayout(pm, BoxLayout.LINE_AXIS));
	}

	public HorizontalMenu(String label) {
		super(label);
		JPopupMenu pm = getPopupMenu();
		pm.setLayout(new BoxLayout(pm, BoxLayout.LINE_AXIS));
	}

	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

//	public void setPopupMenuVisible(boolean b) {
//		boolean isVisible = isPopupMenuVisible();
//		if (b != isVisible) {
//			if ((b==true) && isShowing()) {
//				//Set location of popupMenu (pulldown or pullright).
//				//Perhaps this should be dictated by L&F.
//				int x = 0;
//				int y = 0;
//				Container parent = getParent();
//				if (parent instanceof JPopupMenu) {
//					x = 0;
//					y = getHeight();
//				} else {
//					x = getWidth();
//					y = 0;
//				}
//				getPopupMenu().show(this, x, y);
//			} else {
//				getPopupMenu().setVisible(false);
//			}
//		}
//	}
}
