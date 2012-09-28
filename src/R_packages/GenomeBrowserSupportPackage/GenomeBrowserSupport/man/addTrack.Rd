\name{addTrack}
\alias{addTrack}
\title{Adding tracks to the Genome Browser}

\description{
Create a new track in the Genome Browser from data in an R data structure.
}

\usage{
addTrack(source, trackData, name = NULL, type = NULL, table.name = "temp", attributes = NULL)
}

\arguments{
  \item{source}{A dataset description (\emph{ggb.dataset.description}) object or the filename of a SQLite database.}
  \item{trackData}{A data.frame relating data values to coordinates on the genome, whose columns correspond to the track \emph{type}.}
  \item{name}{Specify the track's name}
  \item{type}{One of \emph{quantitative.positional}, \emph{quantitative.segment}, \emph{quantitative.segment.matrix},
    or \emph{gene}. Other track types may be supported in the future.}
  \item{table.name}{Optional, name for the new track in the Genome Browser.}
  \item{attributes}{Optional, attributes to apply to the track. These can be visual properties or a reference to the source of the data.}
}

\details{
Track data must be a data.frame in one of the prescribed formats below. Other data formats may be supported in the future.

\itemize{
  \item{\strong{quantitative.positional}}{(sequence, strand, position, value)}
  \item{\strong{quantitative.segment}}{(sequence, strand, start, end, value)}
  \item{\strong{quantitative.segment.matrix}}{(sequence, strand, start, end, value0, value1, ... value\emph{n})}
  \item{\strong{gene}}{(sequence, strand, start, end, name, common_name, gene_type)}
}

Adding a track is a two step process. First, we dump data into a temp table in the RSQLite
database specified in the dataset description. Then, we broadcast a small tuple object to
the genome browser, which tells GGB to process the temp table into a track. Specifically,
the track data is copied into a track table sorted by sequence, strand, start, and end.
}

\references{
  \url{http://gaggle.systemsbiology.net/docs/geese/genomebrowser/}
}

\seealso{
  \code{\link{GenomeBrowserSupport-package}}, 
  \code{\link{getDatasetDescription}}
}

\examples{
# connect R to the Gaggle
gaggleInit()

# The genome browser should be running, connected to the Gaggle
# and have a dataset loaded. In the genome browser, broadcast a
# dataset description object to R and receive it like this:
ds <- getDatasetDescription()

# query the dataset description for names of sequences, for example 'chr'
getSequenceNames(ds)

# get the length of the sequence 'chr'
len <- ds$sequences$chr$length

# create some bogus data
starts <- seq(1,len,100)
track.fwd <- data.frame(sequence='chr', strand='+', start=starts, end=starts+99, value=sin(starts/1000.0))
track.rev <- data.frame(sequence='chr', strand='-', start=starts+49, end=starts+148, value=sin(starts/900.0))
track <- rbind(track.fwd, track.rev)

# attributes for track
attr <- list(color='0x804B0082',source='Finklestein, et al. 2009', top=0.20, height=0.15, viewer='Scaling', group='bogus data')

# finally, add the track to the genome browser
addTrack(ds, track, name='waves', attributes=attr)

# a dialog should appear in the genome browser. Click OK and the new track should appear.
}

\keyword{ track }
\keyword{ gaggle }
