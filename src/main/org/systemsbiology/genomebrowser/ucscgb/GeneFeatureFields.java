/**
 * 
 */
package org.systemsbiology.genomebrowser.ucscgb;

import org.systemsbiology.genomebrowser.model.GeneFeatureType;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.sqlite.FeatureFields;
import org.systemsbiology.ucscgb.Gene;
import org.systemsbiology.util.MathUtils;


/**
 * An adaptor between UCSC genes and GGB genes. Note that we need to correct
 * start coordinates because UCSC used zero based coordinates rather than one based.
 */
public class GeneFeatureFields implements FeatureFields {
	public Gene gene;

	public String getSequenceName() {
		return gene.chrom;
	}

	public String getStrand() {
		return Strand.fromString(gene.strand).toAbbreviatedString();
	}

	public int getStart() {
		// correct for zero-based coordinates used by UCSC GB.
		return gene.cdsStart + 1;
	}

	public int getEnd() {
		return gene.cdsEnd;
	}

	public int getPosition() {
		return MathUtils.average(getStart(), getEnd());
	}

	public String getName() {
		return gene.name;
	}

	public String getCommonName() {
		return gene.name2;
	}

	public String getGeneType() {
		return GeneFeatureType.fromString(gene.geneType).toString();
	}

	public double getValue() {
		return 0;
	}
}