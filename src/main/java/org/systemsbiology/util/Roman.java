package org.systemsbiology.util;

import java.util.regex.Pattern;


/*

   1000s         100s          10s           1s
--------------------------------------------------
0 = nothing  0 = nothing  0 = nothing  0 = nothing  
1 = M        1 = C        1 = X        1 = I        
2 = MM       2 = CC       2 = XX       2 = II       
3 = MMM      3 = CCC      3 = XXX      3 = III      
4 = MMMM     4 = CD       4 = XL       4 = IV       
5 = nothing  5 = D        5 = L        5 = V        
6 = nothing  6 = DC       6 = LX       6 = VI       
7 = nothing  7 = DCC      7 = LXX      7 = VII      
8 = nothing  8 = DCCC     8 = LXXX     8 = VIII     
9 = nothing  9 = CM       9 = XC       9 = IX       

*/


/**
 * Represent Roman numerals between 1 and 4999 inclusive.
 * 
 * @author Christopher Bare
 */
public class Roman implements Comparable<Roman> {
	private int value;
	private String roman;

	private final static String[][] digits = {
		{"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"},
		{"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"},
		{"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"},
		{"", "M", "MM", "MMM", "MMMM"}
	};

	private final static Pattern regex = Pattern.compile("M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})");


	public Roman(int n) {
		value = n;
		roman = intToRoman(n);
	}

	public Roman(String string) {
		if (isRoman(string)) {
			value = romanToInt(string);
			roman = string;
		}
		else
			throw new RuntimeException("\""+string+"\" is not a valid roman numeral.");
	}

	public String toString() {
		return roman;
	}

	public int toInt() {
		return value;
	}

	/**
	 * Test whether the input string is a valid roman numeral (between 1 and 4999).
	 */
	public static boolean isRoman(String s) {
		return s!=null && !"".equals(s) && regex.matcher(s.toUpperCase()).matches();
	}

	/**
	 * Convert an integer between 1 and 4999 (inclusive) to a Roman numeral.
	 */
	public static String intToRoman(int n) {
		if (n>=5000 || n <= 0)
			throw new RuntimeException("Roman equivalents exist only for numbers between 1 and 4999.");
		StringBuilder sb = new StringBuilder(16);
		int place = 0;
		while (n>0) {
			int qoutient = n/10;
			int remainder = n - (qoutient * 10);
			sb.insert(0, digits[place][remainder]);
			place++;
			n = qoutient;
		}
		return sb.toString();
	}

	/**
	 * Convert a roman numeral to an integer. This method isn't strict about
	 * the rules of what makes a valid roman numeral. In particular, it will
	 * happily convert IIIIIIIIII to 10 or IL to 49 even though neither of
	 * these are valid roman numerals. To check whether a String is a valid
	 * roman numeral use isRoman().
	 */
	public static int romanToInt(String roman) {
		int value = 0;
		int prev = 1000;

		roman = roman.toUpperCase();

		for (int i=0; i<roman.length(); i++) {
			int v = charToValue(roman.charAt(i));
			if (v > prev) {
				value -= (prev << 1);
			}
			value += v;
			prev = v;
		}

		return value;
	}

	/**
	 * @return return the integer value of a single roman character.
	 * @throws RuntimeException if the character isn't a roman numeral.
	 */
	private static int charToValue(char c) {
		if (c=='I') return 1;
		if (c=='V') return 5;
		if (c=='X') return 10;
		if (c=='L') return 50;
		if (c=='C') return 100;
		if (c=='D') return 500;
		if (c=='M') return 1000;
		throw new RuntimeException("Illegal character in roman numeral: " + c);
	}

	public int compareTo(Roman other) {
		return this.value - other.value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Roman other = (Roman) obj;
		return this.value == other.value;
	}
}
