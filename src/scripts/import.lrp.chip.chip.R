# Read a clone file and build a data frame suitable for import to the
# Gaggle Genome Browser. Made for processing Fang Yin's ChIP-chip data.

# depends on genome.browser.support.R, medichi and the gaggle R package
# library(MeDiChI)
# source("/Users/cbare/Documents/work/eclipse-workspace-isb/GenomeBrowser/src/scripts/genome.browser.support.R")
# source("/Users/cbare/Documents/work/projects/FYL-p1351/clone_file_to_GGB.R")

# I always end up printing really large things by mistake, so let's head that off at the pass.
options("max.print"=200)

# filenames <- c('p1351cmyc_1.1_IP_vs_p1351cmyc_1.1_WCE.clone',
#                'p1351cmyc_1.2_IP_vs_p1351cmyc_1.2_WCE.clone',
#                'ptrh4cmyc_1.2_IP_vs_ptrh4cmyc_1.2_WCE.clone')

make.data.frame <- function(c, values) {
    # Build a list of sequences for each row, substituting chr, pNRC200 and
    # pNRC100 for the NCBI accession numbers.
    seqs <- gsub(".*NC_00260(7|9).*", "chr", c$external_ID, perl=T)
    seqs <- gsub(".*NC_002608.*", "pNRC200", seqs, perl=T)
    seqs <- gsub(".*NC_001869.*", "pNRC100", seqs, perl=T)

    # Build a data frame from the sequences, genome coordinates Xn/Yn ratio.
    # x/y correlates w/ muRatio.
    # The period value for strand means no strand (or both).
    df <- data.frame(sequence=seqs, strand=".", start=c$chromosome_start_position, end=c$chromosome_end_position, value=values)

    # Clear out error correcting measurements which won't have a sequence name
    df <- df[seqs!="", ]
}


# load a clone file and massage it for import to GGB.
# filename is the name of the clone file
# ds is the dataset description from GGB.
# We assume that sample is in the X column and reference is in the Y
# column and compute sample/reference for each of two replicates.
# In general, I think you have to check as is done in Dave Reiss's medichi.utils.R
process.track <- function(filename, ds, name=NULL, attr=NULL) {
    # Load our clone file
    c <- read.table(filename, header=T, sep="\t", quote="", comment.char="")
    dim(c)

    # The header row starts with a # character, so we'll need to fix the
    # first column name.
    #colnames(c)
    colnames(c)[1] <- "GENE_NAME"

    # we append the two replicates to plot them in the same track
    df0 <-  make.data.frame(c, c$X0/c$Y0)
    df1 <-  make.data.frame(c, c$X1/c$Y1)
    df <- rbind(df0, df1)
    
    # sort unnecessary as it's done by GGB
    #df <- df[ order(df$sequence, df$strand, df$start, df$end), ]

    # define some attributes for the new track
    if (is.null(attr)) {
        attr <- list()
    }
    attr$source <- filename
    attr$viewer <- 'Scaling'
    attr$visible <- TRUE
    
    if (is.null(name)) {
        name <- filename
    }
    
    # broadcast track to GGB
    addTrack(ds, df, name=name, attributes=attr, auto.confirm=TRUE)
}


