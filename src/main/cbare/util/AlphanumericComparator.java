package cbare.util;

import java.util.Comparator;

/**
 * compare strings that start with integers numerically. Strings that do not
 * start with an integer are sorted alphabetically.
 * 
 * Here's an example sorted array of junk:
 *    1
 *    01
 *    001
 *    001
 *    0001
 *    2
 *    002
 *    3doodle
 *    6
 *    7
 *    9doodle
 *    10
 *    010
 *    11
 *    11doodle
 *    12doodle
 *    14
 *    33
 *    99
 *    100doodle
 *    101
 *    110ac
 *    110ba
 *    127.0.0.1
 *    127.0.0.2
 *    127.0.0.10
 *    127.0.0.10
 *    127.0.10.10
 *    asdf001aaa
 *    asdf001xyz
 *    asdf001zzz
 *    asdf0001xyz
 *    halobacterium
 *    moose
 *    whatever
 *
 * @author cbare
 *
 */
public class AlphanumericComparator implements Comparator<String> {

	/**
	 * @throws NullPointerException if s1 or s2 is null
	 */
	public int compare(String s1, String s2) {
		int i=0, j=0;
		int d1, d2;
		int z1, z2, bias;
		int len1 = s1.length();
		int len2 = s2.length();

		while (i < len1 && j < len2) {
			// if we see digit characters, group blocks of digits into ints
			// and compare them numerically. There are two tricky issues:
			// leading zeros and numbers too large to be ints.
			if (Character.isDigit(s1.charAt(i)) && Character.isDigit(s2.charAt(j))) {
				// leading zeros don't matter for numeric comparison
				// but do matter for alphabetic comparison, so we 
				// record their position for settling ties later.
				z1=i;
				z2=j;
				while (i < len1 && s1.charAt(i)=='0') i++;
				while (j < len2 && s2.charAt(j)=='0') j++;

				// record the position of the first non-zero character
				d1=i;
				d2=j;

				// At this point, we may have eaten all the digits.
				// If so, bias will remain 0 meaning the two blocks of
				// digits are numerically equal.

				// bias idea stolen from strnatcmp.c by Martin Pool
				bias = 0;
				while (i < len1 && j < len2) {
					char a = s1.charAt(i);
					char b = s2.charAt(j);
					if (!(Character.isDigit(a) && Character.isDigit(b)))
						break;

					if (a > b) {
						if (bias == 0)
							bias = 1;
					}
					else if (a < b) {
						if (bias ==0)
							bias = -1;
					}
					i++;
					j++;
				}

				// eat any remaining digits
				while (i < len1 && Character.isDigit(s1.charAt(i))) i++;
				while (j < len2 && Character.isDigit(s2.charAt(j))) j++;

				if ((i-d1) != (j-d2)) {
					// more digits is greater than fewer digits
					// (not counting leading zeros)
					return (i-d1) - (j-d2);
				}
				else if (bias != 0) {
					// if the blocks are the same number of digits (not
					// counting leading zeros), the bias determines which
					// is greater.
					return bias;
				}
				else if ((i-z1) != (j-z2)) {
					// if the blocks of digits differ only in leading
					// zeros, more leading zeros are greater than fewer.
					return (i-z1) - (j-z2);
				}
			}
			// compare characters alphabetically
			else if (s1.charAt(i) != s2.charAt(j)) {
				return s1.charAt(i) - s2.charAt(j);
			}
			else {
				i++;
				j++;
			}
		}

		// if the alphanumeric strings were equal so far, and both have been
		// completely consumed, they must be equal
		if (i==len1 && j==len2) {
			return 0;
		}
		// if one string is the prefix of the other, the longer is greater
		else
			return len1 - len2;
	}

}
