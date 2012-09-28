import re

kvpre = re.compile(r"([\w\d_\-\.]+)=(?:(?:" "\"(.*?)\"" ")|(" r"[^\s=]+" "))")

chrom = ""
filename='data/halo.gc.txt'
for line in file(filename, 'r'):
	line = line.strip()
	if (line.startswith('track')):
		print "#" + line
	elif (line.startswith('#')):
		print line
	elif (line.startswith('variableStep')):
		props = {}
		for m in kvpre.finditer(line[12:]):
			key = m.group(1)
			if m.group(2):
				value = m.group(2)
			else:
				value = m.group(3)
			props[key] = value
		print "#" + line
		chrom = props["chrom"]
	else:
		print "%s\t.\t%s" % (chrom, line)
