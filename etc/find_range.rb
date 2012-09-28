filename = ARGV[0]
col = ARGV[1].to_i()

File.open(filename, "r") do |file|

  # first line holds column headers
#  line = file.gets
#  column_headers = line.strip.split("\t")
  min = 1000000
  max = -1000000

  # for each line in file
  while line = file.gets
    fields = line.strip.split("\t")
    
    val = fields[col].to_f()
    min = [min, val].min
    max = [max, val].max
  end
  
  puts "min=#{min} max=#{max}"
end

