### Support functions for working with MeDiChI and the genome browser
### by: Christopher Bare <cbare@systemsbiology.org>
### July, 2009

# requires an up-to-date R Goose.

# TODO use warning for proper error messages


.onLoad <- function(libname, pkgname) {
	cat("\n\n-- genome.browser.support --\n\n")
	libname = gsub(" ", "%20", libname)
	cat (paste ('\nonLoad -- libname:', libname, 'pkgname:', pkgname, '\n'))
	checkDependencies()
}

checkDependencies <- function() {
	if (require("RSQLite")) {
		cat("RSQLite ok\n")
	} else {
		cat("-- missing RSQLite package\n")
	}

	if (require("gaggle")) {
		cat("gaggle ok\n")
	} else {
		cat("-- missing gaggle package\n")
	}

	if (require("MeDiChI")) {
		cat("MeDiChI ok\n")
	} else {
		cat("----------------------------------------------------------------------\n")
		cat("Warning: MeDiChI package not installed. Some functions will not work.\n")
		cat("To install MeDiChI, follow the instructions here:\n")
		cat("http://baliga.systemsbiology.net/medichi/\n")
		cat("----------------------------------------------------------------------\n\n")
	}

}

checkAndInstallDependencies <- function() {
	# Attempt to load and install, if necessary, RSQLite, gaggle and MeDiChI libraries.
	if (require("RSQLite")) {
		cat("RSQLite ok\n")
	} else {
		cat("-- installing RSQLite\n")
		install.packages("RSQLite")
		library(RSQLite)
	}

	if (require("gaggle")) {
		cat("gaggle ok\n")
	} else {
		cat("-- installing gaggle package from bioconductor\n")
		source("http://www.bioconductor.org/biocLite.R")
		biocLite()
		biocLite('gaggle')
		library(gaggle)
	}

	if (require("MeDiChI")) {
		cat("MeDiChI ok\n")
	} else {
		# automatically installing this requires that we know what platform
		# we're on and that we can download and run 
		# http://baliga.systemsbiology.net/medichi/MeDiChI_0.2.8.tgz
		# library(MeDiChI)
		cat("----------------------------------------------------------------------\n")
		cat("Warning: MeDiChI package not installed. Some functions will not work.\n")
		cat("To install MeDiChI, follow the instructions here:\n")
		cat("http://baliga.systemsbiology.net/medichi/\n")
		cat("----------------------------------------------------------------------\n\n")
	}
}


fetchHaloKernels <- function() {
	# load the halo high and low resolution kernels:
	# kernel.halo.hires and kernel.halo.lowres
	halo.kernel.url = "http://gaggle.systemsbiology.net/docs/geese/genomebrowser/r/halo.kernels.rda"
	tryCatch({
		conn = url(halo.kernel.url)
		load(conn, .GlobalEnv)	
	},
	error=function(e) {
		cat("error accessing ", halo.kernel.url, ":\n", conditionMessage(e), "\n")
	},
	finally={
		if (conn) {
			close(conn)
		}
	})
}






# Create a medichi style matrix from a data.frame
# Expects a data.frame with columns "name", "position", and "value" or
# "start" and "end" in place of "position". Returns a matrix with sequence
# name in the row names, position on the chromosome in the first column
# and value in the second column.
toMedichiMatrix <- function(trackData, strand=NULL) {
	# TODO implement for segment tracks
	if (is.null(strand) || strand=='*') {
		df = trackData
	}
	else {
		df = trackData[trackData$strand==strand, ]
	}
	if ('position' %in% colnames(trackData)) {
		m = matrix( c(df$position, df$value), ncol=2)
		colnames(m) = c("position", "value")
		rownames(m) = df$name
		return(m)
	}
	else if (all(c('start', 'end') %in% colnames(trackData))) {
		m = matrix( c((df$start + df$end)/2, df$value), ncol=2)
		colnames(m) = c("position", "value")
		rownames(m) = df$name
		return(m)
	}
}


