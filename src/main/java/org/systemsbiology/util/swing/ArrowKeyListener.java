package org.systemsbiology.util.swing;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextField;


/**
 * Try to minimize some of the boilerplate for listening to arrow keys.
 * Subclasses should override methods up, down, and optionally enter with custom behavior.
 * Register listener like so:
 * <code>
 * myTextField.addKeyListener(new ArrowKeyListener() {
 *   public void enter() {
 *     // do something
 *   }
 *   public void up(boolean shift) {
 *     if (shift)
 *       // go up big
 *     else
 *       // go up
 *   }
 *   public void down(boolean shift) {
 *     if (shift)
 *       // go down big
 *     else
 *       // go down
 *   }
 * });
 * </code>
 * Typically, this is used to enable a JTextField to increment or decrement a numeric value
 * in response to the arrow keys or shift arrow keys.
 */
public abstract class ArrowKeyListener implements KeyListener {

	public void keyPressed(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.VK_ENTER) {
			enter();
		}
		else if (event.getKeyCode() == KeyEvent.VK_DOWN) {
			down(((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) > 0));
		}
		else if (event.getKeyCode() == KeyEvent.VK_UP) {
			up(((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) > 0));
		}
	}

	public void keyReleased(KeyEvent event) {}
	public void keyTyped(KeyEvent event) {}

	public void enter() {}
	public abstract void up(boolean shift);
	public abstract void down(boolean shift);


	public static class IntArrowKeyListener extends ArrowKeyListener {
		public int small = 1;
		public int big = 10;
		public JTextField textField;
		public Runnable update;

		public IntArrowKeyListener(JTextField textField, Runnable update) {
			this.textField = textField;
			this.update = update;
		}

		public void enter() {
			update.run();
		}
		public void down(boolean shift) {
			try {
				int value = Integer.parseInt(textField.getText().trim());
				value -= shift ? big : small;
				textField.setText(String.valueOf(value));
				update.run();
			} catch (NumberFormatException e) {
				// ignore if text doesn't parse to an integer
			}
		}
		public void up(boolean shift) {
			try {
				int value = Integer.parseInt(textField.getText().trim());
				value += shift ? big : small;
				textField.setText(String.valueOf(value));
				update.run();
			} catch (NumberFormatException e) {
				// ignore if text doesn't parse to an integer
			}
		}
	}
}
