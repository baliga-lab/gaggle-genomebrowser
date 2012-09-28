filename = ARGV[0]

File.open(filename, "r") do |file|
  line = file.gets
  while line = file.gets
    fields = line.strip.split("\t")
    s = fields[0]
    e = fields[1]
    v = Float(fields[2])
    i = (v * 50).to_int()

    puts "chr\ttiling\tts\t#{s}\t#{e}\t#{i}\t+\t."
  end
end

