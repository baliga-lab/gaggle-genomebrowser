package cbare.stringsearch;

/**
 * A pattern against which strings may be tested for a match.
 */
public interface Pattern {

	/**
	 * Test whether the String a matches this pattern.
	 */
	public abstract boolean match(String a);
}