# Get data from a genome browser .hbgb file and convert it to a matrix whose
# row names are the names of sequences (chromosomes) and whose first column
# is a position on the sequence and whose second column is an intensity
# measurement. This type of matrix can serve as the input to MeDiChI.
# The parameter 'source' may either be a filename or a dataset description object.
# depends: RSQLite
getTrackDataAsMatrix <- function(source, trackName) {
	conn = dbConnect(SQLite(), getFilename(source))

	# get table name for track
	sql = paste("select * from tracks where name='", trackName, "';", sep="")
	trackInfo = dbGetQuery(conn, sql)
	table = trackInfo$table_name

	cat("track type = ", trackInfo$type, "\n")

	# TODO optionally convert segments to position or use segments

	# get track data as a data.frame
	if (trackInfo$type=='quantitative.segment') {
	    # convert segment tracks to positional
		sql = paste('select s.name, f.strand, (f.start+f.end)/2 as position, f.value from', table, 'f join sequences s on f.sequences_id=s.id;')
		df = dbGetQuery(conn, sql)
	}
	else if (trackInfo$type=='quantitative.positional') {
		sql = paste('select s.name, f.strand, f.position, f.value from', table, 'f join sequences s on f.sequences_id=s.id;')
		df = dbGetQuery(conn, sql)
	}
	else {
		print('Inappropriate track type:', trackInfo$type)
		return(NULL)
	}

	# construct medichi-style matrix
	m = matrix( c(df$position, df$value), ncol=2)
	colnames(m) = c("position", "value")
	rownames(m) = df$name
	return(m)
}


# Computes the profile of a MeDiChI fit. Takes MeDiChI peaks and a kernel. Output
# is a matrix with position in the first column and intensity in the second.
# peaks: a matrix with position in the first column and intensity in the second
# kernel: a MeDiChI kernel
# by: return every nth row of the resulting profile
# TODO make medichiProfile a generic method
medichiProfile.peaks <- function(peaks, kernel, by=1) {
	# Kernel posns are centered on zero, so adding these ranges together
	# gives a range from the first peak's position - the left half of the kernel
	# to the last peak's position plus the right half of the kernel.
	r <- range(peaks[,1]) + range(kernel[,1])
	start <- as.integer(max(1,r[1]))
	end <- as.integer(r[2])
	nrows <- end-start+1

	cat(sprintf("\ngenerating profile: peaks %d, nrows=%d, start=%d, end=%d\n", nrow(peaks), nrows, start, end))

	# In MeDiChI, the positions of the kernel should be in increments of 1 base pair.
	# If not, we could get into a tricky problem of resampling the kernel to fit into
	# the resulting profile. Instead, we warn if there looks to be a problem.
	if (nrow(kernel) > 1 && any(kernel[2:nrow(kernel),1]-kernel[1:(nrow(kernel)-1),1]!=1)) {
		kernel.res <- unique(kernel[2:nrow(kernel),1]-kernel[1:(nrow(kernel)-1)])
		cat("kernel resolution appears to be: ", kernel.res , "\n")
		cat("error: expecting kernel to be at 1 base-pair resolution.\n")
		return()
	}

	# create a 2-column matrix to hold the results with position in the first column
	# and all zeros in the second column.
	profile <- matrix(c(seq(start, end),seq(from=0,to=0,length.out=nrows)), nrow=nrows, ncol=2)
	#cat("dim(profile) = ", dim(profile), "\n")

	# For each peak, scale it by it's intensity and translate it to its position
	# then add it onto the profile.
	for (i in seq(along=peaks[,1])) {
		position <- peaks[i,1]
		intensity <- peaks[i,2]
		kernel.tmp <- kernel
		kernel.tmp[,1] <- as.integer(kernel.tmp[,1] + position)
		kernel.tmp[,2] <- kernel.tmp[,2] * intensity
		# Trim off portion of kernel that has a negative or zero position (clip to start of sequence).
		# This is a bug for circular sequences which have a peak near the 0 point.
		kernel.tmp <- kernel.tmp[ (kernel.tmp[,1]>=start) & (kernel.tmp[,1]<=end), ]
		indices <- kernel.tmp[,1] - start + 1
		#cat("positions=", range(kernel.tmp[,1]), "\n")
		#cat("indices=", range(indices), "\n")
		profile[indices,2] <- profile[indices,2] + kernel.tmp[,2]
	}

	if (by > 1) {
		return(profile[seq(1, nrow(profile), by=by),])
	}
	return(profile)
}

