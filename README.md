##  Genome Browser

The genome browser is used to visualize data of various kinds plotted against the genome. Tiling
microarrays and ChIP-chip are two possible use-cases. For more information see:

http://gaggle.systemsbiology.net/docs/geese/genomebrowser/


## Building

	ant

## Running

	./genomebrowser.sh


## Dependencies
  log4j
  junit
  gaggle (see gaggle.systemsbiology.net)
  colorpicker.jar from https://colorchooser.dev.java.net/ and http://javagraphics.blogspot.com/2007/04/jcolorchooser-making-alternative.html
  sqlite

  CaliforniaIconPack, Crystal project icons, Icons stolen from Eclipse.  


## Known Bugs

Features that straddle the zero point on circular chromosomes aren't handled properly.

* NCBI interface:
  * lproks.cgi and leuks.cgi were replaced by ftp://ftp.ncbi.nih.gov/genomes/GENOME_REPORTS/
    need to be worked into NCBI API

## TODO

* Data exchange (NCBI/UCSC) should be external dependency
