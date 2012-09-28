# An example of how one might replace the gene annotations in a .hbgb file
# cbare@systemsbiology.org

library('RSQLite')
conn = dbConnect(SQLite(), 'halo_new_genes.hbgb')
on.exit( close(conn) )

# delete old genes (do this on a copy!)
sql <- "delete from features_genes where 1=1;"
dbSendQuery(conn, sql)

# we're going to load features into a temp table first
# so, clear out any existing temp table
sql <- "drop table temp;"
dbSendQuery(conn, sql)

# get sequence data
sql <- "select * from sequences;"
seqs <- dbGetQuery(conn, sql)

# very advanced gene finding algorithm
strands <- c('+','-')
genes <- do.call(rbind,
  apply(seqs, 1, function(sequence) {
    do.call(rbind,
    lapply(strands, function(strand) {
      offset = if (strand=='-') { 300 } else { 0 }
      starts = seq(1+offset,as.integer(sequence['length'])-200-offset,600)
      data.frame(sequences_id=as.integer(sequence['id']),
                 strand=strand,
                 start=starts,
                 end=starts+200,
                 name=sprintf('VNG%04d', starts),
                 common_name=sprintf('foo%d', starts %% 10),
                 gene_type='cds',
                 stringsAsFactors=F)
    }))
  })
)

# insert genes into temp table
dbWriteTable(conn,'temp',genes)

# copy new gene data into features_genes table
sql <- "insert into features_genes
        select sequences_id, strand, start, end, name, common_name, gene_type
        from temp
        order by sequences_id, strand, start, end;"
dbSendQuery(conn, sql)

# cleanup
sql <- "drop table temp;"
dbSendQuery(conn, sql)
sql <- "vacuum;"
dbSendQuery(conn, sql)

