# Process tiling array data for Gaggle Genome Browser (GGB)
# =========================================================
#
# processes tiling array data in the following structures to GGB format
# > ls()
#  [1] "break.densities"       "break.densities.start" "break.densities.stop"
#  [4] "breaks"                "breaks.start"          "breaks.stop"
#  [7] "gene.coords"           "org"                   "probe.probs"
# [10] "probe.spacing"         "probs.expressed"       "rats"
# [13] "rats.cors"             "refs"


# requires genome.browser.support.R



# create a GGB track for average over all reference arrays
# takes a refs table with columns like so:
#  [1] "PROBE_ID"                                                  
#  [2] "GENE_EXPR_OPTION"                                          
#  [3] "SEQ_ID"                                                    
#  [4] "START"                                                     
#  [5] "END"                                                       
#  [6] "POSITION"                                                  
#  [7] "PROBE_LENGTH"                                              
#  [8] "SYMBOL"                                                    
#  [9] "data column 1"
# [10] "data column 2"
# [11] "data column 3"
# ...
# [n+8] "data column n"
#
# source will be assigned as an attribute of the new track
# ds is a dataset description broadcast from GGB. If defined, broadcast the track when complete.
#
# returns the GGB data.frame (sequence, strand, start, end, value)
#
# example: 
# > ds <- getDatasetDescription()
# > df <- compute.reference.average(refs, source='253052110001,2,3,4,12,13,14', ds=ds)
#
compute.reference.average <- function(refs, source=NULL, ds=NULL) {
    # create GGB data frame from refs data frame
    gb.refs = data.frame(
                sequence=toSequence(refs$SEQ_ID),
                strand=toOppositeStrand(refs$GENE_EXPR_OPTION),
                start=refs$START,
                end=refs$END,
                value=apply(refs[,9:ncol(refs)], 1, mean))

    # reverse start and end for probes that span the zero point
    ii <- (gb.refs$end - gb.refs$start) > 1000
    tmp.start <- gb.refs$start[ii]
    gb.refs$start[ii] <- gb.refs$end[ii]
    gb.refs$end[ii] <- tmp.start

    # broadcast the data frame, if ds is defined
    if (!is.null(ds)) {
        # define track attributes
        attr <- list(viewer='Scaling', color='0x80336699')
        if (!is.null(source)) {
            attr$source = source
        }

        # broacast the track to GGB
        addTrack(ds, gb.refs, name='reference average', attributes=attr)
    }

    return(invisible(gb.refs))
}


halo.sequences = list(
    Chr=list(name='Chr', gb.name='chromosome', refseq='NC_002607', length=2014239),
    pNRC200=list(name='pNRC200', gb.name='pNRC200', refseq='NC_002608', length=365425),
    pNRC100=list(name='pNRC100', gb.name='pNRC100', refseq='NC_001869', length=191346))


