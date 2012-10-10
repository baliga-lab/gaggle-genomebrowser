package cbare.stringsearch

/**
 * A pattern against which strings may be tested for a match.
 */
trait Pattern {

	/** Test whether the String a matches this pattern. */
	def matches(a: String): Boolean
}

/**
 * Match strings against patterns with a wildcard character denoted by a '*'.
 * "\*" is a literal character '*'. "\\" = "\".
 */
class WildcardPattern(p: String, caseSensitive: Boolean=false) extends Pattern {

  // Start state
  private[this] var startState: Node = compile(p)

  /**
   * An edge in the finite automaton is associated with a character.
   * We can move from the source node to the target node by consuming
   * the character from the input.
   */
  class Edge(val c: Char, val node: Node) {
    def this(node: Node) = this(0, node)
  }

  /**
   * A node represents a state in a finite automaton. The node may
   * either be an accepting state or not.
   */
  class Node(var edges: Array[Edge]=null, var defaultEdge: Node=null) {

    def transition(c: Char): Node = {
      if (edges != null) {
        for (e <- edges) if (c == e.c) return e.node
      }
      if (defaultEdge != null) {
        if (defaultEdge == this) return this  // self edges consume the input character
        else return defaultEdge.transition(c) // for back edges, we go back without consuming
      } else return null
    }

    // doesn't work if we can build a pattern out of multiple keywords and
    // one keyword is a prefix of another
    def accept: Boolean = edges == null
  }


  private def compile(p: String) = {
    var escaped = false
    val start = new Node()
    var back: Node = null
    var n: Node = start

    for (i <- 0 until p.length) {
      var c = p.charAt(i)
      if (c == '\\' && !escaped) escaped = true
      else if (c == '*' && !escaped) {
        // * = wildcard character
        back = n
        back.defaultEdge = back
      } else {
        if (!caseSensitive) c = Character.toLowerCase(c)
        val next = new Node()
        next.defaultEdge = back
        val edges = Array(new Edge(c, next))
        n.edges = edges
        n = next
        escaped = false
      }
    }
    start
  }

  def matches(str: String): Boolean = {
    if (str == null) false
    else {
      var state = startState
      for (i <- 0 until str.length) {
        var c = str.charAt(i)
        if (!caseSensitive) c = Character.toLowerCase(c)
        state = state.transition(c)
        if (state == null) return false
      }
      state.accept
    }
  }
}
