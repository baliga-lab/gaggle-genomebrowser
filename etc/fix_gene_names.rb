filename = ARGV[0]

File.open(filename, "r") do |file|
  while line = file.gets
    fields = line.strip.split("\t")

    if (fields[6] == fields[7])
      fields[1] = ""
    end
    puts fields.join("\t")
  end
end

