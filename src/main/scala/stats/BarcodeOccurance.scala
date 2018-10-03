package main.scala.stats

import java.io.{PrintWriter, File}
import java.util.logging.{Level, Logger}
import scala.collection.mutable

/**
 * created by aaronmck on 6/19/14
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
// a container for stats on barcode co-occurance and edit distances
case class BarcodeOccurance(barcodeList1: Option[Array[String]],barcodeList2: Option[Array[String]],maxEditDistance: Int) {
  val logger = Logger.getLogger("BarcodeOccurance")
  logger.setLevel(Level.INFO)

  val allBarcodeConst = "all"
  val unknown = "UNKNOWN"
  val editDistCount = maxEditDistance + maxEditDistance + 1

  var barcodes1 = barcodeList1.getOrElse(Array[String](allBarcodeConst))
  var barcodes2 = barcodeList2.getOrElse(Array[String](allBarcodeConst))
  barcodes1 :+= unknown
  barcodes2:+= unknown
  logger.info("first barcodes: " + barcodes1.mkString(","))
  logger.info("second barcodes: " + barcodes2.mkString(","))
  val editDistances = new Array[Array[Array[Int]]](barcodes1.length)
  for (i <- 0 until editDistances.length) {
    editDistances(i) = new Array[Array[Int]](barcodes2.length)
    for (j <- 0 until barcodes2.length) { editDistances(i)(j) = new Array[Int](editDistCount) }
  }

  // setup an unknown count bin
  val unknownCounts = new mutable.HashMap[String,Int]()

  def addEditDistance(barcode1: Option[String], barcode2: Option[String], editDist1: Int, editDist2: Int) {
    var pos1 = if (barcode1.isDefined) barcodes1.indexOf(barcode1.get) else barcodes1.indexOf(allBarcodeConst)
    var pos2 = if (barcode2.isDefined) barcodes2.indexOf(barcode2.get) else barcodes2.indexOf(allBarcodeConst)
    if (pos1 < 0) {
      pos1 = barcodes1.indexOf(unknown)
      if (!(unknownCounts contains barcode1.get))
        unknownCounts(barcode1.get) = 0
      unknownCounts(barcode1.get) += 1
    }
    if (pos2 < 0) {
      pos2 = barcodes2.indexOf(unknown)
      if (!(unknownCounts contains barcode2.get))
        unknownCounts(barcode2.get) = 0
      unknownCounts(barcode2.get) += 1
    }

    editDistances(pos1)(pos2)(editDist1+editDist2) += 1
  }

  def count(barcode1: Option[String], barcode2: Option[String]): Option[Array[Int]] = {
    var indx1 = if (barcode1.isDefined) barcodes1.indexOf(barcode1.get) else barcodes1.indexOf(allBarcodeConst)
    var indx2 = if (barcode2.isDefined) barcodes2.indexOf(barcode2.get) else barcodes2.indexOf(allBarcodeConst)
    if (indx1 < 0) indx1 = barcodes1.indexOf(unknown)
    if (indx2 < 0) indx2 = barcodes1.indexOf(unknown)
    Some(editDistances(indx1)(indx2))
  }

  def toFile(file: File) {
    val output = new PrintWriter(file.getAbsolutePath)
    output.write("barcode1\tbarcode2\t" + ((0 until editDistCount).map{"distance_" + _.toString}.mkString("\t")) + "\n")
    barcodes1.foreach(bcOne => {
      barcodes2.foreach(bcTwo => {
        output.write(bcOne + "\t" + bcTwo + "\t" + editDistances(barcodes1.indexOf(bcOne))(barcodes2.indexOf(bcTwo)).mkString("\t") + "\n")
      })
    })
    output.close()
  }

  def toUnknownFile(file: File) {
    val output = new PrintWriter(file.getAbsolutePath)
    output.write("barcode1\tcount\n")
    (unknownCounts.toList sortBy {_._2}).reverse.foreach{case(key,value) => output.write(key + "\t" + value + "\n")}
    output.close()
  }
}
