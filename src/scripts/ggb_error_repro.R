# This reproduces an error:
# Can't convert a null to a double
#   at org.systemsbiology.util.Attributes.getDouble(Attributes.java:98)
#   at org.systemsbiology.genomebrowser.sqlite.SqliteTrackBuilder.applyOverlay(SqliteTrackBuilder.java:199)
#
# The cause was an error in logic where we propogate the attributes RangeMin and RangeMax to
# overlayed tracks. Fixed by changing || to &&.

# source("http://gaggle.systemsbiology.net/R/genome.browser.support.R")
# gaggleInit()
# ds <- getDatasetDescription()

data.1 <- data.frame(sequence='chromosome', strand='.', position=seq(1,20000,10), value=rnorm(2000))
data.2 <- data.frame(sequence='chromosome', strand='.', position=seq(1,20000,10), value=rnorm(2000))

my.attributes.1 <- list(overlay='my.overlay',color='#ff0000',groups='my.group',viewer='VerticalBar',visible=TRUE)
my.attributes.2 <- list(overlay='my.overlay',color='#0000ff',groups='my.group',viewer='VerticalBar',visible=TRUE)

addTrack(ds, data.1, name='data.1', attributes=my.attributes.1, auto.confirm=TRUE)
addTrack(ds, data.2, name='data.2', attributes=my.attributes.2, auto.confirm=TRUE)
