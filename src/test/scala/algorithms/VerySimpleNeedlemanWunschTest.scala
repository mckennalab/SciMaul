package algorithms

import algorithms.dynamic.VerySimpleNeedlemanWunsch
import org.scalatest.{FlatSpec, Matchers}

class VerySimpleNeedlemanWunschTest extends FlatSpec with Matchers {

  val seqA = "AAA"

  "VS Needleman Wunsch" should "align some simple sequences" in {
    val nmw = new VerySimpleNeedlemanWunsch(seqA)

    nmw.alignedScore("ATA") should be (1)
    nmw.alignedScore("AAA") should be (3)
    nmw.alignedScore("TTT") should be (-3)

  }

  "VS Needleman Wunsch" should "align some barcode problems we have" in {

    val nmw = new VerySimpleNeedlemanWunsch("CATACGGCGA")

    // CATAC GGCGA
    // CATACCGGCG
    //      *    *
    nmw.alignedScore("CATACCGGCG") should be (7)

  }

  "VS Needleman Wunsch" should "align some other barcode problems we have" in {

    val nmw = new VerySimpleNeedlemanWunsch("TGGGCTCTTC")

    // TGG GCTCTTC
    // TGGAGC CTTC
    //    *  *
    nmw.alignedScore("TGGAGCCTTC") should be (7)

  }

}