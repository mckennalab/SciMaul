package recipe.sequence

import algorithms.dynamic.VerySimpleNeedlemanWunsch
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
  val sequenceMapping = new mutable.LinkedHashMap[String, Option[SequenceAndError]]()

  var collisions = 0

  // Generate the set of common errors in the known sequences for quick lookup
  permuteErrorCodes()
  logger.info("For dimension " + resolvedDimension.name + " we generated " + sequenceMapping.size + " sequences with an allowed error count of " + resolvedDimension.maxError +
    "; we had " + collisions + " collisions when making potential error-mismatched sequences")

  val conversionMemory = new mutable.HashMap[String, BarcodeTrio]()


  /**
    * correct an observed sequence to a set barcode, within a max distance (counting Ns in the barcode as
    * an automatic mismatch)
    *
    * @param string  the sequence to map onto the known barcodes
    * @param maxDist the maximum allowed distance of a potential match to a known barcode
    * @return the corrected sequence and distance
    */
  def correctSequence(string: String, maxDist: Int = 1): Option[SequenceAndError] = {
    assert(string.size == resolvedDimension.length, "This barcode is of the wrong length: " + string + " (should be len " + resolvedDimension.length + ")")

    if (!resolvedDimension.allowAlignmentCorrection) {
      sequenceMapping.getOrElse(string, None)
    } else {
      sequenceMapping.getOrElseUpdate(string, {
        // this is where things get much more expensive -- we have to look through the whole list and
        // find the closest hit by alignment. If we do find something we'll cache it for later lookup
        var bestHitScore = Int.MaxValue
        var bestHit: Option[SequenceAndError] = None
        var dropSequence = false

        if (resolvedDimension.allowAlignmentCorrection) {
          resolvedDimension.sequences.foreach { case (sqs) => {
            SequenceCorrector.alignmentMatch(string, sqs, maxDist).foreach { case (aligned) => {
              if (aligned.error < bestHitScore) {
                bestHit = Some(aligned)
                bestHitScore = aligned.error
              } else if (aligned.error == bestHitScore) {
                dropSequence = true
              }
            }
            }
          }
          }
        }

        if (dropSequence)
          return None

        bestHit
      })
    }
  }


  /**
    * This function builds up the internal hash lookup tables for our sequences, generating a number of possible mismatch sequences that
    * we might expect to see within N errors to the original set
    */
  def permuteErrorCodes(): Unit = {
    val sequencePermutations = new ArrayBuffer[Tuple2[String, SequenceAndError]]()

    resolvedDimension.sequences.foreach { case (seq) => {
      sequencePermutations += Tuple2[String, SequenceAndError](seq.name, SequenceAndError(seq, 0))
      sequenceMapping(seq.sequence) = Some(SequenceAndError(seq, 0))
    }
    }

    // now generate errors on those sequences
    (0 until resolvedDimension.maxError).foreach { errorStep => {
      val addedError = new ArrayBuffer[Tuple2[String, SequenceAndError]]()

      sequencePermutations.foreach { case (fb, seqAndError) => {
        SequenceCorrector.toOneBaseChange(seqAndError.sequence.sequence).
          foreach { errorFB => {
            addedError += Tuple2[String, SequenceAndError](errorFB, SequenceAndError(seqAndError.sequence, seqAndError.error + 1))
          }
          }
      }
      }
      addedError.foreach { case (fb, seqAndError) => {
        sequencePermutations += Tuple2[String, SequenceAndError](fb, seqAndError)

        // check for collision
        if ((sequenceMapping contains fb) && (sequenceMapping(fb).get.error != 1)) {
          collisions += 1
          logger.warn("Collision with barcode " + fb + " from " + seqAndError.error + "'s from " + seqAndError.sequence)
        } else {
          sequenceMapping(fb) = Some(seqAndError)
        }
      }
      }
    }
    }
  }
}

case class SequenceAndError(sequence: Sequence, error: Int)

object SequenceCorrector {
  /**
    * create all possible one-base changes of a sequence
    *
    * @param code the original sequence
    * @return a List of all possible 1 base sequence changes
    */
  def toOneBaseChange(code: String): List[String] = {
    code.toArray.zipWithIndex.flatMap { case (base, index) => {
      SequenceCorrector.otherBases(base).map { altBase => {
        code.slice(0, index) + altBase.toString + code.slice(index + 1, code.length)
      }
      }
    }
    }.toList
  }


  /**
    * align two sequences with scoring parameters that allow for X mismatches or gaps
    *
    * @param candidate     the first sequence to align
    * @param knownSequence the second sequence to align
    * @return a Some of an alignment of the two sequences, or None if one coulnd't be found
    */
  def alignmentMatch(candidate: String, knownSequence: Sequence, allowedMistakes: Int): Option[SequenceAndError] = {
    assert(candidate.size == knownSequence.sequence.size, "This function is really only intended for sequences of the same length")

    val aligner = new VerySimpleNeedlemanWunsch(knownSequence.sequence)
    val alignment = aligner.alignedScore(candidate)

    // for our simple alignments we want to allow
    val allowedScore = aligner.errorsToScore(allowedMistakes)

    if (alignment >= allowedScore)
      Some(SequenceAndError(knownSequence, aligner.scoreToErrors(alignment)))
    else
      None
  }

  /**
    * a quick and dirty loookup function for all possible alternative bases.. there are better solutions than this
    *
    * @param base the base of interest
    * @return an Array of all alternative bases (the full set exclusive of the provided base)
    */
  def otherBases(base: Char): Array[Char] = base match {
    case 'A' => Array[Char]('C', 'G', 'T')
    case 'C' => Array[Char]('A', 'G', 'T')
    case 'G' => Array[Char]('C', 'A', 'T')
    case 'T' => Array[Char]('C', 'G', 'A')
    case _ => throw new IllegalStateException("Unable to process base: " + base)
  }
}