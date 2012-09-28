##
## Import peptidespectrareport-with-genome-location.txt to sqlite
## for use in the Gaggle Genome Browser.
##

# These Sulfolobus peptides come from Steve Yannone's fractionation experiments, as
# mapped to the SSO genome by Kenneth Frankel kafrankel@lbl.gov.
#
# additional junk that goes with this is in:
# /Users/cbare/Documents/work/projects/genome_browser/data/sulfolobus_prot_yannone
#
# We create on large table with all experiments in it and then create a view for
# each experiment so the visibility can be toggled individually. We insert the
# features into a temp table first, so we can insert them in sorted order into
# the real table.

require 'rubygems'
require 'sqlite3'
require 'UUIDTools'


db = SQLite3::Database.new( "/Users/cbare/Documents/hbgb/Sulfolobus_solfataricus_P2_proteomics_2.hbgb" )

experiments = [ "Insoluble_SucroseGradientNoIMV_Aug_24_2008",
                "LMW_10fracSucGradSuperose6_Nov_25_2008",
                "LMW_NoGels_Aug_8_2009",
                "LMW_SucroseGradientNoIMV_Aug_24_2008",
                "LMW_UCSedGFElution4FF_Nov_14_2008",
                "MEM_TEMPRPR9_Jul_30_2008",
                "MEM_UCSedGFElution4FF_Nov_14_2008",
                "MT_SucroseGradientwMemb_Jan_11_2008",
                "MT_Temp2RPR6_Feb_07_2008",
                "Media_Secretion2_April_24_2009",
                "SEC_Secretion1_April_16_2009",
                "SEC_Secretion2_April_24_2009",
                "SEC_Secretion2_Mar_10_2009",
                "SEC_Secretome_July_13_2009",
                "SMW-Anaerobic_DEAE1_Mar_03_2007",
                "SMW-Anaerobic_DEAE2_Mar_03_2007",
                "SMW_45k4hSupernate_Aug_24_2008",
                "SMW_SucroseGradientNoIMV_Aug_24_2008",
                "d-LMW_19fracSucGradSuperose6_Nov_25_2008",
                "d-LMW_2060SucGradwIMV_Jan_11_2008",
                "d-LMW_SucroseGradientNoIMV_Aug_24_2008",
                "d-LMW_SucroseGradient_Aug_24_2008",
                "d-LMW_SucroseGradientwMemb_Jan_11_2008",
                "mwc-LMW_Temp1RPR6_Feb_07_2008",
                "mwc-LMW_Temp2RPR6_Feb_07_2008",
                "s-MEM_SLayer_July_9_2009",
                "therm-LMW_TEMPRPR9_Jul_30_2008"]

def to_features_table_name(name)
  return "features_peptides_#{ name.gsub("-", "_") }"
end

db.execute( "drop table if exists temp;")
db.execute( """
create table temp (
  sequences_id integer,
  strand text,
  start integer,
  end integer,
  name text,
  common_name text,
  gene_type text,
  score real,
  redundancy int,
  #{ (0...experiments.length).map {|i| "value#{i} real" }.join(", ") }
);""" )

file = File.open('peptidespectrareport-with-genome-location.txt')

headers = file.readline.chomp.split("\t")
line_num = 0

# load data from text file into temp table. We'll query and sort
# rows out of this table into a totals table and a table for
# each individual experiment / fraction.

while (line = file.gets)
  fields = line.chomp.split("\t")
  line_num += 1

  name = fields[0]
  
  fractions = fields[1..27].map { |field| field.to_i }

  # Starting in column 33, there can be several places on the genome where the
  # peptide maps. Strand is encoded in whether start < end or end < start. 
  loci = []
  col = 33

  while (col < 200)
    pos1 = fields[col].to_i
    pos2 = fields[col+1].to_i
    if (pos1==0 and pos2==0) then break end
    strand = pos1 < pos2 ? '+' : '-'
    start_pos = [pos1, pos2].min
    end_pos = [pos1, pos2].max
    if (end_pos - start_pos > 10000)
      puts "~~~~~~~~~~~~~~~~~~~~~ feature questionably large\n"
      puts line
      puts "~~~~~~~~~~~~~~~~~~~~~\n"
    else
      loci << [strand, start_pos, end_pos]
    end
    col += 2
  end

  if loci.length == 0 then
    puts "No locations for #{name} on line #{line_num}!"
  end
  
  loci.each do |locus|
    sql = "insert into temp values (1, '#{locus[0]}', #{locus[1]}, #{locus[2]}, '#{name}', NULL, 'peptide', #{ fractions.inject(0, &:+) }, #{ loci.length }, #{ fractions.map {|f| "#{f}"}.join(", ") });"
    puts sql
    db.execute( sql )
  end