# Create a GGB data frame for a segmentation track
# Finds the average value over probes whose (central) position is within the segment boundaries. Returns
# a data frame with columns (sequence, strand, start, end, value)
#
# Parameters
# ----------
# df: a data frame in GGB format (sequence, strand, start, end, value)
# breaks: a list of breaks in the following format:
#     List of 3
#      $ Chr    :List of 2
#       ..$ REVERSE: num [1:756] 5562 9939 11385 16110 17759 ...
#       ..$ FORWARD: num [1:757] 5223 9846 11462 12806 20176 ...
#      $ pNRC100:List of 2
#       ..$ REVERSE: num [1:90] 2352 3862 4831 8844 9806 ...
#       ..$ FORWARD: num [1:79] 2039 3882 4628 5587 8667 ...
#      $ pNRC200:List of 2
#       ..$ REVERSE: num [1:138] 2479 3885 4853 8764 9706 ...
#       ..$ FORWARD: num [1:142] 1211 1942 3993 4463 5357 ...
# sequences: list of sequences. Each sequence is a list w/ fields
#              name: name in breaks data structure
#              gb.name: name in genome browser
#              length: length of chromosome or plasmid
# value.column: the column in df whose value we are averaging
# source: assigned as an attribute of the new track
# ds: a dataset description broadcast from GGB. If defined, broadcast track when complete
#
# example:
# > ds <- getDatasetDescription()
# > df <- compute.reference.average(refs, source='253052110001,2,3,4,12,13,14', ds=ds)
# > compute.segments(df, breaks, halo.sequences, source='253052110001,2,3,4,12,13,14', ds=ds)
compute.segments <- function(df, breaks, sequences=halo.sequences, value.column='value', source=NULL, ds=NULL) {
    result = list()

    # check if df is a GGB df's (sequence, strand, start, end, value)
    if (!all(c('sequence', 'start', 'end', value.column) %in% colnames(df))) {
        stop("Error: parameter df has an unrecognized format.")
    }

    # add central.position column to df
    df$central.position = apply(df[,c('start','end')], 1, mean)
    
    for (seq in sequences) {
        cat('name=', seq$name, ', gb.name=',seq$gb.name,', len=',seq$length, '\n')
        
        # Forward strand -- note that strands are reversed in breaks, refs and rats
        # make lists of starts and ends
        starts = c(0,breaks[[seq$name]]$REVERSE)
        ends = c(breaks[[seq$name]]$REVERSE, seq$length)
        cat('len + = ', length(starts), '\n')

        # pull values out and average them for a all probes whose central point is inside the segment
        values = c()
        for (i in 1:(length(starts))) {
            values[i] = mean(
                subset(df,
                    sequence==seq$gb.name & central.position>=starts[i] & central.position<ends[i] & strand=='+',
                    select=value.column))
        }
        result[[length(result)+1]] = data.frame(sequence=seq$name, strand='+', start=starts, end=ends, value=values)

        # Reverse strand -- note that strands are reversed in breaks, refs and rats
        # make lists of starts and ends
        starts = c(0,breaks[[seq$name]]$FORWARD)
        ends = c(breaks[[seq$name]]$FORWARD, seq$length)
        cat('len - = ', length(starts), '\n')

        # pull values out and average them for a all probes whose central point is inside the segment
        values = c()
        position = apply(df[,c('start','end')], 1, mean)
        for (i in 1:(length(starts))) {
            values[i] = mean(
                subset(df,
                    sequence==seq$gb.name & central.position>=starts[i] & central.position<ends[i] & strand=='-',
                    select=value.column))
        }
        result[[length(result)+1]] = data.frame(sequence=seq$name, strand='-', start=starts, end=ends, value=values)

    }
    segments <- do.call(rbind, result)

    # broadcast the data frame, if ds is defined
    if (!is.null(ds)) {
        # define track attributes
        attr <- list(viewer='Scaling', color='0x80336699')
        if (!is.null(source)) {
            attr$source = source
        }

        # broacast the track to GGB
        addTrack(ds, segments, name='segmentation', attributes=attr)
    }

    return(invisible(segments))
}


# Given a rats data frame, creates a quantitative.segment.matrix track in the GGB
# example:
# > create.rats.matrix(rats, source='253052110001,2,3,4,12,13,14', ds=ds)
create.rats.matrix <- function(rats, source=NULL, ds=NULL) {
    # check if rats is in the right format
    if (class(rats) != 'data.frame' || !all(c('SEQ_ID', 'GENE_EXPR_OPTION', 'START', 'END') %in% colnames(rats))) {
        stop("rats must be a data.frame with the columns: 'SEQ_ID', 'GENE_EXPR_OPTION', 'START', 'END' plus some value columns.")
    }
    
    # create a data.frame in the GGB format
    gb.rats <- data.frame(
        sequence=toSequence(rats$SEQ_ID),
        strand=toOppositeStrand(rats$GENE_EXPR_OPTION),
        start=rats$START,
        end=rats$END,
        value=rats[,9:ncol(rats)])

    # fix column names
    colnames(gb.rats) <- c('sequence', 'strand', 'start', 'end', paste('value', 1:(ncol(gb.rats)-4), sep=''))

    # fix any probes that span the zero point
    ii <- (gb.rats$end - gb.rats$start) > 1000
    tmp.start <- gb.rats$start[ii]
    gb.rats$start[ii] <- gb.rats$end[ii]
    gb.rats$end[ii] <- tmp.start

    # broadcast the data frame, if ds is defined
    if (!is.null(ds)) {
        # define track attributes
        attr <- list(viewer='MatrixHeatmap', split.strands='true')
        if (!is.null(source)) {
            attr$source = source
        }

        # broacast the track to GGB
        addTrack(ds, gb.rats, name='ratios matrix', type="quantitative.segment.matrix", attributes=attr)
    }

    return(invisible(gb.rats))
}


