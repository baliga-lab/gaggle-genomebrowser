#  Compute segment tracks from expression data.
#  Finds the average value over probes whose (central) position is within the segment boundaries. Returns
#  a data frame with columns (sequence, strand, start, end, value)
#
#  To be used with the data file: tilingArraySeg_mm_export.RData
#  Not complete for general use.
#
#  parameters:
#  df: data.frame in the form of refs or rats (with the following columns)
#       $ GENE_EXPR_OPTION: Factor w/ 2 levels "FORWARD","REVERSE"
#       $ POSITION        : int
#       plus some value column to be named in the parameter 'col'
#  breaks: positions of segment boundaries
#  seqs: names and lengths of sequences in the form: list( list('Chr', 'NC_002754', 2992245), list('SSV1', 'X07234', 15465) )
#        where 'Chr' is the name used to index the breaks data structure:
#        > str(breaks)
#        List of 2
#         $ Chr :List of 2
#          ..$ REVERSE: num [1:1902] 895 3191 3787 5295 6395 ...
#          ..$ FORWARD: num [1:1884] 5349 6355 11414 11774 11926 ...
#         $ SSV1:List of 2
#          ..$ REVERSE: num [1:2] 11284 11807
#          ..$ FORWARD: num [1:5] 1153 2732 11266 11366 12179
#        and 'NC_002754' is used in the SEQ_ID column of df
#        $ SEQ_ID                : Factor w/ 2 levels "NC_002754","X07234" ...
#
#  col: the name of the value column to be averaged over
#
# note that strands are assumed to be reversed in df and breaks
# not really suitable for general use.

segment <- function(df, breaks, seqs, col='value') {
	result = list()
	for (seq in seqs) {
		seq.name = seq[[1]]
		seq.id = seq[[2]]
		seq.len = seq[[3]]
		cat('name=', seq.name, ', id=',seq.id,', len=',seq.len, '\n')

		starts = c(0,breaks[[seq.name]]$REVERSE)
		ends = c(breaks[[seq.name]]$REVERSE, seq.len)
		cat('len + = ', length(starts), '\n')
		values = c()
		for (i in 1:(length(starts))) {
		  a = subset(df, SEQ_ID==seq.id & POSITION>=starts[i] & POSITION<ends[i] & GENE_EXPR_OPTION=='REVERSE', select=col)
		  values[i] = mean(a[,col])
		}
		result[[length(result)+1]] = data.frame(sequence=seq.name, strand='+', start=starts, end=ends, value=values)

		starts = c(0,breaks[[seq.name]]$FORWARD)
		ends = c(breaks[[seq.name]]$FORWARD, seq.len)
		values = c()
		cat('len - = ', length(starts), '\n')
		for (i in 1:(length(starts))) {
		  a = subset(df, SEQ_ID==seq.id & POSITION>=starts[i] & POSITION<ends[i] & GENE_EXPR_OPTION=='FORWARD', select=col)
		  values[i] = mean(a[,col])
		}
		result[[length(result)+1]] = data.frame(sequence=seq.name, strand='-', start=starts, end=ends, value=values)
	}

	return( do.call("rbind", result) )
}
