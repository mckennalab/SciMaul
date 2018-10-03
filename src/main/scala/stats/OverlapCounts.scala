package algorithms.stats

import scala.collection.mutable
import java.io.{PrintWriter, File}
import scala.collection.immutable.TreeMap

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
class OverlapCounts {
  val overlaps = new mutable.HashMap[String,mutable.HashMap[Int,Int]]()
  val mergedLengths = new mutable.HashMap[String,mutable.HashMap[Int,Int]]()
  var maxReadLength = 0
  /**
   * add a read overlap to our counts
   *
   * @param barcode the barcode we associate this read with
   * @param overlap the amount of overlap betwen the reads
   * @param read1Length the length of the first read, trimmed of any adapters
   * @param read2Length the length of the second read, trimmed of any adapters
   */
  def addRead(barcode: String, overlap: Int, read1Length: Int, read2Length: Int) {
    if (read1Length > maxReadLength || read2Length > maxReadLength) maxReadLength = math.max(read1Length,read2Length)

    if (!(overlaps contains barcode)) {
      overlaps(barcode) = new mutable.HashMap[Int,Int]()
      mergedLengths(barcode) = new mutable.HashMap[Int,Int]()
    }
    if (!(overlaps(barcode) contains overlap)) {
      overlaps(barcode)(overlap) = 1
    } else {
      overlaps(barcode)(overlap) += 1
    }
    val mergedLength = (read1Length + read2Length) - overlap
    if (!(mergedLengths(barcode) contains mergedLength))
      mergedLengths(barcode)(mergedLength) = 1
    else
      mergedLengths(barcode)(mergedLength) += 1
  }

  def writeToFile(outputOverlap: File) {
    val outputOv = new PrintWriter(outputOverlap)
    outputOv.write("barcode\toverlap\tcount\n")

    // start at zero, go to the maximum read length we've see

    overlaps.map{case(barcode,mp) => {
      //val newMap1 = TreeMap[Int,Int](mp.toArray:_*)
      //newMap1.map{case(key,value) => outputOv.write(barcode + "\t" + key + "\t" + value + "\n")}
      for (i <- 0 until maxReadLength + 1) {
        var count = 0
        if (mp contains i) count = mp(i)
          outputOv.write(barcode + "\t" + i + "\t" + count + "\n")
      }
    }}
    outputOv.close()

  }


}
