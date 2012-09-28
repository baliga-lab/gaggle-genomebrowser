setwd('/Users/cbare/Documents/work/tiling_visualization_Tie/')
load('data/Median_quantiles_ref1_10.Rdata')
load('data/all.fitted.ann.Rdata')
ls()

toPlusMinus <- function(a) { if (a=="REVERSE") "+" else if (a=="FORWARD") "-" else ""; }
transcript.signal = data.frame(SEQ_ID=median.quantiles$SEQ_ID, START=median.quantiles$START, END=median.quantiles$END, STRAND=sapply(median.quantiles$GENE_EXPR_OPTION, toPlusMinus), Median=log2(median.quantiles$Median), q.05=log2(median.quantiles$q.05), q.95=log2(median.quantiles$q.95), Segmentation=all.fitted.ann$all.fitted)

transcript.signal = transcript.signal[ order(transcript.signal$START), ]

cols = c("START", "END", "STRAND", "Median", "q.05", "q.95", "Segmentation")
transcript.signal.NC_002607 = transcript.signal[ transcript.signal$SEQ_ID=="NC_002607", cols]
transcript.signal.NC_002608 = transcript.signal[ transcript.signal$SEQ_ID=="NC_002608", cols]
transcript.signal.NC_001869 = transcript.signal[ transcript.signal$SEQ_ID=="NC_001869", cols]

write.table(transcript.signal.NC_002607, file="transcript.signal.NC_002607.tsv", quote=FALSE, sep="\t", row.names=FALSE)
write.table(transcript.signal.NC_002608, file="transcript.signal.NC_002608.tsv", quote=FALSE, sep="\t", row.names=FALSE)
write.table(transcript.signal.NC_001869, file="transcript.signal.NC_001869.tsv", quote=FALSE, sep="\t", row.names=FALSE)
