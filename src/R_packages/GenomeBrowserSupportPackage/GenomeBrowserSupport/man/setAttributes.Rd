\name{setAttributes}
\alias{setAttributes}
\title{Set attributes of a Gaggle Genome Browser object}

\description{
  Sets attributes (key/value pairs) on Gaggle Genome Browser tracks, sequences or datasets.
}

\usage{
  setAttributes(attributes, uuid=NULL, track.name=NULL)
}

\arguments{
  \item{attributes}{A list of key/value pairs}
  \item{uuid}{}
  \item{track.name}{Name of track}
}

\details{
  Only works on tracks, for now.
}

\references{
  \url{http://gaggle.systemsbiology.net/docs/geese/genomebrowser/}
}

\seealso{\code{\link{GenomeBrowserSupport-package}}}

\examples{

# attributes for track
attr <- list(color='0x804B0082',source='Finklestein, et al. 2009', top=0.20, height=0.15, viewer='Scaling', group='bogus data')

# apply these attributes to the waves track
setAttributes(attr, track.name='waves')
}

\keyword{ track }
\keyword{ gaggle }
\keyword{ genome browser }
