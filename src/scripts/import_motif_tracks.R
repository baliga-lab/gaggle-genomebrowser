# a hacky script for importing motif cluster data, which comes in a sparse matrix
# called 'tab'.

# # load the motif cluster data
# load('motifCluster_posns.RData')
# 
# # connect to gaggle
# library(gaggle)
# gaggleInit()
# 
# # source the GGB / R interop code
# # source("http://gaggle.systemsbiology.net/R/genome.browser.support.R")
# source('~/Documents/work/eclipse-workspace-isb/GenomeBrowser/src/scripts/genome.browser.support.R')
# 
# # broadcast dataset description from R
# ds <- getDatasetDescription()


mms <- 1:ncols

attributes = list(color='#6699DD',groups='motif clusters',viewer='VerticalBar', rangeMax=1500, rangeMin=0)

load_motif_cluster <- function(m) {
  cat("processing motif cluster ", m, "\n")
  positions <- which(tab[,m]>0)
  values <- tab[ tab[,m]>0,m ]
  t1 <- data.frame( sequence='chromosome', strand='.', position=positions, value=values )
  addTrack(ds, t1, name= paste('motif cluster', m), auto=T, attributes=attributes, suppress.errors=FALSE)
  cat("track complete!\n")
}

for (m in mms) { load_motif_cluster(m); Sys.sleep(7) }
