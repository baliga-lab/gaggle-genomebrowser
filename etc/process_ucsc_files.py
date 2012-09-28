# functions used from the interactive python console to twiddle text files
# from the UCSC genome browser into shape

# columns:
# name
# description
# commonName
# scientificName
# domain
# clade
# taxid

import re




def archs(lines, out_filename):
	# ditch first line of column headers
	lines = lines[1:]

	# split the clade field into domain and clade
	out = file(out_filename, "w")
	for line in lines:
		fields = line.strip().split("\t")
		domain,clade = fields[3].split("-")
		# output line= dbName, description, commonName, scientificName, domain, clade, taxid
		out.write("\t".join( (fields[0],"",fields[1],fields[2],domain,clade,fields[4]) ))
		out.write("\n")
	out.flush()
	out.close()


def euks(lines, out_filename):
	out = file(out_filename, "a")
	for line in lines:
		fields = line.strip().split("\t")
		# output line= dbName, description, commonName, scientificName, domain, clade, taxid
		out.write("\t".join((fields[0], fields[1], fields[2], fields[3], "eukaryota", fields[5], fields[4])))
		out.write("\n")
	out.flush()
	out.close()

def toNum(db):
	if db==None: return None
	pattern = re.compile('.*\D(\d+)')
	m = pattern.match(db)
	if (m):
		return int(m.group(1))
	else:
		return 0

def filter_only_most_recent(lines):
	dbs = {}
	for line in lines:
		fields = line.strip().split("\t")
		num = toNum(fields[0])
		old = toNum(dbs.get(fields[4]))
		if (old==None or old < num):
			dbs[fields[4]] = fields[0]

	filtered_lines = []
	for line in lines:
		fields = line.strip().split("\t")
		if (fields[0] in dbs.values()):
			filtered_lines.append(line)

	return filtered_lines

