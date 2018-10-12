package recipe.sequence

import barcodes.{BarcodeTrio, FastBarcode}
import barcodes.FastBarcode.FastBarcode
import com.typesafe.scalalogging.LazyLogging
import recipe.ResolvedDimension
import utils.BitEncoding

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class SequenceCorrector(resolvedDimension: ResolvedDimension) extends LazyLogging {

  val mask = FastBarcode.stringSizeToMask(resolvedDimension.length)

  // to make this lookup as fast as possible, we'll make a mapping of every possible
  // one-base change to a barcode
  val sequenceMapping = new mutable.LinkedHashMap[FastBarcode, SequenceAndError]()

  var collisions = 0

  val sequencePermutations = new ArrayBuffer[Tuple2[FastBarcode, SequenceAndError]]()
  resolvedDimension.sequences.foreach { case (seq) => {
    sequencePermutations += Tuple2[FastBarcode, SequenceAndError](seq.fastBarcode, SequenceAndError(seq, 0))
    sequenceMapping(seq.fastBarcode) = SequenceAndError(seq, 0)
  }
  }

  // now generate errors on those sequences
  (0 until resolvedDimension.maxError).foreach { errorStep => {
    val addedError = new ArrayBuffer[Tuple2[FastBarcode, SequenceAndError]]()

    sequencePermutations.foreach { case (fb, seqAndError) => {
      SequenceCorrector.toFastBarcodeList(SequenceCorrector.toOneBaseChange(seqAndError.sequence.sequence)).
        foreach { errorFB => {
          addedError += Tuple2[FastBarcode, SequenceAndError](errorFB, SequenceAndError(seqAndError.sequence, seqAndError.error + 1))
        }
        }
    }
    }
    addedError.foreach { case (fb, seqAndError) => {
      sequencePermutations += Tuple2[FastBarcode, SequenceAndError](fb, seqAndError)

      // check for collision
      if ((sequenceMapping contains fb) && (sequenceMapping(fb).error != 1)){
        collisions += 1
        // logger.warn("Collision with barcode " + BitEncoding.bitDecodeString(fb, seq.sequence.size) + " from " + seqAndError.error + "'s from " + seqAndError.sequence)
      } else {
        sequenceMapping(fb) = seqAndError
      }
    }
    }
  }
  }

  logger.info("For dimension " + resolvedDimension.name + " we generated " + sequenceMapping.size + " sequences with an allowed error count of " + resolvedDimension.maxError)
  logger.info("For dimension " + resolvedDimension.name + " we had " + collisions + " collisions")
  val conversionMemory = new mutable.HashMap[String, BarcodeTrio]()

  /**
    * correct an observed sequence to a set barcode, within a max distance (counting Ns in the barcode as
    * an automatic mismatch)
    *
    * @param string the sequence
    * @return the corrected sequence and distance
    */
  def correctSequence(string: String, maxDist: Int = 2, extensiveSearch: Boolean = false): Option[SequenceAndError] = {
    assert(string.size == resolvedDimension.length, "This barcode is of the wrong length: " + string + " (should be len " + resolvedDimension.length + ")")

    // our hashed barcode lookup
    val fb = conversionMemory.getOrElseUpdate(string, FastBarcode.toFastBarcodeWithMismatches(string))

    if (sequenceMapping contains fb.barcode)
      return Some(SequenceAndError(sequenceMapping(fb.barcode).sequence, sequenceMapping(fb.barcode).error + fb.mismatches))

    // this is where things get much more expensive -- we have to look through the whole list and
    // find the closest hit
    var bestHitScore = Int.MaxValue
    var bestHit: Option[SequenceAndError] = None

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
    code.toArray.zipWithIndex.flatMap { case (base, index) => {
      SequenceCorrector.otherBases(base).map { altBase => {
        code.slice(0, index) + altBase.toString + code.slice(index + 1, code.length)
      }
      }
    }
    }.toList
  }


  def otherBases(base: Char): Array[Char] = base match {
    case 'A' => Array[Char]('C', 'G', 'T')
    case 'C' => Array[Char]('A', 'G', 'T')
    case 'G' => Array[Char]('C', 'A', 'T')
    case 'T' => Array[Char]('C', 'G', 'A')
    case _ => throw new IllegalStateException("Unable to process base: " + base)
  }

  def toFastBarcodeList(strings: List[String]): List[FastBarcode] = {
    strings.map { str => BitEncoding.bitEncodeString(str) }
  }
}