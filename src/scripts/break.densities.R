
# break densities are computed at very high resolution. Here, we down-sample it, so
# it won't take up a ton of space.
process.break.densities <- function() {
	bd.p200.f <- approx(break.densities$pNRC200$REVERSE[,1], break.densities$pNRC200$REVERSE[,2], xout=seq(1,365425,10))
	bd.p200.f <- data.frame(sequence='pNRC200', strand='+', position=bd.p200.f$x, value=bd.p200.f$y)
	bd.p200.f <- bd.p200.f[ !is.na(bd.p200.f$value), ]

	bd.p200.r <- approx(break.densities$pNRC200$FORWARD[,1], break.densities$pNRC200$FORWARD[,2], xout=seq(1,365425,10))
	bd.p200.r <- data.frame(sequence='pNRC200', strand='-', position=bd.p200.r$x, value=bd.p200.r$y)
	bd.p200.r <- bd.p200.r[ !is.na(bd.p200.r$value), ]

	bd.p100.f <- approx(break.densities$pNRC100$REVERSE[,1], break.densities$pNRC100$REVERSE[,2], xout=seq(1,191346,10))
	bd.p100.f <- data.frame(sequence='pNRC100', strand='+', position=bd.p100.f$x, value=bd.p100.f$y)
	bd.p100.f <- bd.p100.f[ !is.na(bd.p100.f$value), ]

	bd.p100.r <- approx(break.densities$pNRC100$FORWARD[,1], break.densities$pNRC100$FORWARD[,2], xout=seq(1,191346,10))
	bd.p100.r <- data.frame(sequence='pNRC100', strand='-', position=bd.p100.r$x, value=bd.p100.r$y)
	bd.p100.r <- bd.p100.r[ !is.na(bd.p100.r$value), ]

	bd.chr.f <- data.frame(sequence='chromosome', strand='+', position=break.densities$Chr$REVERSE[,1], value=break.densities$Chr$REVERSE[,2])
	bd.chr.r <- data.frame(sequence='chromosome', strand='-', position=break.densities$Chr$FORWARD[,1], value=break.densities$Chr$FORWARD[,2])

	bd <- rbind(bd.chr.f, bd.chr.r, bd.p200.f, bd.p200.r, bd.p100.f, bd.p100.r)
}

halo.sequences = list(
	Chr=list(name='Chr',gb.name='chromosome',refseq='NC_002607',length=2014239),
	pNRC200=list(name='pNRC200', gb.name='pNRC200', refseq='NC_002608', length=365425),
	pNRC100=list(name='pNRC100', gb.name='pNRC100', refseq='NC_001869', length=191346))


toSequence <- function(seqIds) {
	seqIds <- as.vector(seqIds)
	results <- character(length=length(seqIds))
	for (i in seq(along=seqIds)) {
		seqId <- seqIds[i]
		if (seqId=='NC_002607') results[i] <- 'chromosome'
		else if (seqId=='NC_002608') results[i] <- 'pNRC200'
		else if (seqId=='NC_001869') results[i] <- 'pNRC100'
		else {
			warning(paste('unknown sequence:',seqId))
			results[i] <- seqId
		}
	}
	return(results)
}


probes <- data.frame(probe_id=refs$PROBE_ID, sequence=toSequence(refs$SEQ_ID), strand=toOppositeStrand(refs$GENE_EXPR_OPTION), start=refs$START, end=refs$END)



# Process a nested list structure in the following format:
#   struct$SEQ_ID$STRAND$SUB
# f.process.sub(sequence, strand, sub)
# f.combine.subs(list.of.processed.subs)
# f.process.sequence(seqId) optional
process.struct <- function(struct, f.process.sub, f.combine.subs=rbind.list, f.process.sequence=NULL) {

	rbind.list <- function(subs) {
		do.call(rbind, subs)
	}
	
	subs = list()
	for (i in seq(along=struct)) {
		seq <- if (!is.null(f.process.sequence))
			f.process.sequence(names(struct)[i])
		else
			names(struct)[i]
		substruct <- struct[[i]]
		for (j in seq(along=substruct)) {
			strand = toOppositeStrand(names(substruct))[j]
			subs[[length(subs)+1]] <- f.process.sub(seq, strand, substruct[[j]])
		}
	}
	return(f.combine.subs(subs))
}



f.process.sub <- function(seq, strand, sub) {
	data.frame(probe_id=names(sub), value=sub)
}


pp <- process.struct(probe.probs, f.process.sub)

