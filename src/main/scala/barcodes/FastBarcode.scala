package barcodes

import barcodes.FastBarcode.{FastBarcode, FastMask}
import utils.BitEncoding

// the triplet of three barcodes that address a single cell. The well ID describes the well for
// each indexing step,
// type FastBarcode = Long

object FastBarcode {
  type FastBarcode = Long
  type FastMask = Long

  def mismatches(bc1: FastBarcode, bc2: FastBarcode, mask: FastMask): Int = {
    BitEncoding.mismatches(bc1,bc2)
  }

  def stringSizeToMask(stringSize: Int): FastMask = BitEncoding.mask(stringSize)

  /**
    * encode a fastbarcode from a sequence
    * @param str the string to encode
    * @return the FastBarcode, the FastMask, and how many errors (Ns) we found
    */
  def toFastBarcodeWithMismatches(str: String): BarcodeTrio = {
    // we need to remove any Ns in the read, count them as mismatches, and mask them out for comparison
    var mismatches = 0
    var mask = BitEncoding.mask(str.size)
    var newStr = str.replace('N','A') // we just put an arbitrary base in

    val ns = str.toUpperCase.zipWithIndex.filter{case(base,index) => (base == 'N')}.map{case(b,index) => index}.toArray
    ns.foreach{n => {
      mask = mask & BitEncoding.blankBaseMask(str.size,n)

    }}

    BarcodeTrio(BitEncoding.bitEncodeString(newStr),mask,ns.size)
  }

}

case class BarcodeTrio(barcode: FastBarcode, mask: FastMask, mismatches: Int)