# given a GGB dataset description, import LRP tracks
# from HM to a GGB instance
import.lrp.tracks <- function(ds) {
    dir <- "/Volumes/HM/FangYin/Lrp_ChIPChip/"
    setwd(dir)

    filenames <- c("asnCcmyc_1.1_IP_vs_asnCcmyc_1.1_WCE.clone",
                "asnCcmyc_2.1_IP_vs_asnCcmyc_2.1_WCE.clone",
                "asnCcmyc_2.2_IP_vs_asnCcmyc_2.2_WCE.clone",
                "p1179cmyc_1.1_IP_vs_p1179cmyc_1.1_WCE.clone",
                "p1179cmyc_1.2_IP_vs_p1179cmyc_1.2_WCE.clone",
                "p1179cmyc_2.1_IP_vs_p1179cmyc_2.1_WCE.clone",
                "p1179cmyc_2.2_IP_vs_p1179cmyc_2.2_WCE.clone",
                "p1237cmyc_1.1_IP_vs_p1237cmyc_1.1_WCE.clone",
                "p1237cmyc_1.2_IP_vs_p1237cmyc_1.2_WCE.clone",
                "p1237cmyc_2.1_IP_vs_p1237cmyc_2.1_WCE.clone",
                "p1237cmyc_2.2_IP_vs_p1237cmyc_2.2_WCE.clone",
                "p1351cmyc_1.1_IP_vs_p1351cmyc_1.1_WCE.clone",
                "p1351cmyc_1.2_IP_vs_p1351cmyc_1.2_WCE.clone",
                "p1351cmyc_2.1_IP_vs_p1351cmyc_2.1_WCE.clone",
                "p1351cmyc_2.2_IP_vs_p1351cmyc_2.2_WCE.clone",
                "pMTFcmyc_1.1_IP_vs_pMTFcmyc_1.1_WCE.clone",
                "pMTFcmyc_1.2_IP_vs_pMTFcmyc_1.2_WCE.clone",
                "pMTFcmyc_2.1_IP_vs_pMTFcmyc_2.1_WCE.clone",
                "pMTFcmyc_2.2_IP_vs_pMTFcmyc_2.2_WCE.clone",
                "ptrh4cmyc_1.1_IP_vs_ptrh4cmyc_1.1_WCE.clone",
                "ptrh4cmyc_1.2_IP_vs_ptrh4cmyc_1.2_WCE.clone",
                "ptrh4cmyc_2.1_IP_vs_ptrh4cmyc_2.1_WCE.clone",
                "trh2cmyc_1.1_IP_vs_trh2cmyc_1.1_WCE.clone",
                "trh2cmyc_1.2_IP_vs_trh2cmyc_1.2_WCE.clone",
                "ptrh2cmyc_2.1_IP_vs_ptrh2cmyc_2.1_WCE.clone",
                "trh2cmyc_2.2_IP_vs_trh2cmyc_2.2_WCE.clone",
                "trh3cmyc_1.1_IP_vs_trh3cmyc_1.1_WCE.clone",
                "trh3cmyc_1.2_IP_vs_trh3cmyc_1.2_WCE.clone",
                "ptrh3cmyc_2.1_IP_vs_ptrh3cmyc_2.1_WCE.clone",
                "trh3cmyc_2.2_IP_vs_trh3cmyc_2.2_WCE.clone",
                "trh6cmyc_1.1_IP_vs_trh6cmyc_1.1_WCE.clone",
                "trh7cmyc_1.1_IP_vs_trh7cmyc_1.1_WCE.clone",
                "trh7cmyc_1.2_IP_vs_trh7cmyc_1.2_WCE.clone",
                "trh7cmyc_2.2_IP_vs_trh7cmyc_2.2_WCE.clone")

    overlays <- c(  rep('asnCcmyc', 3),
                    rep('p1179cmyc',4),
                    rep('p1237cmyc',4),
                    rep('p1351cmyc',4),
                    rep('pMTFcmyc',4),
                    rep('ptrh4cmyc', 3),
                    rep('trh2cmyc', 4),
                    rep('trh3cmyc', 4),
                    rep('trh6cmyc', 1),
                    rep('trh7cmyc', 3)   )

    cols <- topo.colors(length(filenames))

    for (i in seq_along(filenames)) {
        name <- gsub('\\.clone', '', filenames[i])
        cat("importing file: ", filenames[i], "\n" )
        process.track(filenames[i], ds, name=name, attr=list(color=cols[i],overlay=overlays[i]))
        cat("finished importing file: ", filenames[i], "\n" )
        Sys.sleep(7)
    }

}


import.fits <- function(attr=list()) {
    fits <- c("asnC.1.1.fits", "asnC.2.1.fits", "asnC.2.2.fits",
        "p1179.1.1.fits", "p1179.1.2.fits", "p1179.2.1.fits", "p1179.2.2.fits",
        "p1237.1.1.fits", "p1237.1.2.fits", "p1237.2.1.fits", "p1237.2.2.fits",
        "p1351.1.1.fits_Chr", "p1351.1.2.fits_Chr", "p1351.2.1.fits_Chr", "p1351.2.2.fits_Chr",
        "pMTFcmyc.1.1.fits", "pMTFcmyc.1.2.fits", "pMTFcmyc.2.1.fits", "pMTFcmyc.2.2.fits",
        "trh2.1.1.fits", "trh2.1.2.fits", "trh2.2.1.fits", "trh2.2.2.fits",
        "trh3.1.1.fits", "trh3.1.2.fits", "trh3.2.1.fits", "trh3.2.2.fits",
        "trh4.1.1.fits", "trh4.1.2.fits", "trh4.2.1.fits",
        "trh6.1.1.fits_Chr", "trh6.1.2.fits_Chr", "trh6.2.1.fits_Chr", "trh6.2.2.fits_Chr",
        "trh7.1.1.fits", "trh7.1.2.fits", "trh7.2.2.fits")

    # overlay peaks on top of data tracks
    overlays <- c(  rep('asnCcmyc',  3),
                    rep('p1179cmyc', 4),
                    rep('p1237cmyc', 4),
                    rep('p1351cmyc', 4),
                    rep('pMTFcmyc',  4),
                    rep('trh2cmyc',  4),
                    rep('trh3cmyc',  4),
                    rep('ptrh4cmyc', 3),
                    rep('trh6cmyc',  4),
                    rep('trh7cmyc',  3)  )

    for (i in seq_along(fits)) {
        # convert to data.frame w/ columns sequence, strand, position, value, p.value
        df <- medichiPeaks.deconv.entire.genome(get(fits[i]))

        # hacks to fix sequence names
        df <- df[ df$sequence != 'NC', ]
        df$sequence <- gsub('HALCHR', 'chromosome', df$sequence)

        # define some attributes for the new track
        attr$source <- paste(fits[i], "MeDiChI")
        attr$viewer <- 'Triangle marker'
        attr$visible <- TRUE
        attr$color <- "0x90CC0000"
        attr$overlay=overlays[i]

        # broadcast track to GGB
        cat("importing fits: ", fits[i], "\n" )
        addTrack(ds, df, name=fits[i], attributes=attr, auto.confirm=TRUE)
        cat("finished importing fits: ", fits[i], "\n" )
        Sys.sleep(3)
    }
}


