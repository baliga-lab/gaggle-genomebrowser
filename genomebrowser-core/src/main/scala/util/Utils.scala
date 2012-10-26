package org.systemsbiology.util

/** Used to key a map with strings in a case insensitive way */
class CaseInsensitiveKey(string: String) {
  if (string == null)
    throw new NullPointerException("CaseInsensitiveKey requires a non-null string.");
  private lazy val lower = string.toLowerCase

  override def equals(obj: Any) = {
    if (obj == null) false
    else obj match {
      case key:CaseInsensitiveKey => lower == key.lower
      case str:String => lower == str.toLowerCase
      case _ => lower == obj.toString
    }
  }
  override def hashCode = lower.hashCode
  override def toString = string
}
