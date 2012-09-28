package cbare.stringsearch;

import cbare.stringsearch.fa.Edge;
import cbare.stringsearch.fa.Node;



/**
 * Match strings against patterns with a wildcard character denoted by a '*'.
 * "\*" is a literal character '*'. "\\" = "\".
 * 
 * @author cbare
 */
public class WildcardPattern implements Pattern {
	Node start;
	boolean caseSensitive = false;

	public WildcardPattern(String p) {
		this.caseSensitive = false;
		compile(p);
	}

	public WildcardPattern(String p, boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
		compile(p);
	}

	public void compile(String p) {
		boolean escaped = false;
		start = new Node();
		Node back = null, n = start;
		for (int i = 0; i < p.length(); i++) {
			char c = p.charAt(i);
			if (c == '\\' && !escaped) {
				escaped = true;
			}
			// * = wildcard character
			else if (c == '*' && !escaped) {
				back = n;
				back.defaultEdge = back;
			}
			else {
				if (!caseSensitive)
					c = Character.toLowerCase(c);
				Node next = new Node();
				next.defaultEdge = back;
				Edge[] edges = new Edge[] { new Edge(c, next) };
				n.edges = edges;
				n = next;
				escaped = false;
			}
		}
	}

	/**
	 * @see cbare.stringsearch.Pattern#match(java.lang.String)
	 */
	public boolean match(String a) {
		if (a == null)
			return false;
		Node n = start;
		for (int i = 0; i < a.length(); i++) {
			char c = a.charAt(i);
			if (!caseSensitive)
				c = Character.toLowerCase(c);
			n = n.transition(c);
			if (n == null)
				return false;
		}
		return n.accept();
	}

}
