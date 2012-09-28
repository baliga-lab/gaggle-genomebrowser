filename = ARGV[0]

GENE        = 0
GENE_NAME   = 1
MOLECULE    = 2
GENE_START  = 3
GENE_END    = 4
STRAND      = 5
PROBE_START = 6
PROBE_END   = 7


File.open(filename, "r") do |file|
  while line = file.gets
    fields = line.strip.split("\t")

    gene_start = fields[GENE_START].to_i()
    gene_end   = fields[GENE_END].to_i()
    probe_start = fields[PROBE_START].to_i()
    probe_end = fields[PROBE_END].to_i()

    if (gene_start > gene_end)
      gene_start, gene_end = gene_end, gene_start
    end

    if (probe_start > probe_end)
      probe_start, probe_end = probe_end, probe_start
    end

    if (probe_start < gene_start or probe_start > gene_end or probe_end < gene_start or probe_end > gene_end)
      puts fields[0]
    end

  end
end