# creates a profile based on a MeDiChI chip.deconv object
medichiProfile.chip.deconv <- function(fit, by=1) {
	peaks <- coef(fit)
	if (is.null(peaks)) {
		cat("error: coef returned NULL.\n")
		return()
	}
	# m is a matrix with columns position and intensity
	m <- medichiProfile.peaks(peaks, fit[[1]]$kernel, by=by)
	return(data.frame(sequence=fit[[1]]$args$where, strand='.', position=m[,1], value=m[,2]))
}


# create a whole-genome profile based on a MeDiChI chip.deconv.entire.genome object
medichiProfile.deconv.entire.genome <- function(fits, by=1) {

	if (class(fits)!="chip.deconv.entire.genome") {
		cat("error: expected fits to be a \"chip.deconv.entire.genome\"\n")
		return()
	}

	# for each "chip.deconv" object, create a profile
	profiles <- list()
	for (i in seq(along=fits$fits.fin)) {
		fit <- fits$fits.fin[[i]]
		if (class(fit)=="chip.deconv") {
			profiles[[i]] <- medichiProfile.chip.deconv(fit, by=by)
		}
	}

	# return a combined data frame or NULL if length(profiles)==0
	return(do.call("rbind", profiles))
}


medichiFitData.chip.deconv <- function(fit) {
	data <- fit[[1]]$data
	return(data.frame(sequence=rownames(data), strand='.', position=data[,1], value=data[,2]))
}


medichiFitData.deconv.entire.genome <- function(fits) {
	results <- list()
	for (i in seq(along=fits$fits.fin)) {
		fit <- fits$fits.fin[[i]]
		if (class(fit)=="chip.deconv") {
			results[[i]] <- medichiFitData.chip.deconv(fit)
		}
	}

	# return a combined data frame or NULL if length of list is zero
	return(do.call("rbind", results))
}


medichiPeaks.chip.deconv <- function(fit) {
	peaks <- coef(fit)
	return(data.frame(sequence=fit[[1]]$args$where, strand='.', position=peaks[,1], value=peaks[,2], p.value=peaks[,3]))
}


medichiPeaks.deconv.entire.genome <- function(fits) {
	results <- list()
	for (i in seq(along=fits$fits.fin)) {
		fit <- fits$fits.fin[[i]]
		if (class(fit)=="chip.deconv") {
			results[[i]] <- medichiPeaks.chip.deconv(fit)
		}
	}
	# return a combined data frame or NULL if length of list is zero
	return(do.call("rbind", results))
}


# Take a 2 column matrix of location and intensity values
# and build a 1 column matrix of intensities where the row
# names are of the form [sequence]:[position].
# Sequence can either be passed in, or read from the current
# rownames of the matrix.
asGaggleMatrix <- function(profile, sequence=NULL) {
	old.scipen = getOption("scipen")
	options(scipen=999)
	m = matrix(profile[,2])
	colnames(m) = c("value")
	# if sequence is specified, use it, otherwise us
	if (is.null(sequence)) {
		rownames(m) = paste(rownames(profile), round(profile[,1]), sep=":")
	}
	else {
		rownames(m) = paste(sequence, profile[,1], sep=":")
	}
	options(scipen=old.scipen)
	return(m)
}