# break densities are computed at very high resolution. Here, we down-sample it, so
# it won't take up a ton of space.
# parameters
# ----------
# break.densities: nested list in this format:
#   List of 3
#    $ Chr    :List of 2
#     ..$ FORWARD: num [1:262144, 1:2] 3206 3214 3221 3229 3237 ...
#     ..$ REVERSE: num [1:262144, 1:2] 5410 5417 5425 5433 5440 ...
#    $ pNRC100:List of 2
#     ..$ FORWARD: num [1:262144, 1:2] 1106 1107 1107 1108 1109 ...
#     ..$ REVERSE: num [1:262144, 1:2] 2117 2118 2118 2119 2120 ...
#    $ pNRC200:List of 2
#     ..$ FORWARD: num [1:262144, 1:2] 1034 1036 1037 1039 1040 ...
#     ..$ REVERSE: num [1:262144, 1:2] 2297 2298 2300 2301 2303 ...
# interval: if interval = n, sample break density every n base pairs along each sequence
# sequences: list of sequences. Each sequence is a list w/ fields
#              name: name in break.densities data structure
#              gb.name: name in genome browser
#              length: length of chromosome or plasmid
# example:
# > process.break.densities(break.densities, source='253052110001,2,3,4,12,13,14', ds=ds)
process.break.densities <- function(break.densities, interval=10, sequences=halo.sequences, source=NULL, ds=NULL) {
    parts = list()
    for (seq in sequences) {
        part <- approx(break.densities[[seq$name]]$REVERSE[,1], break.densities[[seq$name]]$REVERSE[,2], xout=seq(1,seq$length,10))
        part <- data.frame(sequence=seq$gb.name, strand='+', position=part$x, value=part$y)
        part <- part[ !is.na(part$value), ]
        parts[[length(parts) + 1]] <- part

        part <- approx(break.densities[[seq$name]]$FORWARD[,1], break.densities[[seq$name]]$FORWARD[,2], xout=seq(1,seq$length,10))
        part <- data.frame(sequence=seq$gb.name, strand='-', position=part$x, value=part$y)
        part <- part[ !is.na(part$value), ]
        parts[[length(parts) + 1]] <- part
    }
    bd <- do.call(rbind, parts)

    # broadcast the data frame, if ds is defined
    if (!is.null(ds)) {
        # define track attributes
        attr <- list(viewer='Scaling', color='0xbf735a3b', top=0.01, height=0.06, rangeMin=0.0, rangeMax=1.0)
        if (!is.null(source)) {
            attr$source = source
        }

        # broacast the track to GGB
        addTrack(ds, bd, name='break.densities', type="quantitative.positional", attributes=attr)
    }

    return(invisible(bd))
}

# transform probe.probs to a quantitative.positional track.
# parameters
# ----------
# probe.probs has the following structure:
#     List of 3
#      $ Chr    :List of 2
#       ..$ FORWARD: Named num [1:23986] NA NA NA 0.2602 0.0561 ...
#       .. ..- attr(*, "names")= chr [1:23986] "HALCHR_rev_000001" "HALCHR_rev_000002" "HALCHR_rev_000003" "HALCHR_rev_000004" ...
#       ..$ REVERSE: Named num [1:24170] NA NA NA 0.998 0.998 ...
#       .. ..- attr(*, "names")= chr [1:24170] "HALCHR_fwd_023980" "HALCHR_fwd_000001" "HALCHR_fwd_000002" "HALCHR_fwd_000003" ...
#      $ pNRC100:List of 2
#       ..$ FORWARD: Named num [1:2282] NA NA NA 0.553 0.75 ...
#       .. ..- attr(*, "names")= chr [1:2282] "pNRC100_rev_000001" "pNRC100_rev_000002" "pNRC100_rev_000003" "pNRC100_rev_000004" ...
#       ..$ REVERSE: Named num [1:2297] NA NA NA 0.31 0.145 ...
#       .. ..- attr(*, "names")= chr [1:2297] "pNRC100_fwd_000001" "pNRC100_fwd_000002" "pNRC100_fwd_000003" "pNRC100_fwd_000004" ...
#      $ pNRC200:List of 2
#       ..$ FORWARD: Named num [1:4351] NA NA NA 0.507 0.757 ...
#       .. ..- attr(*, "names")= chr [1:4351] "pNRC200_rev_000001" "pNRC200_rev_000002" "pNRC200_rev_000003" "pNRC200_rev_000004" ...
#       ..$ REVERSE: Named num [1:4367] NA NA NA 0.657 0.351 ...
#       .. ..- attr(*, "names")= chr [1:4367] "pNRC200_fwd_004351" "pNRC200_fwd_000001" "pNRC200_fwd_000002" "pNRC200_fwd_000003" ...
# probes is a data frame describing probe locations on the genome. Rows in probe.probes are named
# with probe IDs, as are rows in probes. Probes has sequence, strand, and position information for
# the probes.
# sequences: list of sequences. Each sequence is a list w/ fields
#              name: name in breaks data structure
#              gb.name: name in genome browser
#              length: length of chromosome or plasmid
# source: assigned as an attribute of the new track
# ds: a dataset description broadcast from GGB. If defined, broadcast track when complete
process.probe.probs <- function(probe.probs, probes, sequences=halo.sequences, source=NULL, ds=NULL) {
    parts = list()
    for (seq in sequences) {
        # forward (strands are reversed in probe.probs)
        my.probes <- probes[ names(probe.probs[[seq$name]]$REVERSE), ]
        part <- data.frame(sequence=seq$gb.name, strand='+', position=my.probes$POSITION, value=probe.probs[[seq$name]]$REVERSE)
        part <- part[ !is.na(part$value), ]
        parts[[length(parts) + 1]] <- part

        # reverse (strands are reversed in probe.probs)
        my.probes <- probes[ names(probe.probs[[seq$name]]$FORWARD), ]
        part <- data.frame(sequence=seq$gb.name, strand='-', position=my.probes$POSITION, value=probe.probs[[seq$name]]$FORWARD)
        part <- part[ !is.na(part$value), ]
        parts[[length(parts) + 1]] <- part
    }
    result <- do.call(rbind, parts)

    # broadcast the data frame, if ds is defined
    if (!is.null(ds)) {
        # define track attributes
        attr <- list(viewer='Scaling', color='0xbf735a3b', top=0.01, height=0.06, rangeMin=0.0, rangeMax=1.0)
        if (!is.null(source)) {
            attr$source = source
        }

        # broacast the track to GGB
        addTrack(ds, result, name='probe.probs', type="quantitative.positional", attributes=attr)
    }

    return(invisible(result))
}


