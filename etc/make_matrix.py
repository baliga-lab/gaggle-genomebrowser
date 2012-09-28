import math

lines = [line.strip() for line in file("../data/halo.genes")]
lines = lines[1:]
theta = 0.0

def compare_lines(line1, line2):
	c1 = cmp(line1.split("\t")[2], line2.split("\t")[2])
	if (c1==0):
		return cmp(int(line1.split("\t")[4]), int(line2.split("\t")[4]))
	else:
		return c1

lines.sort(compare_lines)


for line in lines:
	fields = line.split("\t")
	start = min(int(fields[4]), int(fields[5]))
	end	= max(int(fields[4]), int(fields[5]))

	if fields[2]=='chr':
		seq = "chromosome"
	elif fields[2]=='plasmid_pNRC200':
		seq = "pNRC200"
	elif fields[2]=='plasmid_pNRC100':
		seq = "pNRC100"

	strand = fields[3]

	out_fields = [seq, strand]

	if fields[3]=="+":
		out_fields.append(start)
		out_fields.append(end)
	else:
		out_fields.append(end)
		out_fields.append(start)

	theta += 0.1
	for i in range(0,4):
		out_fields.append(math.sin(theta + i/4.0))

	print "%s%s:%d-%d\t%f\t%f\t%f\t%f" % tuple(out_fields)

