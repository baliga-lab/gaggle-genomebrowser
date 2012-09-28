require 'RubyGems'
require 'sqlite3'

db = SQLite3::Database.new( ARGV[1] )

File.open(ARGV[0], "r") do |infile|
  counter = 1
  while (line = infile.gets)
    line.strip!
    if (!line.start_with? '>')
      start_position = counter
      end_position = counter + line.length - 1
      db.execute( "insert into bases values (1, #{start_position}, #{end_position}, '#{line}');" )
      counter = end_position + 1
      if (counter % 100 == 0)
        puts counter
      end
    end
  end
end

# TTGACCCACTGAATCACGTCTGACCGCGCGTACGCGGTCACTTGCGGTGCCGTTTTCTTTGTTACCGACG