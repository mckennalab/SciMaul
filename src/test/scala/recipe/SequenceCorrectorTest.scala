package recipe

import algorithms.dynamic.VerySimpleNeedlemanWunsch
import org.scalatest.{FlatSpec, Matchers}
import recipe.sequence.{Sequence, SequenceCorrector}
import transforms.{ReadPosition, SequenceType}

class SequenceCorrectorTest extends FlatSpec with Matchers {

  val seqA = "AAA"

  "SequenceCorrector" should "find an alignment correctly" in {
    val matched = SequenceCorrector.alignmentMatch("CATACCGGCG", Sequence("test","CATACGGCGA"), 1)

    // CATACCGGCG
    // CATAC GGCGA
    //      *    *
    matched.isDefined should be (true)
    (matched.get.error) should be (1)
    (matched.get.sequence.sequence) should be ("CATACGGCGA")
  }

  "SequenceCorrector" should "score a double dropped base correctly" in {
    val drop = 2
    val dim1 = ResolvedDimension("dim1", ReadPosition.Index1, 0, 10, SequenceType.Index, Array[Sequence](Sequence("test1","ACGTACGTAC")), 1, true, "fake_mask", 2)

    val sc = new SequenceCorrector(dim1)

    val baseString = "XXGTACGTAC"
    val corrected = sc.correctSequence(baseString)

    corrected.isDefined should be (true)
    (corrected.get.sequence.sequence) should be ("GTACGTAC")
    (corrected.get.error) should be (0)
  }

  "SequenceCorrector" should "score a double dropped base correctly that has one error" in {
    val drop = 2
    val dim1 = ResolvedDimension("dim1", ReadPosition.Index1, 0, 10, SequenceType.Index, Array[Sequence](Sequence("test2","ACGTACGTAC")), 1, true, "fake_mask", 2)

    val sc = new SequenceCorrector(dim1)

    val baseString = "XXGTACGTAT"
    val corrected = sc.correctSequence(baseString)

    corrected.isDefined should be (true)
    (corrected.get.sequence.sequence) should be ("GTACGTAC")
    (corrected.get.error) should be (1)
  }

}