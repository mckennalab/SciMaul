package scala


import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
import org.scalatest.{FlatSpec, Matchers}

class ReadContainerTest extends FlatSpec with Matchers {
  "ReadContainer" should "slice out a section of the read" in {

    // ReadContainer.sliceFromFastq(toSlice: FastqRecord, start: Int, length: Int, keepSequence: Boolean)
    val fastqRecord = new FastqRecord(
      "FakeRead",
      "AAAAACCCCCGGGGGTTTTTAAAAACCCCCGGGGGTTTTT",
      "+",
      "AAAAABBBBBAAAAABBBBBAAAAABBBBBAAAAABBBBB")

    val container = ReadContainer.sliceFromFastq(fastqRecord,0,6,false)

    ((container._1.getReadString) should be ("CCCCGGGGGTTTTTAAAAACCCCCGGGGGTTTTT"))
    ((container._1.getBaseQualityString) should be ("BBBBAAAAABBBBBAAAAABBBBBAAAAABBBBB"))
  }
}