package transforms

import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
import recipe.ResolvedDimension
import utils.ReadUtils

/**
  * clean up reads based on quality score
  * @param dim
  */
class QualityScoreTransformReads(dim: Array[ResolvedDimension], minQual: Int, windowSize: Int) extends ReadTransform {
  override def name: String = "QualTransform"

  override def description: String = "Remove poor quality regions of reads"

  override def transform(reads: ReadContainer): TransforedReadAndDimension = {
    // TransforedReadAndDimension()
    reads.read1 = if (reads.read1.isDefined)
      Some(QualityScoreTransformReads.removeLowQualityEnd(reads.read1.get,minQual,windowSize)) else None
    reads.read2 = if (reads.read1.isDefined)
      Some(QualityScoreTransformReads.removeLowQualityEnd(reads.read1.get,minQual,windowSize)) else None

    TransforedReadAndDimension(reads,None)
  }

  override def dimension: Option[ResolvedDimension] = None

  override def isDimensioned: Boolean = false
}

object QualityScoreTransformReads {

  /**
    * Remove the low quality end of a read
    * @param fastqRecord the fastq record to clean up
    * @param minQual the minimal quality score, in phred score (e.g. 30)
    * @param windowSize the size of the window to look
    * @return a transformed fastq record
    */
  def removeLowQualityEnd(fastqRecord: FastqRecord, minQual: Int, windowSize: Int): FastqRecord = {

    // make a running average over the read
    val toProb = fastqRecord.getBaseQualityString.map{c => ReadUtils.phredToScore(c)}.toArray

    // where to start removing sequence
    var sliceWindow = -1
    (0 until (toProb.size - windowSize)).foreach{ index => {
      val average = toProb.slice(index, index + windowSize).sum / windowSize.toDouble
      if (average <= minQual && sliceWindow < 0) {
        sliceWindow = index
      }
    }}

    if (sliceWindow >= 0) {
      new FastqRecord(fastqRecord.getReadHeader,
        fastqRecord.getReadString.slice(0,sliceWindow),
        fastqRecord.getBaseQualityHeader,
        fastqRecord.getBaseQualityString.slice(0,sliceWindow))
    } else {
      fastqRecord
    }
  }


}