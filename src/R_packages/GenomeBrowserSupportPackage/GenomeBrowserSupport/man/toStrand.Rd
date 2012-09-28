\name{toStrand}
\alias{toStrand}
\alias{toOppositeStrand}
\title{Work with strand data}

\description{
  Convert a character vector of forward strands ("forward", "for", "+") and
  reverse strands ("reverse", "rev", "-") to the standard for expected by
  GGB (either "-" or "+"). \strong{toOppositeStrand} reverses the strandedness
  in the process.
}

\usage{
  toStrand(strands)
  toOppositeStrand(strands)
}

\arguments{
  \item{strands}{A character vector of strand specifiers.}
}

\value{
  Returns a character vector of "+" and "-".
}

\references{
  \url{http://gaggle.systemsbiology.net/docs/geese/genomebrowser/}
}

\seealso{\code{\link{GenomeBrowserSupport-package}}}

\examples{
# create a vector of strands
strands <- c('FORWARD', 'REVERSE', 'For', 'rev', 'ReVeRsE')

# convert to standard form
toStrand(strands)
# [1] "+" "-" "+" "-" "-"

# reverse strandedness
toOppositeStrand(strands)
# [1] "-" "+" "-" "+" "+"
}

\keyword{ track }
\keyword{ gaggle }
\keyword{ genome browser }