# Calls medichiProfile to construct a plottable fit, then transforms the
# resulting matrix into the proper format to be broadcast to the genome
# browser.
medichiProfileAsGaggleMatrix <- function(fit, kernel=NULL, sequence=NULL) {
	if (is.null(sequence)) {
		sequence = fit[[1]]$args$where
	}
	if (is.null(kernel)) {
		kernel = fit[[1]]$kernel
	}
	profile = medichiProfile.peaks(coef(fit), kernel)
	m = asGaggleMatrix(profile, sequence)
	return(m)
}

# create a profile from a MeDiChI fit and broadcast it as a Gaggle
# matrix with positions encoded in the row names.
broadcastProfile <- function(fit, kernel=NULL, name="MeDiChI profile", sequence=NULL) {
	profile = medichiProfileAsGaggleMatrix(fit, kernel, sequence)
	broadcast(profile, name)
}

# broadcast peaks from a MeDiChI fit.
broadcastPeaks <- function(fit, name="MeDiChI peaks", sequence=NULL) {
	if (is.null(sequence)) {
		sequence = fit[[1]]$args$where
	}
	broadcast(asGaggleMatrix(coef(fit), sequence), name)
}








# Take a positional data frame and return a segment data frame.
# Assuming that the positions are at the center of the probes, the
# segments start at position - probe_width/2 + 1 and end at
# position + probe_width/2 + 1
positionToSegment <- function(df, probe_width) {
	return(data.frame(sequence=df$sequence, strand=df$strand, start=as.integer(df$position-(probe_width/2)+1), end=as.integer(df$position+(probe_width/2)+1), value=df$value))
}

toStrand <- function(strands) {
	result = as.character(strands)
	temp = tolower(strands)
	result[ temp=='+' | temp=='forward' | temp=='for' | temp=='f'] = '+'
	result[ temp=='-' | temp=='reverse' | temp=='rev' | temp=='r'] = '-'
	return(result)
}

toOppositeStrand <- function(strands) {
	result = as.character(strands)
	temp = tolower(strands)
	result[ temp=='+' | temp=='forward' | temp=='for' | temp=='f'] = '-'
	result[ temp=='-' | temp=='reverse' | temp=='rev' | temp=='r'] = '+'
	return(result)
}

toSequence <- function(seqs) {
	result <- as.character(seqs)
	result[ result=='NC_002607' ] <- 'chromosome'
	result[ result=='NC_002608' ] <- 'pNRC200'
	result[ result=='NC_001869' ] <- 'pNRC100'
	return(result)
}


# if source is a string, assume it's a filename and return it.
# if source is a dataset object, return it's SQLite filename.
getFilename <- function(source) {
	if (is.character(source)) {
		return(source)
	}
	else if (is.list(source)) {
		return(source$filename)
	}
	else {
		cat("unknown data source: ", source, "\n")
		return(source)
	}
}


# Get track data from a genome browser .hbgb file and return it as a data.frame
getTrackData <- function(source, name=NULL, uuid=NULL) {
	conn = dbConnect(SQLite(), getFilename(source))

	# get table name for track
	if (!is.null(uuid)) {
		sql = paste("select * from tracks where uuid='", uuid, "';", sep="")
		trackInfo = dbGetQuery(conn, sql)
		table = trackInfo$table_name
	}
	else if (!is.null(name)) {
		sql = paste("select * from tracks where name='", name, "';", sep="")
		trackInfo = dbGetQuery(conn, sql)
		table = trackInfo$table_name
	}

	if (trackInfo$type=='quantitative.segment') {
		# get track data as a data.frame
		sql = paste('select s.name, f.strand, f.start, f.end, f.value from', table, 'f join sequences s on f.sequences_id=s.id;')
		df = dbGetQuery(conn, sql)
		return(df)
	}
	else if (trackInfo$type=='quantitative.positional') {
		# get track data as a data.frame
		sql = paste('select s.name, f.strand, f.position, f.value from', table, 'f join sequences s on f.sequences_id=s.id;')
		df = dbGetQuery(conn, sql)
		return(df)
	}
	else {
		print('Inappropriate track type:', trackInfo$type)
		return(NULL)
	}
}

