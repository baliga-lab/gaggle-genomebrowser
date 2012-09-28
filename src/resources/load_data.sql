.mode tabs
CREATE TABLE temp (name, common_name, start integer, end integer, strand);
.import ./data/HaloTilingArrayReferenceConditions/chromosome/chromosome_coordinates.txt temp
delete from temp where name='canonical_Name';
update temp set strand='+' where strand='For';
update temp set strand='-' where strand='Rev';
update temp set common_name=NULL where common_name='' or common_name=name;
CREATE TABLE features_genes_temp (
	sequences_id integer NOT NULL,
	strand text NOT NULL,
	start integer NOT NULL,
	end integer NOT NULL,
	name text,
	common_name text,
	gene_type text);
INSERT INTO features_genes_temp SELECT 1 as sequences_id, strand, start, end, name, common_name, 'cds' as gene_type from temp;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC200/pNRC200_coordinates.txt temp
delete from temp where name='canonical_Name';
update temp set strand='+' where strand='For';
update temp set strand='-' where strand='Rev';
update temp set common_name=NULL where common_name='' or common_name=name;
INSERT INTO features_genes_temp SELECT 2 as sequences_id, strand, start, end, name, common_name, 'cds' as gene_type from temp;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC100/pNRC100_coordinates.txt temp
delete from temp where name='canonical_Name';
update temp set strand='+' where strand='For';
update temp set strand='-' where strand='Rev';
update temp set common_name=NULL where common_name='' or common_name=name;
INSERT INTO features_genes_temp SELECT 3 as sequences_id, strand, start, end, name, common_name, 'cds' as gene_type from temp;
delete from temp where 1=1;

drop table temp;
CREATE TABLE temp (product_name, start integer, end integer, strand, length, gene_id, locus, locus_tag);
.import ./data/HaloTilingArrayReferenceConditions/chromosome/rna.tsv temp
delete from temp where product_name='Product Name';
INSERT INTO features_genes_temp SELECT 1 as sequences_id, strand, start, end, product_name as name, locus_tag as common_name, 'rna' as gene_type from temp;
update features_genes_temp set gene_type='trna' where name like '%tRNA';
update features_genes_temp set gene_type='rrna' where name like '%ribosomal RNA';
drop table temp;

-- create the features_genes table in two stages so we can sort the RNA genes in with
-- the coding sequences.
CREATE TABLE features_genes (
	sequences_id integer NOT NULL,
	strand text NOT NULL,
	start integer NOT NULL,
	end integer NOT NULL,
	name text,
	common_name text,
	gene_type text);
insert into features_genes select * from features_genes_temp order by sequences_id, strand, start, end;
drop table features_genes_temp;

CREATE TABLE features_transcript_signal (
	sequences_id integer NOT NULL,
	strand text NOT NULL,
	start integer NOT NULL,
	end integer NOT NULL,
	value numeric);

CREATE TABLE temp (start integer, end integer, value numeric);
.import ./data/HaloTilingArrayReferenceConditions/chromosome/transcript.signal.forward.tsv temp
delete from temp where start like '%START';
INSERT INTO features_transcript_signal SELECT 1 as sequences_id, '+' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/chromosome/transcript.signal.reverse.tsv temp
delete from temp where start like '%START';
INSERT INTO features_transcript_signal SELECT 1 as sequences_id, '-' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;
drop table temp;

CREATE TABLE temp (start integer, end integer, value numeric, junk1, junk2);
.import ./data/HaloTilingArrayReferenceConditions/pNRC200/transcript.signal.forward.tsv temp
delete from temp where start like '%START';
INSERT INTO features_transcript_signal SELECT 2 as sequences_id, '+' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC200/transcript.signal.reverse.tsv temp
delete from temp where start like '%START';
INSERT INTO features_transcript_signal SELECT 2 as sequences_id, '-' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC100/transcript.signal.forward.tsv temp
delete from temp where start like '%START';
INSERT INTO features_transcript_signal SELECT 3 as sequences_id, '+' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC100/transcript.signal.reverse.tsv temp
delete from temp where start like '%START';
INSERT INTO features_transcript_signal SELECT 3 as sequences_id, '-' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;


create table features_segmentation (
	sequences_id integer NOT NULL,
	strand text NOT NULL,
	start integer NOT NULL,
	end integer NOT NULL,
	value numeric);

drop table temp;
CREATE TABLE temp (start integer, end integer, value numeric);
.import ./data/HaloTilingArrayReferenceConditions/chromosome/segmentation.forward.tsv temp
delete from temp where start like '%START';
INSERT INTO features_segmentation SELECT 1 as sequences_id, '+' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/chromosome/segmentation.reverse.tsv temp
delete from temp where start like '%START';
INSERT INTO features_segmentation SELECT 1 as sequences_id, '-' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC200/segmentation.forward.tsv temp
delete from temp where start like '%START';
INSERT INTO features_segmentation SELECT 2 as sequences_id, '+' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC200/segmentation.reverse.tsv temp
delete from temp where start like '%START';
INSERT INTO features_segmentation SELECT 2 as sequences_id, '-' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC100/segmentation.forward.tsv temp
delete from temp where start like '%START';
INSERT INTO features_segmentation SELECT 3 as sequences_id, '+' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC100/segmentation.reverse.tsv temp
delete from temp where start like '%START';
INSERT INTO features_segmentation SELECT 3 as sequences_id, '-' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;


