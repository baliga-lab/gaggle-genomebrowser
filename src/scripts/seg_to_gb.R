chr.len=1908256

gb.refs = data.frame(sequence='chr', strand=toOppositeStrand(refs$GENE_EXPR_OPTION), start=refs$START, end=refs$END, value=apply(refs[,9:15], 1, mean))
ii <- (gb.refs$end - gb.refs$start) > 100
tmp.start <- gb.refs$start[ii]
gb.refs$start[ii] <- gb.refs$end[ii]
gb.refs$end[ii] <- tmp.start
addTrack(ds, gb.refs, name='reference average', attributes=list(source='tilingArraySeg_pf_export.RData 12/30/09', viewer='Scaling', color='0x80336699'))

breaks.forward = data.frame(sequence='chr', strand='+', position=breaks$Chr$REVERSE, value=0)
addTrack(ds, breaks.forward, name='breaks.forward', attributes=list(color='0x40003366', top=0, end=0.49))

breaks.reverse = data.frame(sequence='chr', strand='-', position=breaks$Chr$FORWARD, value=0)
addTrack(ds, breaks.reverse, name='breaks.reverse', attributes=list(color='0x40003366', top=0.5, height=0.5))


breaks.forward <- list()
breaks.reverse <- list()
for (i in seq(along=breaks)) {
	seq_id <- names(breaks)[i]
	breaks.forward[[seq_id]] <- data.frame(sequence=seq_id, strand='+', position=breaks[[i]]$REVERSE, value=0)
	breaks.reverse[[seq_id]] <- data.frame(sequence=seq_id, strand='-', position=breaks[[i]]$FORWARD, value=0)
}

bf <- do.call(rbind, breaks.forward)
br <- do.call(rbind, breaks.reverse)

addTrack(ds, bf, name='breaks.forward', attributes=list(color='0x40003366', top=0, height=0.49, viewer='VerticalDelimiter'))
addTrack(ds, br, name='breaks.reverse', attributes=list(color='0x40003366', top=0.5, height=0.5, viewer='VerticalDelimiter'))




gb.rats = data.frame(sequence=toSequence(rats$SEQ_ID), strand=toOppositeStrand(rats$GENE_EXPR_OPTION), start=rats$START, end=rats$END, value=rats[,9:21])
gb.rats = data.frame(sequence='chr', strand=toOppositeStrand(rats$GENE_EXPR_OPTION), start=rats$START, end=rats$END, value=rats[,9:15])
colnames(gb.rats) <- c('sequence', 'strand', 'start', 'end', 'value1', 'value2', 'value3', 'value4', 'value5', 'value6', 'value7')

ii <- (gb.rats$end - gb.rats$start) > 100
tmp.start <- gb.rats$start[ii]
gb.rats$start[ii] <- gb.rats$end[ii]
gb.rats$end[ii] <- tmp.start

addTrack(ds, gb.rats, name='ratios', type='quantitative.segment.matrix', attributes=list(description='expression time series: 05h_0.036, 06.5h_0.041, 08h_0.124, 10h_0.23, 12h_0.292, 13h_0.322, 16h_0.354', color='0x80336699', viewer='MatrixHeatmap'))


segs.for <- data.frame(sequence='chr', strand='+', start=c(1,breaks$Chr$REVERSE), end=c(breaks$Chr$REVERSE, chr.len))
segs.rev <- data.frame(sequence='chr', strand='-', start=c(1,breaks$Chr$FORWARD), end=c(breaks$Chr$FORWARD, 1908256))
segs <- rbind(segs.for, segs.rev)

probes <- data.frame(probe_id=rats$PROBE_ID, sequence='chr', strand=toOppositeStrand(rats$GENE_EXPR_OPTION), start=rats$START, end=rats$END)
ppf <- data.frame(probe_id=names(probe.probs$Chr$REVERSE), value=probe.probs$Chr$REVERSE)
ppf <- ppf[ !is.na(ppf$value), ]
ppr <- data.frame(probe_id=names(probe.probs$Chr$FORWARD), value=probe.probs$Chr$FORWARD)
ppr <- ppr[ !is.na(ppr$value), ]
pp <- rbind(ppf, ppr)
ppm <- merge(probes, pp, by='probe_id')
ppms <- ppm[,2:6]
head(ppms)
addTrack(ds, ppms, name='prob.expressed')


head(break.densities$Chr$FORWARD)
bdf <- data.frame(sequence='chr', strand='+', position=break.densities$Chr$REVERSE[,1], value=break.densities$Chr$REVERSE[,2])
bdr <- data.frame(sequence='chr', strand='-', position=break.densities$Chr$FORWARD[,1], value=break.densities$Chr$FORWARD[,2])
bd <- rbind(bdf, bdr)
addTrack(ds, bd, name='break.densities')

bd.peices <- list()
j <- 0
for (i in seq(along=break.densities)) {
	seq_id <- names(break.densities)[i]
	j <- j+1
	bd.peices[[j]] <- data.frame(sequence=seq_id, strand='+', position=break.densities[[i]]$REVERSE[,1], value=break.densities[[i]]$REVERSE[,2])
	j <- j+1
	bd.peices[[j]] <- data.frame(sequence=seq_id, strand='-', position=break.densities[[i]]$FORWARD[,1], value=break.densities[[i]]$FORWARD[,2])
}

bd <- do.call(rbind, bd.peices)



CREATE TABLE features_peaks_chip_chip_tfbd_500bp (
 sequences_id integer NOT NULL,
 strand text NOT NULL,
 position integer NOT NULL,
 value numeric);
CREATE TABLE features_chip_chip_tfbd_500bp (
 sequences_id integer NOT NULL,
 strand text NOT NULL,
 start integer NOT NULL,
 end integer NOT NULL,
 value numeric);
CREATE TABLE features_chip_chip_tfbd_nimb (
 sequences_id integer NOT NULL,
 strand text NOT NULL,
 start integer NOT NULL,
 end integer NOT NULL,
 value numeric);
CREATE TABLE features_peaks_chip_chip_tfbd_nimb (
 sequences_id integer NOT NULL,
 strand text NOT NULL,
 position integer NOT NULL,
 value numeric);