# Receives a broadcast of type tuple and converts it to an R nested list
# structure holding details of a genome browser dataset. To see structure,
# broadcast "Description of Dataset" from the genome browser and type:
# > ds = getDataset()
# > str(ds)
getDatasetDescription <- function() {
	dataset = getTupleAsList()
	attributes(dataset)['class'] = 'ggb.dataset.description'
	return(dataset)
}

print.ggb.dataset.description <- function(dataset) {
	cat('dataset: ', dataset$name, '\n')
	cat('..$ uuid', dataset$uuid, '\n')
	cat('..$ filename', dataset$filename, '\n')
	# for (i in seq(along=dataset)) {
	# 	if (typeof(dataset[[i]])!='list' && names(dataset)[i] != 'name') {
	# 		cat('..$ ', names(dataset)[i], ': ', dataset[[i]], '\n')
	# 	}
	# }
	cat(sprintf('..$ sequences: :list of %d\n', length(dataset$sequences)))
	for (i in seq(1, length=min(10, length(dataset$sequences)))) {
		cat(sprintf("..  ..$ %s (%d)\n", names(dataset$sequences)[i], dataset$sequences[[i]]$length))
	}
	cat('..$ tracks: (', length(dataset$tracks) ,')\n')
}


# Queries the given database file for information about tracks. Returns the
# results in a data.frame with the columns (uuid, name, track_type, table_name)
getTrackInfo <- function(source) {
	conn = dbConnect(SQLite(), getFilename(source))
	tracks = dbGetQuery(conn, 'select * from tracks;')
	return(tracks)
}

# Get track names from a dataset
getTrackNames <- function(dataset) {
	if (is.null(dataset)) return(c())
	return(names(dataset$tracks))
}

# get sequence names from a dataset
getSequenceNames <- function(dataset) {
	if (is.null(dataset)) return(c())
	return(names(dataset$sequences))
}


