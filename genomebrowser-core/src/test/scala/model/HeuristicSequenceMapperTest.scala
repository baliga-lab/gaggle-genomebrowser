package org.systemsbiology.genomebrowser.model

import java.util.{List, ArrayList}

import org.systemsbiology.util.Roman

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TestHeuristicSequenceMapperSpec extends FlatSpec with ShouldMatchers {
  private def haloSetup = {
		val mapper = new HeuristicSequenceMapper
		val sequenceNames = new ArrayList[String]
		sequenceNames add "chromosome"
		sequenceNames add "pNRC200"
		sequenceNames add "pNRC100"
		mapper.setStandardSequenceNames(sequenceNames)
    mapper
  }

  private def romanSetup = {
		val mapper = new HeuristicSequenceMapper
		val sequenceNames = new ArrayList[String]
		for (i <- 1 to 16) sequenceNames.add(Roman.intToRoman(i))
		sequenceNames.add("chrM")
		sequenceNames.add("2micron")
		mapper.setStandardSequenceNames(sequenceNames)
    mapper
  }

  private def arabicSetup = {
		val mapper = new HeuristicSequenceMapper
		val sequenceNames = new ArrayList[String]
		for (i <- 1 to 16) sequenceNames.add("chr" + i)
		sequenceNames.add("chrM")
		sequenceNames.add("2micron")
		mapper.setStandardSequenceNames(sequenceNames)
    mapper
  }

  "HeuristicSequenceMapper with halo setup" should "map sequence names to chromosome" in {
    val mapper = haloSetup

		mapper.map("chromosome") should be ("chromosome")
		mapper.map("Chromosome") should be ("chromosome")
		mapper.map("ChRoMoSOME") should be ("chromosome")

		mapper.map("ChRoMoSOME1") should be ("chromosome")
		mapper.map("ChRoMoSOME_1") should be ("chromosome")
		mapper.map("ChRoMoSOME 1") should be ("chromosome")

		mapper.map("chr") should be ("chromosome")
		mapper.map("chr1") should be ("chromosome")
		mapper.map("chr 1") should be ("chromosome")
		mapper.map("chr_1") should be ("chromosome")
		mapper.map("1") should be ("chromosome")

		mapper.map("chromosome") should be ("chromosome")
		mapper.map("chromosome-1") should be ("chromosome")
		mapper.map("CHR1") should be ("chromosome")
		mapper.map("CHR") should be ("chromosome")
  }
  it should "map to plasmid pNRC200" in {
    val mapper = haloSetup
		mapper.map("pnrc200") should be ("pNRC200")
		mapper.map("plasmid pnrc200") should be ("pNRC200")
		mapper.map("plasmid_pnrc200") should be ("pNRC200")
		mapper.map("PNRC200") should be ("pNRC200")
		mapper.map("pNRC200") should be ("pNRC200")
		mapper.map("plasmid pNRC200") should be ("pNRC200")
		mapper.map("plasmid_pNRC200") should be ("pNRC200")
  }

  it should "map to plasmid pNRC100" in {
    val mapper = haloSetup
		mapper.map("pNRC100") should be ("pNRC100")
		mapper.map("Plasmid pNRC100") should be ("pNRC100")
  }

  it should "throw an exception" in {
    val mapper = haloSetup
    val thrown = evaluating { mapper.map("asdfasdf") } should produce [RuntimeException]
    thrown.getMessage should be ("Unknown sequence: \"asdfasdf\"")
  }

  it should "handle roman and mixed case" in {
    val mapper = haloSetup
		mapper.map("ChRoMoSOME I") should be ("chromosome")
  }

  "A roman mapper setup" should "map names correctly" in {
    val mapper = romanSetup
		mapper.map("chrI") should be ("I")
		mapper.map("chrII") should be ("II")
		mapper.map("chr-II") should be ("II")
		mapper.map("chrIV") should be ("IV")
		mapper.map("chrX") should be ("X")
		mapper.map("chrXVI") should be ("XVI")
		mapper.map("m") should be ("chrM")
		mapper.map("M") should be ("chrM")
		mapper.map("2micron") should be ("2micron")
  }
  it should "handle names with arabic numbers" in {
    val mapper = romanSetup
		mapper.map("chr1") should be ("I")
		mapper.map("chr2") should be ("II")
		mapper.map("chr4") should be ("IV")
		mapper.map("chr10") should be ("X")
		mapper.map("chr16") should be ("XVI")
  }

  "An arabic mapper setup" should "map names correctly" in {
    val mapper = arabicSetup
		mapper.map("I") should be ("chr1")
		mapper.map("II") should be ("chr2")
		mapper.map("IV") should be ("chr4")
		mapper.map("X") should be ("chr10")
		mapper.map("XVI") should be ("chr16")

		mapper.map("1") should be ("chr1")
		mapper.map("2") should be ("chr2")
		mapper.map("4") should be ("chr4")
		mapper.map("10") should be ("chr10")
		mapper.map("16") should be ("chr16")

		mapper.map("chromosome II") should be ("chr2")
		mapper.map("chrIV") should be ("chr4")
		mapper.map("chr-IX") should be ("chr9")
		mapper.map("Chromosome_XI") should be ("chr11")

		mapper.map("M") should be ("chrM")
  }

  "A mixed setup" should "map according to suffixes" in {
		val mapper = new HeuristicSequenceMapper
		val sequenceNames = new ArrayList[String]
		sequenceNames.add("Chromosome 1")
		sequenceNames.add("Chromosome 2")
		sequenceNames.add("Chromosome 3")
		sequenceNames.add("Plasmid 1")
		sequenceNames.add("Plasmid 2")
		sequenceNames.add("Plasmid 3")
		sequenceNames.add("Plasmid A")
		mapper.setStandardSequenceNames(sequenceNames)

		mapper.map("chr1") should be ("Chromosome 1")
		mapper.map("chromosome1") should be ("Chromosome 1")
		mapper.map("chromosome 1") should be ("Chromosome 1")
		mapper.map("1") should be ("Chromosome 1")
		mapper.map("chr2") should be ("Chromosome 2")
		mapper.map("chromosome3") should be ("Chromosome 3")

    // plasmids
		mapper.map("plasmid1") should be ("Plasmid 1")
		mapper.map("plasmid_2") should be ("Plasmid 2")
		mapper.map("plasmid 3") should be ("Plasmid 3")
		mapper.map("plasmid A") should be ("Plasmid A")
		mapper.map("A") should be ("Plasmid A")
  }
}
