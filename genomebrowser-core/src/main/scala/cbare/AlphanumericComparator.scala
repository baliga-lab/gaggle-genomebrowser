package cbare.util

import java.util.Comparator

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
 */
class AlphanumericComparator extends Comparator[String] {

  /**
   * @throws NullPointerException if s1 or s2 is null
   */
  def compare(s1: String, s2: String): Int = {
    var i = 0
    var j = 0
    var d1 = 0
    var d2 = 0
    var z1 = 0
    var z2 = 0
    var bias = 0
    val len1 = s1.length
    val len2 = s2.length

    while (i < len1 && j < len2) {
      // if we see digit characters, group blocks of digits into ints
      // and compare them numerically. There are two tricky issues:
      // leading zeros and numbers too large to be ints.
      if (Character.isDigit(s1.charAt(i)) && Character.isDigit(s2.charAt(j))) {
        // leading zeros don't matter for numeric comparison
        // but do matter for alphabetic comparison, so we 
        // record their position for settling ties later.
        z1 = i
        z2 = j
        while (i < len1 && s1.charAt(i) == '0') i += 1
        while (j < len2 && s2.charAt(j) == '0') j += 1

        // record the position of the first non-zero character
        d1 = i
        d2 = j

        // At this point, we may have eaten all the digits.
        // If so, bias will remain 0 meaning the two blocks of
        // digits are numerically equal.
        // bias idea stolen from strnatcmp.c by Martin Pool
        bias = 0
        var dobreak = false
        while (!dobreak && i < len1 && j < len2) {
          val a = s1.charAt(i)
          val b = s2.charAt(j)
          if (!(Character.isDigit(a) && Character.isDigit(b))) dobreak = true

          if (!dobreak) {
            if (a > b && bias == 0) bias = 1
            else if (a < b && bias == 0)	bias = -1
            i += 1
            j += 1
          }
        }

        // eat any remaining digits
        while (i < len1 && Character.isDigit(s1.charAt(i))) i += 1
        while (j < len2 && Character.isDigit(s2.charAt(j))) j += 1

        if ((i - d1) != (j - d2)) {
          // more digits is greater than fewer digits
          // (not counting leading zeros)
          return (i - d1) - (j - d2)
        }	else if (bias != 0) {
          // if the blocks are the same number of digits (not
          // counting leading zeros), the bias determines which
          // is greater.
          return bias
        }	else if ((i - z1) != (j - z2)) {
          // if the blocks of digits differ only in leading
          // zeros, more leading zeros are greater than fewer.
          return (i - z1) - (j - z2)
        }
      }	else if (s1.charAt(i) != s2.charAt(j)) {
        // compare characters alphabetically
        return s1.charAt(i) - s2.charAt(j)
      }	else {
        i += 1
        j += 1
      }
    }
    // if the alphanumeric strings were equal so far, and both have been
    // completely consumed, they must be equal
    if (i == len1 && j == len2) 0
    // if one string is the prefix of the other, the longer is greater
    else len1 - len2
  }
}
