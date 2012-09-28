package cbare.stringsearch.fa;


/**
 * An edge in the finite automaton is associated with a character.
 * We can move from the source node to the target node by consuming
 * the character from the input.
 */
public class Edge {
	public char c;
	public Node node;

	public Edge(char c, Node node) {
		super();
		this.c = c;
		this.node = node;
	}

	public Edge(Node node) {
		super();
		this.node = node;
	}
	
}
