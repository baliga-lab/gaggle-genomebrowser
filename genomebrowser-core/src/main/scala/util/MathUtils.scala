package org.systemsbiology.util

object MathUtils {

  /** Clips an integer 'a' to be within the range [min, max]. */
  def confine(a: Int, min: Int, max: Int): Int = {
    if (a < min) min else (if (a > max) max else a)
  }
  /** Clip a double value to a specified range */
  def clip(x: Double, floor: Double, ceil: Double): Double = {
    if (x < floor) floor else (if (x > ceil) ceil else x)
  }
  // overflow proof average of two integers (whose sum might be larger than max int)
  def average(a: Int, b: Int): Int = (a + b) >>> 1
  def average(values: Array[Double]): Double = values.sum / values.length
  def max(ints: Array[Int]): Int = {
    var max = Integer.MIN_VALUE
    for (i <- ints) if (max < i) max = i
    max
  }
}
