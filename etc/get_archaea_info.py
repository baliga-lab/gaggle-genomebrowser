#
# Take the list of prokaryotic organisms from the UCSC Archaeal
# genome browser and try to find NCBI taxids
#

import urllib2
import urllib
from BeautifulSoup import *
from urlparse import urljoin
import re

resources = "/Users/cbare/Documents/work/isb/eclipse-workspace-gb/GenomeBrowser/src/resources"
filename = "ucsc.prokaryotic.genomes.tsv"

lines=[line for line in file(resources + "/" + filename)]

for line in lines:
  fields = line.strip().split("\t")
  if (len(fields) < 3):
    continue
  url = "http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?mode=Undef&srchmode=1&filter=genome_filter&%s" % (urllib.urlencode({'name':fields[1]}))

  try:
    c=urllib2.urlopen(url)
  except:
    print "Could not open %s" % url
    continue

  try:
    soup=BeautifulSoup(c.read())
  except:
    print "Could not parse page %s" % url

  taxid = []

  string = str(soup)

  r = re.compile('No result found in the Taxonomy database for complete name')
  m = r.search(string)
  if (not m):

    # look for Taxonomy ID:
    r = re.compile('<em>Taxonomy ID: </em>(\d+)')
    m = r.search(string)
    if (m):
      taxid.append(m.group(1))

    # scrape taxids from hrefs
    hrefs = [li.a['href'] for li in soup.findAll('li')]
    r = re.compile('&id=(\d+)')
    for href in hrefs:
      m = r.search(href)
      if (m):
        taxid.append(m.group(1))

  print "%s\t%s\t%s\t%s" % (fields[0], fields[1], fields[2], ",".join(taxid))
