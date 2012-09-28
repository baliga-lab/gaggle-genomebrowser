package cbare.stringsearch.fa;


/**
 * A node represents a state in a finite automaton. The node may
 * either be an accepting state or not.
 */
public class Node {

    public Edge[] edges;
    public Node defaultEdge;

    public Node transition(char c) {
    	if (edges != null) {
			for (Edge e: edges) {
				if (c==e.c)
					return e.node;
			}
    	}
    	if (defaultEdge != null) {
    		if (defaultEdge == this)
        		// self edges consume the input character
    			return this;
    		else
    			// for back edges, we go back without consuming
    			return defaultEdge.transition(c);
    	}
    	else
    		return null;
    }

    // doesn't work if we can build a pattern out of multiple keywords and
    // one keyword is a prefix of another
    public boolean accept() {
    	return edges == null;
    }
}
