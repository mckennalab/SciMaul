package recipe.sequence

import barcodes.{BarcodeTrio, FastBarcode}
import barcodes.FastBarcode.FastBarcode
import recipe.ResolvedDimension
import utils.BitEncoding

import scala.collection.mutable

class SequenceCorrector(resolvedDimension: ResolvedDimension) {

  val mask = FastBarcode.stringSizeToMask(resolvedDimension.length)

  // to make this lookup as fast as possible, we'll make a mapping of every possible
  // one-base change to a barcode
  val sequenceMapping = new mutable.LinkedHashMap[FastBarcode,SequenceAndError]()

  resolvedDimension.sequences.foreach{case(seq) => {
    sequenceMapping(seq.fastBarcode) = SequenceAndError(seq,0)
    SequenceCorrector.toFastBarcodeList(SequenceCorrector.toOneBaseChange(seq.sequence)).
      foreach{fb =>
        sequenceMapping(fb) = SequenceAndError(seq,1)}
  }}

  val conversionMemory = new mutable.HashMap[String,BarcodeTrio]()

  /**
    * correct an observed sequence to a set barcode, within a max distance (counting Ns in the barcode as
    * an automatic mismatch)
    * @param string the sequence
    * @return the corrected sequence and distance
    */
  def correctSequence(string: String, maxDist: Int = 2, extensiveSearch: Boolean = false): Option[SequenceAndError] = {
    assert(string.size == resolvedDimension.length, "This barcode is of the wrong length: " + string + " (should be len " + resolvedDimension.length + ")")

    // our hashed barcode lookup
    val fb = conversionMemory.getOrElseUpdate(string,FastBarcode.toFastBarcodeWithMismatches(string))

    if (sequenceMapping contains fb.barcode)
      return Some(SequenceAndError(sequenceMapping(fb.barcode).sequence,sequenceMapping(fb.barcode).error + fb.mismatches))

    // this is where things get much more expensive -- we have to look through the whole list and
    // find the closest hit
    var bestHitScore = Int.MaxValue
    var bestHit : Option[SequenceAndError] = None

    if (extensiveSearch) {
      resolvedDimension.sequences.foreach { case (sqs) => {
        val mm = FastBarcode.mismatches(fb.barcode, sqs.fastBarcode, fb.mask) + fb.mismatches
        if (mm <= maxDist && mm < bestHitScore) {
          bestHit = Some(SequenceAndError(sqs, mm))
          bestHitScore = mm
        }
      }
      }
    }
    bestHit
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
    strings.map{str => BitEncoding.bitEncodeString(str)}
  }
}