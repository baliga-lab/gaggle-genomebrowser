ffn <- function(dir, fn) {
	paste(dir, fn, sep="/")
}

nfn <- function(dir, fn) {
	fields = strsplit(fn, split="_")[[1]]
	od = fields[3]
	replica = fields[5]
	strand = fields[6]
	n = paste("segmentation", "log10ratio", od, replica, strand, sep="_")
	paste(dir, n, sep="/")
}

shift_segments_to_probe_centers <- function (dir, filenames, shift) {
	for (filename in filenames) {
		print(paste("processing file", filename))
		a = read.table(ffn(dir, filename), sep="\t", header=T)
		b = data.frame(START=(a[3]+shift),END=(a[4]+shift),log_ratio=a[6],p.value=a[7])
		write.table(b, file=nfn(dir, filename), sep="\t", quote=F, col.names=T, row.names=F)
	}
}
