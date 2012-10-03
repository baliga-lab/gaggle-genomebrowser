package cbare.stringsearch;

/**
 * Match strings against patterns with a wildcard character denoted by a '*'.
 * "\*" is a literal character '*'. "\\" = "\".
 * 
 * @author cbare
 */
public class WildcardPattern implements Pattern {

    /**
     * An edge in the finite automaton is associated with a character.
     * We can move from the source node to the target node by consuming
     * the character from the input.
     */
    static class Edge {
        public char c;
        public Node node;

        public Edge(char c, Node node) {
            this.c = c;
            this.node = node;
        }
        public Edge(Node node) {
            this((char) 0, node);
        }
    }

    /**
     * A node represents a state in a finite automaton. The node may
     * either be an accepting state or not.
     */
    static class Node {

        public Edge[] edges;
        public Node defaultEdge;

        public Node transition(char c) {
            if (edges != null) {
                for (Edge e: edges) if (c == e.c) return e.node;
            }
            if (defaultEdge != null) {
                if (defaultEdge == this) return this;  // self edges consume the input character
                else return defaultEdge.transition(c); // for back edges, we go back without consuming
            } else  return null;
        }

        // doesn't work if we can build a pattern out of multiple keywords and
        // one keyword is a prefix of another
        public boolean accept() { return edges == null; }
    }

    private Node start;
    private boolean caseSensitive;

    public WildcardPattern(String p, boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        compile(p);
    }

    public WildcardPattern(String p) {
        this(p, false);
    }

    private void compile(String p) {
        boolean escaped = false;
        start = new Node();
        Node back = null, n = start;
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == '\\' && !escaped) {
                escaped = true;
            } else if (c == '*' && !escaped) {
                // * = wildcard character
                back = n;
                back.defaultEdge = back;
            } else {
                if (!caseSensitive) c = Character.toLowerCase(c);
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
            if (!caseSensitive) c = Character.toLowerCase(c);
            n = n.transition(c);
            if (n == null) return false;
        }
        return n.accept();
    }
}