# add a new track from trackData, a data.frame with columns for a
# quantitative.segment track (sequences_name, strand, start, end, value)
# quantitative.positional track (sequences_name, strand, position, value)
# gene track ()
#
# source: DatasetDescription object or filename
# trackData: a data.frame with a format described above
# name: name of new track
# type: a genome browser track type, 'quantitative.segment', 'quantitative.positional', 'gene'
# table.name: SQLite table to use as temporary storage for features (defaults to 'temp')
# attributes: a list of attributes to be assigned to the new track (key/value pairs of primitive types or Strings)
# 
addTrack <- function(source, trackData, name=NULL, type=NULL, table.name="temp", attributes=NULL) {
	# TODO trackData should accept a matrix with row names of the form chr:1234-5678
	if (is.null(type)) {
		# if (all(c('position', 'value', 'p.value') %in% colnames(trackData))) {
		# 	type = "quantitative.positional.p.value"
		# }
		if (all(c('position', 'value') %in% colnames(trackData))) {
			type = "quantitative.positional"
		}
		else if (all(c('start', 'end', 'value') %in% colnames(trackData))) {
			type = 'quantitative.segment'
		}
		else if (all(c('start', 'end', 'name', 'common_name', 'gene_type') %in% colnames(trackData))) {
			type = 'gene'
		}
		else {
			cat("error: unknown track type. Please try again with a 'type' argument.\n")
			return()
		}
	}

	if (type=='quantitative.positional') {
		create.table.sql = paste("create table if not exists", table.name, "(sequences_name text, strand text, position integer, value numeric);")
		ncols.table = 4
	}
	else if (type=='quantitative.positional.p.value') {
		create.table.sql = paste("create table if not exists", table.name, "(sequences_name text, strand text, position integer, value numeric, p_value numeric);")
		ncols.table = 5
	}
	else if (type=='quantitative.segment') {
		create.table.sql = paste("create table if not exists", table.name, "(sequences_name text, strand text, start integer, end integer, value numeric);")
		ncols.table = 5
	}
	else if (type=='quantitative.segment.matrix') {
		num.value.cols = ncol(trackData) - 4
		value.cols = paste('value', 0:(num.value.cols-1), sep='', collapse=' numeric, ')
		create.table.sql = paste("create table if not exists", table.name, "(sequences_name text, strand text, start integer, end integer,", value.cols, ");")
		ncols.table = num.value.cols + 4
		if (is.null(attributes)) {
			attributes = list()
		}
		attributes[['matrix.num.cols']] = num.value.cols
	}
	else if (type=='gene') {
		create.table.sql = paste("create table if not exists", table.name, "(sequences_name text, strand text, start integer, end integer, name text, common_name text, gene_type text);")
		ncols.table = 7
	}
	else {
		cat("error: unrecognized track type \"" + type + "\". Try either \"quantitative.positional\" or \"quantitative.segment\".\n")
	}

	tryCatch({
		conn = dbConnect(SQLite(), getFilename(source))

		if (table.name=='temp') {
			dbSendQuery(conn, "drop table if exists temp;")
		}
		dbSendQuery(conn, create.table.sql)

#		dbWriteTable(conn, table.name, trackData, row.names=FALSE, append=TRUE)

		# write table in chunks of 'n' rows 'cause it's a lot faster
		n = 10000
		for (i in seq(1,nrow(trackData), n)) {
			last = min(nrow(trackData), i+n-1)

			valStr <- paste(rep("?", ncols.table), collapse=",")
			sql <- sprintf("insert into %s values (%s)", table.name, valStr)
			success <- tryCatch({
				## The 'finally' expression will have access to
				## this frame, not that of 'error'.  We want ret
				## to be defined even if an error occurs.
				ret <- FALSE
				dbBeginTransaction(conn)
				rs <- dbSendPreparedQuery(conn, sql, bind.data=trackData[i:last,])
				cat('i=', i, '\n')
				dbCommit(conn)
				ret <- TRUE
			}, error=function(e) {
				dbRollback(conn)
				stop(conditionMessage(e))
 				ret <- FALSE
			}, finally={
				if (exists("rs"))
					dbClearResult(rs)
				ret
			})

		}
		tuple = newTuple('import.track')
		cmd.import.track = list(command='import.track', source='db', track.type=type, table.name=table.name)
		if (!is.null(name)) {
			cmd.import.track$track.name = name
		}
		if (!is.null(attributes)) {
			cmd.import.track$attributes = newTuple('attributes', attributes)
		}
		broadcast(newTuple('import.track', cmd.import.track), 'add track command from R')
	},
	error=function(e) {
		cat("error creating SQLite table ", table.name, ":\n", conditionMessage(e), "\n")
	},
	finally={
		dbDisconnect(conn)
	})
}



# TODO create new dataset
# TODO set species
# TODO set sequences
# TODO set attributes on track, sequence, or dataset
# TODO set specific visual properties

setAttributes <- function(attributes, uuid=NULL, track.name=NULL) {
	command <- list(command='set.track.attributes', uuid=uuid, track.name=track.name, attributes=attributes)
	tuple <- newTuple('set.track.attributes', command)
	broadcast(tuple, 'command')
}

# TODO
deleteTrack <- function(names) {
	
}

# TODO is this necessary for anything?
# TODO support adding view tracks?
.addTrackMetadata <- function(dataset_uuid, track_uuid) {
	dbSendQuery(paste("insert into datasets_tracks values('", dataset_uuid, "', '", track_uuid, "');", sep=""))
}

# wrapper method for generating UUIDs. We just call the system
# command uuidgen, so this is required.
generateUuid <- function() {
	return(tolower(system('uuidgen', intern=TRUE)))
}


# Get a UUID from a web service at http://uuidgen.com
getUuid <- function() {
	uuid.url = "http://uuidgen.com/t"
	tryCatch({
		conn = url("http://uuidgen.com/t")
		return(tolower(readLines(conn, warn=FALSE)))
	},
	error=function(e) {
		cat("error accessing ", uuid.url, ":\n", conditionMessage(e), "\n")
	},
	finally={
		close(conn)
	})
}
