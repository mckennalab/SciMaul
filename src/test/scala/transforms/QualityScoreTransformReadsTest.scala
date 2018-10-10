package transforms

import htsjdk.samtools.fastq.FastqRecord
import org.scalatest.{FlatSpec, Matchers}

class QualityScoreTransformReadsTest extends FlatSpec with Matchers {
  "QualityScoreTransformReads" should "leave a good read intact" in {
    val fakeRead = new FastqRecord("fakeRead","ACGTACGT","+", "AAAAAAAA")
    val cleaned = QualityScoreTransformReads.removeLowQualityEnd(fakeRead, 30, 5)
    ((cleaned.getReadString) should be (fakeRead.getReadString))
    ((cleaned.getBaseQualityString) should be (fakeRead.getBaseQualityString))
    ((cleaned.getReadHeader) should be (fakeRead.getReadHeader))
    ((cleaned.getBaseQualityHeader) should be (fakeRead.getBaseQualityHeader))
  }

  "QualityScoreTransformReads" should "trim a low quality region from a read" in {
    val fakeRead = new FastqRecord("fakeRead","CCGTACGT","+", "TTT&&&&&")
    val cleaned = QualityScoreTransformReads.removeLowQualityEnd(fakeRead, 30, 5)
    ((cleaned.getReadString) should be ("C"))
    ((cleaned.getBaseQualityString) should be ("T"))
  }

  "QualityScoreTransformReads" should "trim a low quality region from a read with a good base in there" in {
    val fakeRead = new FastqRecord("fakeRead","TCGTACGT","+", "TTTT&&&&")
    val cleaned = QualityScoreTransformReads.removeLowQualityEnd(fakeRead, 30, 5)
    ((cleaned.getReadString) should be ("TC"))
    ((cleaned.getBaseQualityString) should be ("TT"))
  }

  "QualityScoreTransformReads" should "trim a low quality region from a read even with a high-quality end" in {
    val fakeRead = new FastqRecord("fakeRead","TCGTACGTAATT","+", "TTTT&T&&&TTTT")
    val cleaned = QualityScoreTransformReads.removeLowQualityEnd(fakeRead, 30, 5)
    ((cleaned.getReadString) should be ("TCG"))
    ((cleaned.getBaseQualityString) should be ("TTT"))
  }
}