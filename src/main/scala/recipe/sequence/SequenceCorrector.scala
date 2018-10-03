package recipe.sequence

import barcodes.FastBarcode
import barcodes.FastBarcode.FastBarcode
import recipe.ResolvedDimension

import scala.collection.mutable

class SequenceCorrector(resolvedDimension: ResolvedDimension) {

  val mask = FastBarcode.stringSizeToMask(resolvedDimension.length)

  // to make this lookup as fast as possible, we'll make a mapping of every possible
  // one-base change to a barcode
  val sequenceMapping = new mutable.HashMap[FastBarcode,SequenceAndError]()

  resolvedDimension.sequences.foreach{case(seq) => {
    sequenceMapping(seq.fastBarcode) = SequenceAndError(seq,0)
    SequenceCorrector.toFastBarcodeList(SequenceCorrector.toOneBaseChange(seq.sequence)).
      foreach{fb =>
        sequenceMapping(fb) = SequenceAndError(seq,1)}
  }}

  /**
    * correct an observed sequence to a set barcode, with a distance
    * @param string the sequence
    * @return the corrected sequence and distance
    */
  def correctSequence(string: String): SequenceAndError = {
    assert(string.size == resolvedDimension.length, "This barcode is of the wrong length: " + string + " (should be len " + resolvedDimension.length + ")")
    val fb = FastBarcode.toFastBarcode(string)
    if (sequenceMapping contains fb)
      return sequenceMapping(fb)

    // this is where things get much more expensive -- we have to look through the whole list and
    // find the closest hit
    var bestHitScore = Int.MaxValue
    var bestHit : Option[SequenceAndError] = None
    resolvedDimension.sequences.foreach{case(sqs) => {
      val mm = FastBarcode.mismatches(fb,FastBarcode.toFastBarcode(sqs.sequence),mask)
      if (mm < bestHitScore) {
        bestHit = Some(SequenceAndError(sqs,mm))
        bestHitScore = mm
      }
    }}
    bestHit.get
  }
}

case class SequenceAndError(sequence: Sequence, error: Int)

object SequenceCorrector {
  def toOneBaseChange(code: String): List[String] = {
    code.toArray.zipWithIndex.flatMap{case(base,index) => {
      SequenceCorrector.otherBases(base).map { altBase => {
        code.slice(0, index) + altBase.toString + code.slice(index + 1, code.length)
      }}
    }}.toList
  }


  def otherBases(base: Char): Array[Char] = base match {
    case 'A' => Array[Char]('C','G','T')
    case 'C' => Array[Char]('A','G','T')
    case 'G' => Array[Char]('C','A','T')
    case 'T' => Array[Char]('C','G','A')
    case _ => throw new IllegalStateException("Unable to process base: " + base)
  }

  def toFastBarcodeList(strings: List[String]): List[FastBarcode] = {
    strings.map{str => FastBarcode.toFastBarcode(str)}
  }
}