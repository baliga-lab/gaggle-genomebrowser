package org.systemsbiology.util;

public class MathUtils {

	/**
	 * Clips an integer 'a' to be within the range [min, max].
	 */
	public static int confine(int a, int min, int max) {
		return (a < min) ? min : ((a > max) ? max : a);
	}

	/**
	 * Clip a double value to a specified range
     */
	public static double clip(double x, double floor, double ceil) {
		return (x < floor) ? floor : ((x > ceil) ? ceil : x);
	}

	public static int average(int a, int b) {
		// overflow proof average of two integers (whose sum might be larger than max int)
		return (a + b) >>> 1;
	}

	public static double average(double... values) {
		double sum = 0.0;
		for (double value : values) {
			sum += value;
		}
		return sum / values.length;
	}

	public static int max(int... ints) {
		int max = Integer.MIN_VALUE;
		for (int i : ints) {
			if (max < i) max = i;
		}
		return max;
	}
}
