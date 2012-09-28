\name{broadcastPeaks}
\alias{broadcastPeaks}
\alias{broadcastProfile}
\title{Broadcast MeDiChI peaks and profiles}

\description{
Broadcast MeDiChI peaks and profiles as a tracks to the genome browser.
}

\usage{
  broadcastPeaks(fit, name="MeDiChI peaks", sequence=NULL)
  broadcastProfile(fit, kernel=NULL, name="MeDiChI profile", sequence=NULL)
}

\arguments{
  \item{fit}{A MeDiChI fit object.}
  \item{name}{Name of new track.}
  \item{sequence}{Name of target chromosome, plasmid or replicon.}
  \item{kernel}{Defaults to the kernel stored in the fit object.}
}

\details{
  MeDiChI is an algorithm for predicting likely protein-DNA binding
  sites from ChIP-chip data.

  MeDiChI produces a \emph{fit} object as output. These two functions
  create genome browser tracks for the profile (a best-fit of the
  model to the data) and peaks, which represent predicted binding
  sites.
}

\references{
  \url{http://gaggle.systemsbiology.net/docs/geese/genomebrowser/},
  
  \url{http://baliga.systemsbiology.net/drupal/content/medichi}
}

\seealso{\code{\link{GenomeBrowserSupport-package}}}

\examples{
# connect R to the Gaggle
gaggleInit()

# The genome browser should be running, connected to the Gaggle
# and have a dataset loaded. In the genome browser, broadcast a
# dataset description object to R and receive it like this:
ds <- getDatasetDescription()

# get track names in the dataset
getTrackNames(ds)

# retrieve data for a track
track = getTrackDataAsMatrix(ds, 'ChIP-chip TFBd 500bp')

# call the MeDiChI chip.deconv function on a region of the chromosome
fit <- chip.deconv(track, where="chromosome",
       fit.res=10, center=978000, wind=20000, max.steps=100, n.boot=10, 
       kernel=kernel.halo.lowres, verbose=TRUE)

# create a profile track
broadcastProfile(fit, kernel.halo.lowres, name="MeDiChI profile 500bp")

# create a peaks track
broadcastPeaks(fit, name="MeDiChI peaks 500bp")

# In the genome browser, adjust the visual properties of the new tracks
}

\keyword{ track }
\keyword{ gaggle }
\keyword{ genome browser }
\keyword{ medichi }
\keyword{ MeDiChI }
