\name{asGaggleMatrix}
\alias{asGaggleMatrix}
\title{Convert matrix to Gaggle format}

\description{
Take a 2 column matrix of location and intensity values
and build a 1 column matrix of intensities where the row
names are of the form [sequence]:[position].
Sequence can either be passed in, or read from the current
rownames of the matrix.
}

\usage{
asGaggleMatrix(m, sequence='chr')
}

\arguments{
  \item{profile}{a 2 column matrix of location and intensity values.}
  \item{sequence}{the name of a chromosome, plasmid, replicon, etc.}
}

\value{
A matrix format that can be broadcast to Gaggle. Position will be encoded
in row names.
}

\references{
  \url{http://gaggle.systemsbiology.net/docs/geese/genomebrowser/}
}

\seealso{\code{\link{GenomeBrowserSupport-package}}}

\examples{
# input is a matrix with position in the 1st column and
# a measurement at that position in the 2nd column
m <- matrix(c(200,300,400,0.123,0.222,0.345), nrow=3, ncol=2)
colnames(m) <- c('position', 'intensity')
rownames(m) <- c(1,2,3)

m
#   position intensity
# 1      200     0.123
# 2      300     0.222
# 3      400     0.345

asGaggleMatrix(m, sequence='chr')

#         value
# chr:200 0.123
# chr:300 0.222
# chr:400 0.345
}

\keyword{ track }
\keyword{ gaggle }
\keyword{ genome browser }
