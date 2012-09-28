-- create Genome Browser tables for SQLite embedded DB
-- note that in SqliteDatasource.createTables(...) we assume that sql
-- statements are separated by ";\n"

CREATE TABLE if not exists datasets (
	uuid text PRIMARY KEY NOT NULL,
	name text);

CREATE TABLE if not exists sequences (
	id integer primary key AUTOINCREMENT not null,
	uuid text not null,
	name text not null,
	length integer not null,
	topology text);

-- try to support large numbers of sequences
CREATE UNIQUE INDEX if not exists sequences_uuid_index on sequences(uuid);
CREATE UNIQUE INDEX if not exists sequences_name_index on sequences(name);

CREATE TABLE if not exists tracks (
	uuid text primary key not null,
	name text not null,
	type text not null,
	table_name text not null);

CREATE TABLE if not exists datasets_tracks (
	datasets_uuid text NOT NULL,
	tracks_uuid integer NOT NULL);

CREATE TABLE if not exists datasets_sequences (
	datasets_uuid text not null,
	sequences_uuid text not null);

-- Any entity with a uuid can have attributes
-- value is a value of type integer|text|numeric. Booleans can be stored
-- as integers or strings, colors as integers.
CREATE TABLE if not exists attributes (
	uuid text NOT NULL,
	key text NOT NULL,
	value);

CREATE INDEX if not exists attributes_index ON attributes (uuid);

-- block_index divides track data (features) into blocks which can be loaded
-- into memory and cached. Blocks are delineated by a sequence, strand and a
-- range of coordinates, so we can easily check for intersection between the
-- visible area of the genome and each block.
CREATE TABLE if not exists block_index (
	tracks_uuid text not null,
	sequences_id integer not null,
	seqId text not null,
	strand text not null,
	start integer not null,
	end integer not null,
	length integer not null,
	table_name text not null,
	first_row_id integer not null,
	last_row_id integer not null);

CREATE INDEX if not exists block_index_index ON block_index (tracks_uuid, sequences_id, strand, start, end);

-- Store named collections of bookmarks
-- We could further normalize, but let's keep it down to one table for now
-- bookmarks are specilized features, but the situation is a little differant
-- because the user can add, delete, and edit bookmarks. This means we can't
-- rely on rowid as we do with other types of features
CREATE TABLE if not exists bookmarks (
	id integer primary key autoincrement,
	collectionName text not null,
	sequences_id integer not null,
	strand text not null,
	start integer not null,
	end integer not null,
	name text not null,
	sequence text,
	annotation text);
	
-- Store sequences from a fasta file (dmartinez)
CREATE TABLE if not exists bases (
	sequence_id TEXT,
	start INTEGER, 
	end INTEGER,
	sequence TEXT);	