end

file.close

# create peptides_fractions table, holding total scores for all peptides.

db.execute( "drop table if exists peptides_27_experiments;" )
db.execute( "drop table if exists peptides_fractions;" )
db.execute( """
create table peptides_fractions (
  sequences_id integer,
  strand text,
  start integer,
  end integer,
  name text,
  common_name text,
  gene_type text,
  score real,
  redundancy int
);""" )
db.execute( "insert into peptides_fractions select sequences_id, strand, start, end, name, common_name, gene_type, score, redundancy from temp order by sequences_id, strand, start, end;" )

# get dataset uuid
result = db.query( "select uuid, name from datasets;" )
if row = result.next
  printf "dataset: %s, %s\n", row[0], row[1]
  dataset_uuid = row[0]
else
  raise "No datasets found!"
end
result.close

# cleanup old junk, if necessary
track_uuids = []
db.execute( "select uuid from tracks where name like 'peptides:%';" ) { |row| track_uuids << row[0] }
db.execute( "select uuid from tracks where name = 'peptides: all fractions';" ) { |row| track_uuids << row[0] }
db.execute( "delete from tracks where uuid in (#{track_uuids.map {|uuid| "'#{uuid}'"}.join(', ')});" )
db.execute( "delete from datasets_tracks where tracks_uuid in (#{track_uuids.map {|uuid| "'#{uuid}'"}.join(', ')});" )
db.execute( "delete from attributes where uuid in (#{track_uuids.map {|uuid| "'#{uuid}'"}.join(', ')});" )
db.execute( "delete from block_index where tracks_uuid in (#{track_uuids.map {|uuid| "'#{uuid}'"}.join(', ')});" )


track_uuid = UUIDTools::UUID.timestamp_create
db.execute( "insert into tracks values ('#{track_uuid.to_s}', 'peptides: all fractions', 'peptide', 'peptides_fractions');" )
db.execute( "insert into datasets_tracks values ('#{dataset_uuid.to_s}', '#{track_uuid.to_s}');" )
# attributes
db.execute( "insert into attributes values('#{track_uuid.to_s}', 'color', '0x80ff8080');" )
db.execute( "insert into attributes values('#{track_uuid.to_s}', 'height', 0.2);" )
db.execute( "insert into attributes values('#{track_uuid.to_s}', 'offset', 70);" )
db.execute( "insert into attributes values('#{track_uuid.to_s}', 'top', 0.4);" )
db.execute( "insert into attributes values('#{track_uuid.to_s}', 'viewer', 'Peptide');" )

# I wanted to create views into peptides_27_experiments for each track, but that doesn't work,
# because the data is sparse and if we use a where clause to select out the non-zero entries
# for each column, we get discontinuous rowids, which messes up my block indexing scheme.

# here we create individual tracks for each fraction / experiment.

for i in 0...experiments.length do
  track_uuid = UUIDTools::UUID.timestamp_create
  db.execute( "drop table if exists #{to_features_table_name( experiments[i] )};")
  db.execute( """
  create table #{to_features_table_name( experiments[i] )} (
    sequences_id integer,
    strand text,
    start integer,
    end integer,
    name text,
    common_name text,
    gene_type text,
    score real,
    redundancy int
  );""" )
  db.execute( """insert into #{to_features_table_name( experiments[i] )}
                 select sequences_id, strand, start, end, name, common_name, gene_type, value#{i} as score, redundancy
                 from temp
                 where value#{i} > 0
                 order by sequences_id, strand, start, end;""")
  db.execute( "insert into tracks values ('#{track_uuid.to_s}', 'peptides: #{experiments[i]}', 'peptide', '#{to_features_table_name( experiments[i] )}');" )
  db.execute( "insert into datasets_tracks values ('#{dataset_uuid.to_s}', '#{track_uuid.to_s}');" )
  
  # attributes
  db.execute( "insert into attributes values('#{track_uuid.to_s}', 'color', '0x80ff8080');" )
  db.execute( "insert into attributes values('#{track_uuid.to_s}', 'height', 0.2);" )
  db.execute( "insert into attributes values('#{track_uuid.to_s}', 'offset', 58);" )
  db.execute( "insert into attributes values('#{track_uuid.to_s}', 'top', 0.4);" )
  db.execute( "insert into attributes values('#{track_uuid.to_s}', 'viewer', 'Peptide');" )
  db.execute( "insert into attributes values('#{track_uuid.to_s}', 'groups', 'peptides');" )
end

db.execute( "drop table temp;" )

db.close
