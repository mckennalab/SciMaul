package algorithms

import net.sf.picard.fastq.FastqRecord

/**
 * created by aaronmck on 2/13/14
 *
 * Copyright (c) 2014, aaronmck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 
 * 2.  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 *
 */
case class BarcodeEditDistance(barcodes: Array[String], trimDownToSize: Boolean = true) {
  val unknown = "UNKNOWN"

  def distance(barcode: String): Tuple2[String, Int] = {
    var bestDist = Int.MaxValue
    var bestCode = unknown
    barcodes.foreach(code => {
      var cnt = 0

      // match from the front, we allow N's or shorter specified barcodes (the equivilent of N's to the length of the barcode)
      code.zipWithIndex.map {
        case (base, index) => if (base != 'N' && barcode(index) != base) cnt += 1
      }
      if (cnt < bestDist) {
        bestDist = cnt
        bestCode = code
      }

      // check the reverse complement
      cnt = 0
      BarcodeEditDistance.reverseComp(code).zipWithIndex.map {
        case (base, index) => if (base != 'N' && barcode(index) != base) cnt += 1
      }
      if (cnt < bestDist) {
        bestDist = cnt
        bestCode = code
      }
    })
    return (bestCode, bestDist)
  }
}

object BarcodeEditDistance {
  /**
   * reverse complement the bases
   * @param base the bases to reverse compliment
   * @return the reverse comp
   */
  def reverseComp(base: String): String = {
    var strng = ""
    base.toUpperCase.reverse.foreach(i => {
      if (i == 'A') strng += 'T'
      else if (i == 'T') strng += 'A'
      else if (i == 'C') strng += 'G'
      else if (i == 'G') strng += 'C'
      else strng += i
    })
    return strng
  }

  /**
   * find the best match between a barcode and all the known candidates.  Return the best match and it's
   * edit distance to the best match
   * @param barcodeRead the barcode sequence read from the barcode fastq file
   * @param barcodes the barcode candidate list, specified by the user
   * @return a tuple of the best match (if it exists) and it's distance
   */
  def distance(barcodeRead: Option[FastqRecord], barcodes: Array[String]): Tuple2[Option[String], Int] = {
    var bestDist = Int.MaxValue
    var bestCode: Option[String] = None
    if (!barcodeRead.isDefined) return (bestCode, bestDist)
    val barcode = barcodeRead.get.getReadString

    barcodes.foreach(code => {
      var cnt = 0

      // N's are treated as mismatches, if the barcode is shorter than the sequence we call the remaining mismatches
      code.zipWithIndex.map { case (base, index) => if (base != 'N' && barcode(index) != base) cnt += 1}
      if (cnt < bestDist) {
        bestDist = cnt
        bestCode = Some(code)
      }

      // check the reverse complement
      cnt = 0
      reverseComp(code).zipWithIndex.map {case (base, index) => if (base != 'N' && barcode(index) != base) cnt += 1}
      if (cnt < bestDist) {
        bestDist = cnt
        bestCode = Some(code)
      }
    })
    return (bestCode, bestDist)
  }

  // return None (Scala None) if they want to use all the barcodes or an option of strings otherwise
  def parseBarcodes(barcodes: String) : Option[Array[String]] = {
    val sp = barcodes.stripPrefix("\"").stripSuffix("\"").split(",")
    if (sp.length == 1 && sp(0).toLowerCase == "all")
      return None
    else
      return Some(sp)
  }
}
