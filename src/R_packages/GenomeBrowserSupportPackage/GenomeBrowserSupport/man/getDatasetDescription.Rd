\name{getDatasetDescription}
\alias{getDatasetDescription}
\alias{getFilename}
\alias{getSequenceNames}
\alias{getTrackNames}
\alias{getTrackInfo}
\alias{getTrackData}
\alias{getTrackDataAsMatrix}
\title{Working with Gaggle Genome Browser Datasets}

\description{
  Functions for querying GGB datasets. Datasets are made up of a set
  of sequences which define a coordinate system and tracks which are
  data to be plotted on those coordinates.
}

\usage{
  ds <- getDatasetDescription()
  sqlite.filename <- getFilename(source)
  sequences <- getSequenceNames(source)
  track.names <- getTrackNames(source)
  df.track.info <- getTrackInfo(source)
  getTrackData <- function(source, name=NULL, uuid=NULL)
}

\arguments{
  \item{source}{A dataset description (\emph{ggb.dataset.description}) object or the filename of a SQLite database.}
  \item{name}{Name of a track. For best results, name should be unique.}
  \item{uuid}{Unique identifier of a track. Can be found by getTrackInfo().}
}

\details{
A dataset description object is a nested list data structure containing
metadata about a GGB dataset. It must be broadcast to R from the Genome
Browser. Once that's done, we can get more information about sequences
and tracks that are part of the dataset. We can also retrieve track
data into R for further processing and create tracks in R.

Note that \strong{getTrackData} works only for tracks of type
\emph{quantitative.segment} or \emph{quantitative.positional}.
}

\value{
\strong{getFilename} returns the filename of the SQLite database used
to store the dataset. It can be manipulated directly using RSQLite.

\strong{getTrackInfo} returns a data.frame with columns (uuid,
name, type, table_name) reflecting the contents of the \emph{tracks}
table in the DB.

\strong{getTrackData} returns a data.frame with columns (sequence,
strand, start, end, value) for segment tracks or (sequence, strand,
position, value) for positional tracks.
}

\references{
  \url{http://gaggle.systemsbiology.net/docs/geese/genomebrowser/}
}

\seealso{
  \code{\link{GenomeBrowserSupport-package}}, 
  \code{\link{setAttributes}}, 
  \code{\link{addTrack}}
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
# [1] "chr"

# what tracks do we have?
getTrackNames(ds)
# [1] "Genes" "waves"

# get details about tracks
getTrackInfo(ds)
#                                   uuid  name                 type     table_name
# 1 7df8bc40-3b0a-4b53-a2de-dee319db11e4 Genes                 gene features_genes
# 2 cf138222-0a66-499a-8edf-078652f3ce84 waves quantitative.segment features_waves

df.wave.track <- getTrackData(ds, name='waves')
head(df.wave.track)
#   name strand start end        value
# 1  chr      +     1 100 0.0009999998
# 2  chr      +   101 200 0.1008283707
# 3  chr      +   201 300 0.1996492979
# 4  chr      +   301 400 0.2964753952
# 5  chr      +   401 500 0.3903392084
# 6  chr      +   501 600 0.4803028813
}

\keyword{ track }
\keyword{ gaggle }
\keyword{ genome browser }
