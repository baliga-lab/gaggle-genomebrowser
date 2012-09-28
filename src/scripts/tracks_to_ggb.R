# import Elisabeth's tiling data into GGB.

# the script expects files with 3 columns, 

# follow these three instructions before running the script:

# 1.
# requires updated version of gaggle library available at:
# http://gaggle.systemsbiology.net/docs/geese/genomebrowser/help/r/
# library(gaggle)

# 2.
# load genome browser support code
# source("http://gaggle.systemsbiology.net/R/genome.browser.support.R")

# 3.
# start genome browser and broadcast a dataset description from GGB to R.
# receive it like this:
# ds <- getDatasetDescription()


# define filenames, use setwd(...) to set the current working
# directory to the folder holding these files
filenames <- c(
    '0_gMedianSignal.txt',
    '0_rMedianSignal.txt',
    '2_5_gMedianSignal.txt',
    '2_5_rMedianSignal.txt',
    '5_gMedianSignal.txt',
    '5_rMedianSignal.txt',
    '60_gMedianSignal.txt',
    '60_rMedianSignal.txt')


for (i in seq(along=filenames)) {
    filename <- filenames[i]
    cat('importing file: ', filename, '\n')
    
    # This is an attempt to clean control probs and misformatted data.
    # I'm not sure if it works.
    system(paste('grep NC_', filename, '> tmp.txt'))
    system(paste('awk \'{sub(/\t\t/,"\t");print}\' tmp.txt > clean.txt'))
    
    # read the cleaned data
    a <- read.table('clean.txt', header=T, sep="\t", as.is=T)

    # split locations in this format chr1:123-456 into sequence, start, and end
    loc <- strsplit(a[,2], ':')
    seq <- sapply(loc, function(x) {x[1]})
    coords <- strsplit(sapply(loc, function(x) {x[2]}), "-")
    starts <- sapply(coords, function(x) {x[1]})
    ends <- sapply(coords, function(x) {x[2]})

    # convert sequence names
    seq <- gsub('chr1', 'chr', seq)
    seq <- gsub('chr2', 'pNRC100', seq)
    seq <- gsub('chr3', 'pNRC200', seq)

    # create data frame with fields expected by the GGB
    df = data.frame(sequence=seq, strand='.', start=starts, end=ends, value=a[,3])

    # send track data to GGB
    attr <- list(color='0x80000066', description=filename, top=i/(2*length(filenames)), height=0.10)
    addTrack(ds, df, name=paste('tiling_',filename,sep=''), attributes=attr, auto.confirm=T)

    # Wait for the GGB to finish digesting data, may need to be adjusted
    # Seeing something like "RS_SQLite_exec: could not execute1: database is locked" is
    # a sign that the time needs to be increased
    Sys.sleep(5)
}
