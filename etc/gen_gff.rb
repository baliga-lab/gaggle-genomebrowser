seqs = [ Struct.new(:name, :length).new("chromosome", 2014239),
         Struct.new(:name, :length).new("pNRC200", 365425),
         Struct.new(:name, :length).new("pNRC100", 191346)]
strands = ["+", "-"]

f = 1
for seq in seqs do
  for strand in strands do
    1.step(seq.length, 300) do |s|
      e = s + 300
      value = Math.sin(s/300.0)
      puts("#{seq.name}\tsource\tfeature_#{f}\t#{s}\t#{e}\t#{value}\t#{strand}\n")
      f += 1
    end
  end
end


