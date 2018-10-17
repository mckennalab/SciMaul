package recipe

import algorithms.dynamic.VerySimpleNeedlemanWunsch
import org.scalatest.{FlatSpec, Matchers}
import recipe.sequence.{Sequence, SequenceCorrector}
import utils.BitEncoding

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

}