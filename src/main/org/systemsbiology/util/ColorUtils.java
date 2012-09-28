package org.systemsbiology.util;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some functions copied from no.geosoft.cc.color.ui release under LGPL.
 * see: http://www.java2s.com/Code/Java/2D-Graphics-GUI/Commoncolorutilities.htm
 */
public class ColorUtils {

	public static int replaceAlpha(int alpha, int argb) {
		return (alpha << 24) | (0x00ffffff & argb);
	}


	/**
	 * decodes a String containing a 4 byte hex value as a color
	 * with an optional alpha (transparency) channel. A zero
	 * transparency byte is treated as an opaque color.
	 */
	public static Color decodeColor(String string) {
		if (string==null) return Color.BLACK;
		
		// decode a string of the form: "java.awt.Color[r=51,g=102,b=153]"
		if (string.startsWith("java.awt.Color")) {
			Pattern pattern = Pattern.compile("java.awt.Color\\[r=(\\d+),\\s*g=(\\d+),\\s*b=(\\d+)\\s*\\]");
			Matcher m = pattern.matcher(string);
			if (m.matches()) {
				int r = Integer.parseInt(m.group(1));
				int g = Integer.parseInt(m.group(2));
				int b = Integer.parseInt(m.group(3));
				return new Color(r, g, b);
			}
		}

		// use Long.decode 'cause Integer.decode is broken for hex
		// values with the sign bit set. (bastards!)
		int c = Long.decode(string).intValue();
		// c >>> 24 > 0 checks for a transparency (alpha channel)
		// if alpha channel is zero, we return an opaque color by
		// setting hasAplha to false.
		return new Color(c, c >>> 24 > 0);
	}


	/**
	 * @return a hex representation of the given color.
	 */
	public static String colorToString(Color c) {
		return Integer.toHexString(c.getRGB());
	}

	/**
	 * @param c a color
	 * @param factor between 0.0 and 1.0
	 * @return returns a new color based on the given color with its transparency
	 * (alpha) scaled by a factor.
	 */
	public static Color deriveTransparentColorFrom(Color c, double factor) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(255, (int)Math.round(c.getAlpha()*factor)));
	}


	/**
	 * Blend two colors.
	 * 
	 * @param color1  First color to blend.
	 * @param color2  Second color to blend.
	 * @param ratio   Blend ratio. 0.5 will give even blend, 1.0 will return
	 *                color1, 0.0 will return color2 and so on.
	 * @return        Blended color.
	 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
	 * @license GNU LGPL
	 */
	public static Color blend (Color color1, Color color2, double ratio)
	{
		float r  = (float) ratio;
		float ir = (float) 1.0 - r;

		float rgb1[] = new float[3];
		float rgb2[] = new float[3];    

		color1.getColorComponents (rgb1);
		color2.getColorComponents (rgb2);    

		Color color = new Color (rgb1[0] * r + rgb2[0] * ir, 
				rgb1[1] * r + rgb2[1] * ir, 
				rgb1[2] * r + rgb2[2] * ir);

		return color;
	}


	/**
	 * Make an even blend between two colors.
	 * 
	 * @param c1     First color to blend.
	 * @param c2     Second color to blend.
	 * @return       Blended color.
	 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
	 * @license GNU LGPL
	 */
	public static Color blend (Color color1, Color color2)
	{
		return ColorUtils.blend (color1, color2, 0.5);
	}


	/**
	 * Make a color darker.
	 * 
	 * @param color     Color to make darker.
	 * @param fraction  Darkness fraction.
	 * @return          Darker color.
	 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
	 * @license GNU LGPL
	 */
	public static Color darker (Color color, double fraction)
	{
		int red   = (int) Math.round (color.getRed()   * (1.0 - fraction));
		int green = (int) Math.round (color.getGreen() * (1.0 - fraction));
		int blue  = (int) Math.round (color.getBlue()  * (1.0 - fraction));

		if (red   < 0) red   = 0; else if (red   > 255) red   = 255;
		if (green < 0) green = 0; else if (green > 255) green = 255;
		if (blue  < 0) blue  = 0; else if (blue  > 255) blue  = 255;    

		int alpha = color.getAlpha();

		return new Color (red, green, blue, alpha);
	}


	/**
	 * Make a color lighter.
	 * 
	 * @param color     Color to make lighter.
	 * @param fraction  Darkness fraction.
	 * @return          Lighter color.
	 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
	 * @license GNU LGPL
	 */
	public static Color lighter (Color color, double fraction)
	{
		int red   = (int) Math.round (color.getRed()   * (1.0 + fraction));
		int green = (int) Math.round (color.getGreen() * (1.0 + fraction));
		int blue  = (int) Math.round (color.getBlue()  * (1.0 + fraction));

		if (red   < 0) red   = 0; else if (red   > 255) red   = 255;
		if (green < 0) green = 0; else if (green > 255) green = 255;
		if (blue  < 0) blue  = 0; else if (blue  > 255) blue  = 255;    

		int alpha = color.getAlpha();

		return new Color (red, green, blue, alpha);
	}
}