create table features_chip_chip_tfbd_500bp (
	sequences_id integer NOT NULL,
	strand text NOT NULL,
	start integer NOT NULL,
	end integer NOT NULL,
	value numeric);

.import ./data/HaloTilingArrayReferenceConditions/chromosome/tfbd_isb_Chr.tsv temp
delete from temp where start like '%START';
INSERT INTO features_chip_chip_tfbd_500bp SELECT 1 as sequences_id, '.' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC200/tfbd_isb_pNRC200.tsv temp
delete from temp where start like '%START';
INSERT INTO features_chip_chip_tfbd_500bp SELECT 2 as sequences_id, '.' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC100/tfbd_isb_pNRC100.tsv temp
delete from temp where start like '%START';
INSERT INTO features_chip_chip_tfbd_500bp SELECT 3 as sequences_id, '.' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;


create table features_chip_chip_tfbd_nimb (
	sequences_id integer NOT NULL,
	strand text NOT NULL,
	start integer NOT NULL,
	end integer NOT NULL,
	value numeric);

.import ./data/HaloTilingArrayReferenceConditions/chromosome/tfbd_nimb_Chr.tsv temp
delete from temp where start like '%START';
INSERT INTO features_chip_chip_tfbd_nimb SELECT 1 as sequences_id, '.' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC200/tfbd_nimb_pNRC200.tsv temp
delete from temp where start like '%START';
INSERT INTO features_chip_chip_tfbd_nimb SELECT 2 as sequences_id, '.' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC100/tfbd_nimb_pNRC100.tsv temp
delete from temp where start like '%START';
INSERT INTO features_chip_chip_tfbd_nimb SELECT 3 as sequences_id, '.' as strand, start, end, value from temp order by start, end;
delete from temp where 1=1;


create table features_peaks_chip_chip_tfbd_500bp (
	sequences_id integer NOT NULL,
	strand text NOT NULL,
	position integer NOT NULL,
	value numeric);

drop table temp;
CREATE TABLE temp (position integer, value numeric);
.import ./data/HaloTilingArrayReferenceConditions/chromosome/peak_tfbd_isb_Chr.tsv temp
delete from temp where position like '%POSITION';
INSERT INTO features_peaks_chip_chip_tfbd_500bp SELECT 1 as sequences_id, '.' as strand, position, value from temp order by position;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC200/peak_tfbd_isb_pNRC200.tsv temp
delete from temp where position like '%POSITION';
INSERT INTO features_peaks_chip_chip_tfbd_500bp SELECT 2 as sequences_id, '.' as strand, position, value from temp order by position;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC100/peak_tfbd_isb_pNRC100.tsv temp
delete from temp where position like '%POSITION';
INSERT INTO features_peaks_chip_chip_tfbd_500bp SELECT 3 as sequences_id, '.' as strand, position, value from temp order by position;
delete from temp where 1=1;


create table features_peaks_chip_chip_tfbd_nimb (
	sequences_id integer NOT NULL,
	strand text NOT NULL,
	position integer NOT NULL,
	value numeric);

.import ./data/HaloTilingArrayReferenceConditions/chromosome/peak_tfbd_nimb_Chr.tsv temp
delete from temp where position like '%POSITION';
INSERT INTO features_peaks_chip_chip_tfbd_nimb SELECT 1 as sequences_id, '.' as strand, position, value from temp order by position;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC200/peak_tfbd_nimb_pNRC200.tsv temp
delete from temp where position like '%POSITION';
INSERT INTO features_peaks_chip_chip_tfbd_nimb SELECT 2 as sequences_id, '.' as strand, position, value from temp order by position;
delete from temp where 1=1;

.import ./data/HaloTilingArrayReferenceConditions/pNRC100/peak_tfbd_nimb_pNRC100.tsv temp
delete from temp where position like '%POSITION';
INSERT INTO features_peaks_chip_chip_tfbd_nimb SELECT 3 as sequences_id, '.' as strand, position, value from temp order by position;
delete from temp where 1=1;

drop table temp;

insert into tracks values (1, 'B7C82F8E-1485-13A6-56A05454028FBE19', 'Genome', 'gene', 'features_gene');
insert into tracks values (2, 'B7C82F9E-1485-13A6-568398D8F23B06E0', 'Transcription signal', 'quantitative.segment', 'features_transcript_signal');
insert into tracks values (3, 'B7C82FAD-1485-13A6-5636CB652DB72455', 'Segmentation', 'quantitative.segment', 'features_segmentation');
insert into tracks values (4, 'B7E980F9-1485-13A6-5615F47620241891', 'ChIP-chip TFBd nimb', 'quantitative.segment', 'features_chip_chip_tfbd_nimb');
insert into tracks values (5, 'B7E98109-1485-13A6-56428D5C66376F66', 'ChIP-chip TFBd 500bp', 'quantitative.segment', 'features_chip_chip_tfbd_500bp');
insert into tracks values (6, 'B7E98119-1485-13A6-56D415206FB3A1EB', 'Peaks ChIP-chip TFBd nimb', 'quantitative.positional', 'features_peaks_chip_chip_tfbd_nimb');
insert into tracks values (7, 'B7E9AB26-1485-13A6-56EF7B947915F1E8', 'Peaks ChIP-chip TFBd 500bp', 'quantitative.positional', 'features_peaks_chip_chip_tfbd_500bp');

insert into sequences values (1, 'chromosome', 2014239);
insert into sequences values (2, 'pNRC200', 365425);
insert into sequences values (3, 'pNRC100', 191346);

