filename = ARGV[0]

File.open(filename, "r") do |file|

  # first line holds column headers
  line = file.gets
  column_headers = line.strip.split("\t")

  # for each line in file
  while line = file.gets
    fields = line.strip.split("\t")
    
    
  end
end

