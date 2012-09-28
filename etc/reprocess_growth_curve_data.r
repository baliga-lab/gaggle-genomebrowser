
# setwd("/Users/cbare/Documents/work/eclipse-workspace-isb/GenomeBrowser/data/Halo_growth/Halo_growth")

#
# take multiple track files containing values measured for the same set of probes
# and aggregate them into a single tabular data file
#
aggregate_tracks <- function(dir, files, suffix, input.file, output.file) {
  ts = read.table(paste(dir, input.file,  sep=""), header=T, sep="\t")
  for (filename in files) {
    a = read.table(paste(dir, filename, suffix, ".tsv", sep=""), header=T, sep="\t")
    print(paste("dim(a) =", toString(dim(a))))

    if (dim(a)[1]/2 == dim(ts)[1]) {
      values1 = a[seq(1,dim(a)[1], by=2),3]
      values2 = a[seq(2,dim(a)[1], by=2),3]
      ts = data.frame(ts, values1, values2)
      n = names(ts)
      n[length(n)-1] = paste(filename, ".1", sep="")
      n[length(n)]   = paste(filename, ".2", sep="")
      names(ts) <- n
    }
    else if (dim(a)[1]/3 == dim(ts)[1]) {
      values1 = a[seq(1,dim(a)[1], by=3),3]
      values2 = a[seq(2,dim(a)[1], by=3),3]
      values3 = a[seq(3,dim(a)[1], by=3),3]
      ts = data.frame(ts, values1, values2, values3)
      n = names(ts)
      n[length(n)-2] = paste(filename, ".1", sep="")
      n[length(n)-1] = paste(filename, ".2", sep="")
      n[length(n)]   = paste(filename, ".3", sep="")
      names(ts) <- n
    }
  }
  write.table(ts, file=paste(dir, output.file, sep=""), quote=F, sep="\t", row.names=F)
}

# the OD's here are estimated. OD's in the column titles are measured
files = c('log10ratio_OD.0.2_replica.1','log10ratio_OD.0.4_replica.1','log10ratio_OD.0.6_replica.1','log10ratio_OD.0.7_replica.1','log10ratio_OD.0.8_replica.1','log10ratio_OD.0.7a_replica.1','log10ratio_OD.1.3_replica.1','log10ratio_OD.2.5_replica.1','log10ratio_OD.3.8_replica.1','log10ratio_OD.4.5_replica.1','log10ratio_OD.5.0_replica.1','log10ratio_OD.6.0_replica.1','log10ratio_OD.6.0a_replica.1')

aggregate_tracks("chromosome/", files, "_Chr_FORWARD", "transcript.signal.forward.tsv", "transcription.signal.all.forward.tsv")
aggregate_tracks("chromosome/", files, "_Chr_REVERSE", "transcript.signal.reverse.tsv", "transcription.signal.all.reverse.tsv")
aggregate_tracks("pNRC200/", files, "_pNRC200_FORWARD", "transcript.signal.forward.tsv", "transcription.signal.all.forward.tsv")
aggregate_tracks("pNRC200/", files, "_pNRC200_REVERSE", "transcript.signal.reverse.tsv", "transcription.signal.all.reverse.tsv")
aggregate_tracks("pNRC100/", files, "_pNRC100_FORWARD", "transcript.signal.forward.tsv", "transcription.signal.all.forward.tsv")
aggregate_tracks("pNRC100/", files, "_pNRC100_REVERSE", "transcript.signal.reverse.tsv", "transcription.signal.all.reverse.tsv")
