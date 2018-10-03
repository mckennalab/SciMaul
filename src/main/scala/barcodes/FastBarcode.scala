package barcodes

import Utils.BitEncoding

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

  def toFastBarcode(str: String): FastBarcode = BitEncoding.bitEncodeString(str)
}