# made for importing fits from 
# \\Isb-2.systemsbiology.net\hm\FangYin\Lrp_ChIPChip\2011-02-21_fitslist.each.RData
#
# parameters
# ----------
# fitslist: a list of medichi chip.deconv.entire.genome objects
# ds: a data source description from GGB
# attr: a list of attributes for the new tracks
#
# example
# -------
# gaggleInit()
# # broadcast dataset description from genome browser
# ds <- getDatasetDescription()
# load('~/Documents/work/projects/FYL-p1351/2011-02-21_fitslist.each.RData')
# import.fits.2(fitslist.each, ds)
import.fits.2 <- function(fitslist, ds, attr=list()) {
    fits <- c(  "asnC_1.1", "asnC_2.1", "asnC_2.2",
                "p1179_1.1", "p1179_1.2", "p1179_2.1", "p1179_2.2",
                "p1237_1.1", "p1237_1.2", "p1237_2.1", "p1237_2.2",
                "pMTF_1.1", "pMTF_1.2", "pMTF_2.1", "pMTF_2.2",
                "trh2_1.1", "trh2_1.2", "trh2_2.1", "trh2_2.2",
                #"trh3_1.1", "trh3_2.1", "trh3_2.2",
                "trh3.1.1_Chr", "trh3.2.2_Chr",
                "trh4_1.1", "trh4_1.2", "trh4_2.1",
                #"trh6_1.1", "trh6_1.2", "trh6_2.1", "trh6_2.2",
                "trh6.1.1_Chr", "trh6.1.2_Chr", "trh6.2.1_Chr", "trh6.2.2_Chr",
                "trh7_1.1", "trh7_1.2", "trh7_2.2")

    # overlay peaks on top of data tracks (parallel to fits)
    overlays <- c(  rep('asnCcmyc_fits',  3),
                    rep('p1179cmyc_fits', 4),
                    rep('p1237cmyc_fits', 4),
                    rep('pMTFcmyc_fits',  4),
                    rep('trh2cmyc_fits',  4),
                    rep('trh3cmyc_fits',  2),
                    rep('ptrh4cmyc_fits', 3),
                    rep('trh6cmyc_fits',  4),
                    rep('trh7cmyc_fits',  3)  )

    for (i in seq_along(fits)) {
        # convert to data.frame w/ columns sequence, strand, position, value, p.value
        df <- medichiPeaks.deconv.entire.genome(fitslist[[fits[i]]])

        # hacks to fix sequence names
        df <- df[ df$sequence != 'NC', ]
        df$sequence <- gsub('HALCHR', 'chromosome', df$sequence)

        # define some attributes for the new track
        attr$source <- paste(fits[i], "MeDiChI 2011-02-21")
        attr$viewer <- 'Triangle p-value marker'
        attr$p.value.cutoff <- 0.1
        attr$rangeMin <- 0
        attr$rangeMax <- 20
        attr$visible <- TRUE
        attr$color <- "0x90CC0000"
        attr$overlay <- overlays[i]
        attr$groups <- 'peaks_2011-02-21'
        attr$height <- 0.03

        # broadcast track to GGB
        cat("importing fits: ", fits[i], "\n" )
        addTrack(ds, df, name=fits[i], attributes=attr, auto.confirm=TRUE)
        cat("finished importing fits: ", fits[i], "\n" )
        Sys.sleep(3)
    }
}

