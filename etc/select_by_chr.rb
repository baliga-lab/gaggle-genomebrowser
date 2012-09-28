filename = ARGV[0]

File.open(filename, "r") do |file|

  # first line holds column headers
  line = file.gets
  column_headers = line.strip.split("\t")
  
  puts column_headers.join("\t")

  # for each line in file
  while line = file.gets
    fields = line.strip.split("\t")

    # if the chromosome matches the command line arg, output the line
    chr = fields[2]
    strand = fields[5]
    if (chr == ARGV[1] and strand == ARGV[2])
      puts fields.join("\t")
    end
  end
end

