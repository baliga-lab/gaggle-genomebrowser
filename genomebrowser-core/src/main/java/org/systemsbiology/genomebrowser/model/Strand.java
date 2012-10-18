package org.systemsbiology.genomebrowser.model;


/**
 * Indicates a strand of double stranded nucleic acid. None indicates no strand
 * specificity, for example ChIP-chip data. Any is used to match features with
 * any strand value.
 * @see Feature
 * @see FeatureFilter 
 */
public enum Strand {
    forward, reverse, none, any;

    public static Strand intToStrand(int s) {
        if (s == 1) return forward;
        else if (s == -1) return reverse;
        else return none;
    }

    public static Strand fromString(String s) {
        if (s == null || "".equals(s)) return none;
        s = s.toLowerCase();
        if ("for".equals(s) || "+".equals(s) || "forward".equals(s)) return forward;
        if ("rev".equals(s) || "-".equals(s) || "reverse".equals(s)) return reverse;
        if (".".equals(s) || "none".equals(s)) return none;
        if ("*".equals(s) || "any".equals(s)) return any;
        try {
            int i = Integer.parseInt(s);
            return intToStrand(i);
        } catch (NumberFormatException e) { }
        return none;
    }

    public String toAbbreviatedString() {
        switch (this) {
        case forward: return "+";
        case reverse: return "-";
        case any: return "*";
        default: return ".";
        }
    }
    public boolean encompasses(Strand strand) { return (this==strand || this==Strand.any); }
    public static Strand[] both = new Strand[] {forward, reverse};
    public static Strand[] all = new Strand[] {forward, reverse, none};
    public static Strand opposite(Strand strand) {
        if (strand==forward) return reverse;
        if (strand==reverse) return forward;
        return strand;
    }
}
