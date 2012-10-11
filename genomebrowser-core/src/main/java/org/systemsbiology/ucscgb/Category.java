package org.systemsbiology.ucscgb;

/**
 * Enumerates the biological category of organisms used in the UCSC genome
 * browser. I've conflated their concept of clade with domain.
 * 
 * @author cbare
 */
public enum Category {
    all, virus, archaea, bacteria, prokaryotes, eukaryotes, deuterostome,
        insect, mammal, vertebrate, worm, yeast;
	
    public boolean isProkaryotic() {
        return (this == archaea || this == bacteria || this == prokaryotes);
    }

    public boolean isEukaryotic() {
        return (this == eukaryotes || this == deuterostome || this == insect ||
                this == mammal || this == vertebrate
                || this == worm || this == yeast);
    }

    public boolean isA(Category other) {
        if (other == null) return false;
        if (this == other) return true;
        if (other == all) return true;
        if (other == prokaryotes) {
            return (this == archaea || this == bacteria);
        }
        if (other == eukaryotes) {
            return (this == deuterostome || this == insect || this == mammal ||
                    this == vertebrate || this == worm || this == yeast);
        }
        return false;
    }

    public static Category fromString(String string) {
        if (string == null || string.equals("") || string.equals("other"))
            throw new IllegalArgumentException("No Category called \"" + string + "\"");
        string = string.toLowerCase();
        if (string.equals("viruses")) return Category.virus;
        if (string.equals("eukaryota")) return Category.eukaryotes;
        if (string.equals("prokaryota")) return Category.prokaryotes;
        return Enum.valueOf(Category.class, string);
    }

    public static Category fromDomainAndClade(String domain, String clade) {
        try {
            return fromString(clade);
        }	catch (Exception e) {
            return fromString(domain);
        }
    }
}
