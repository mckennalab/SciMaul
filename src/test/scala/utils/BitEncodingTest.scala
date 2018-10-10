package utils


import java.io.File

import barcodes.FastBarcode
import org.scalatest.{FlatSpec, Matchers}
import output.CellPathGenerator
import recipe.{Coordinate, ResolvedDimension}
import recipe.sequence.Sequence
import transforms.{ReadPosition, SequenceType}


/**
  * test that we can encode and decode string and counts to a binary Long -- most of this code
  * has been previously tested, so we're just testing new additions right now
  */
class BitEncodingTest extends FlatSpec with Matchers {
  "BitEncoding" should "make a single base mask correctly" in {

    val str1 = "AACCTTGA"
    val str1BE = BitEncoding.bitEncodeString(str1)

    val str2 = "TACCATGG"
    val str2BE = BitEncoding.bitEncodeString(str2)

    val mask = BitEncoding.blankBaseMask(str1.size,4)

    ((BitEncoding.mismatches(str1BE,str2BE,mask)) should be (2))

    val mask2 = BitEncoding.blankBaseMask(str1.size,7)
    ((BitEncoding.mismatches(str1BE,str2BE,mask & mask2)) should be (1))

    val mask3 = BitEncoding.blankBaseMask(str1.size,0)
    ((BitEncoding.mismatches(str1BE,str2BE,mask & mask2 & mask3)) should be (0))
  }


}