\name{GenomeBrowserSupport}
\docType{package}
\alias{GenomeBrowserSupport-package}
\alias{GenomeBrowserSupport}
\alias{genome.browser.support}
\title{Functions for exchanging data between R and the Gaggle Genome Browser}

\description{
The GenomeBrowserSupport package helps exchange data between R and the Gaggle
Genome Browser. It also contains some functions for working with output from
the MeDiChI algorithm for model-based deconvolution of protein-DNA binding.
}

\details{
Kernels for halobacterium high-density 244k tiling arrays and low-density 500bp
tiling arrays are included. Type \code{data('halo.kernels')} to load two
matrix objects named \dQuote{kernel.halo.hires} and \dQuote{kernel.halo.lowres}.
}

\references{
  \url{http://gaggle.systemsbiology.net/docs/geese/genomebrowser/},
  
  \url{http://baliga.systemsbiology.net/drupal/content/medichi}
}

\seealso{
  \code{\link{addTrack}},
  \code{\link{broadcastPeaks}},
  \code{\link{broadcastProfile}},
  \code{\link{getDatasetDescription}},
  \code{\link{setAttributes}}
}

\author{
  Christopher Bare \email{cbare@systemsbiology.org}
}

\keyword{genome browser}
\keyword{gaggle genome browser}
\keyword{gaggle}
\keyword{track}