# chop large matrix track into 9 sub-matrices by creating views into the features_ratios_matrix table
# purpose-specific to Serdar's tiling array data
create.views.on.ratios.matrix <- function(ds) {
    # specify condition names and value columns from the features_ratios_matrix table
    conditions = list( list(name="Dura3_p0=Temp_d0=25.0C",              cols=c(0,1,2)),
                       list(name="Dura3DtfbD_p0=Temp_d0=25.0C",         cols=c(3,4,5)),
                       list(name="Dura3DtfbD-pST0",                     cols=c(6,7,8)),
                       list(name="Dura3DtfbD-pST0_p0=Temp_d0=25.0C",    cols=c(9,10,11)),
                       list(name="Dura3DtfbD-pST3",                     cols=c(12,13,14)),
                       list(name="Dura3DtfbD-pST3_p0=Temp_d0=25.0C",    cols=c(15,16,17)),
                       list(name="Dura3DtfbD-pST4",                     cols=c(18,19,20)),
                       list(name="Dura3DtfbD-pST4_p0=Temp_d0=25.0C",    cols=c(21,22,23,24)),
                       list(name="Dura3DtfbE_p0=Temp_d0=25.0C",         cols=c(25,26,27)))

    # define a set of attributes we'll assign to the tracks
    attr <- list(viewer='MatrixHeatmap', split.strands='true', rangeMax=4.0, rangeMin=-4.0, top=0.08, height=0.03, source='253052110001,2,3,4,12,13,14')

    # initialize parameters for computing vertical placement of track
    track.spacing = 0.003
    space.per.lane = 0.009
    top.next.track <- attr$top

    conn = dbConnect(SQLite(), getFilename(ds))
    on.exit(function() { dbDisconnect(conn) })
    for (cond in conditions) {

        cat("\n", cond$name, "\n", paste(rep('-', nchar(cond$name)), collapse=''), "\n")

        # create view
        value.cols <- paste('value', cond$cols, ' as value', 0:(length(cond$cols)-1), sep='', collapse=', ')
        view.name <- paste('features_', gsub("[,=\\+\\.\\-]","_",cond$name), sep='')
        sql <- paste('CREATE VIEW', view.name, 'as select rowid, sequences_id, strand, start, end,', value.cols, 'from features_ratios_matrix;')
        cat(sql, '\n')
        dbSendQuery(conn, paste('drop view if exists', view.name, ';'))
        dbSendQuery(conn, sql)

        # generate a UUID for the track (works on OS X and should work on UNIX variants)
        track_uuid = generateUuid()

        # insert entry into tracks table
        sql <- paste("insert into tracks (uuid, name, type, table_name) values ('", 
                    track_uuid, "', '", cond$name, "', 'quantitative.segment.matrix', '", view.name, "');", sep="")
        cat(sql, '\n')
        dbSendQuery(conn, sql)

        # insert entry into datasets_tracks table
        dbSendQuery(conn, paste("insert into datasets_tracks values('", ds$uuid, "', '", track_uuid, "');", sep=""))

        # figure out placement for track 
        # (top and height are expressed as a fraction of window height)
        attr$top <- top.next.track
        attr$hieght <- space.per.lane * length(cond$cols)
        top.next.track <- attr$top + attr$height + track.spacing

        # insert attributes for track
        for (key in names(attr)) {
            sql <- paste("insert into attributes (uuid, key, value) values ('", track_uuid, "', '", key, "', '", attr[[key]], "');", sep="")
            cat(sql, '\n')
            dbSendQuery(conn, sql)
        }
    }
}







