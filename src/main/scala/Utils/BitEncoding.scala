/*
 *
 *     Copyright (C) 2017  Aaron McKenna
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package Utils

import java.lang.{Long => JavaLong}
import java.math.BigInteger

import barcodes.FastBarcode.{FastBarcode, FastMask}

import scala.annotation.switch

/**
  * perform high-speed encoding and decoding of strings to longs
  */
object BitEncoding {

  var allComparisons = 0l

  val encodeA = 0x0
  val encodeC = 0x1
  val encodeG = 0x2
  val encodeT = 0x3

  val characterMask = 0x3

  val stringLimit = 24

  val stringMask =         0xFFFFFFFFFFFFFFFFl
  val upperBits =          0xAAAAAAAAAAAAAAAAl
  val stringMaskHighBits = 0xAAAAAAAAAAAAAAAAl
  val stringMaskLowBits =  0x5555555555555555l

  type TargetLong = Long


  def mask(stringSize: Int) = stringMask >> (64 - (stringSize * 2))

  /**
    * encode our target string and count into a 64-bit Long
    *
    * @param str the string to encode
    * @return the Long encoding of this string
    */
  def bitEncodeString(str: String): Long = {
    require(str.size <= 32, "String " + str + " is too long to be encoded (" + str.size + " > 32)")

    var encoding: Long = 0l

    str.toUpperCase.foreach { ch => {
      encoding = encoding << 2

      (ch: @switch) match {
        case 'A' => encoding = BitEncoding.encodeA | encoding
        case 'C' => encoding = BitEncoding.encodeC | encoding
        case 'G' => encoding = BitEncoding.encodeG | encoding
        case 'T' => encoding = BitEncoding.encodeT | encoding
        case _ => throw new IllegalStateException("Unable to encode character " + ch)
      }
    }
    }
    encoding
  }


  /**
    * decode the string and count into an object
    *
    * @param encoding the encoding as a long
    * @return an object representation
    */
  def bitDecodeString(encoding: Long, actualSize: Int): String = {
    val stringEncoding = new Array[Char](actualSize)

    (0 until actualSize).foreach { index => {
      (0x3 & (encoding >> (index * 2))) match {
        case BitEncoding.encodeA => stringEncoding(index) = 'A'
        case BitEncoding.encodeC => stringEncoding(index) = 'C'
        case BitEncoding.encodeG => stringEncoding(index) = 'G'
        case BitEncoding.encodeT => stringEncoding(index) = 'T'
      }
    }
    }
    stringEncoding.reverse.mkString("")
  }

  /**
    * add a counter value this this long's encoding
    *
    * @param encodedTarget the currently encoded target
    * @param count         the count to add
    * @return
    */
  def updateCount(encodedTarget: Long, count: Short): Long = {
    // now shift the counts to the top of the 64 bit encoding
    (encodedTarget & BitEncoding.stringMask) | ((count.toLong << 48))
  }

  // return the current count of a long-encoded string
  def getCount(encoding: Long): Short = (encoding >> 48).toShort


  /**
    * return the number of mismatches between two strings (encoded in longs), given both our set comparison mask
    * as well as an additional mask (optional). This uses the Java bitCount, which on any modern platform should call out
    * to the underlying POPCNT instructios. With bitshifting it's about 20X faster than any loop you'll come up with
    *
    * @param encoding1      the first string encoded as a long
    * @param encoding2      the second string
    * @param mask           consider only the specified bases
    * @return their differences, as a number of bases
    */
  def mismatches(encoding1: FastBarcode, encoding2: FastBarcode, mask: FastMask = BitEncoding.stringMask): Int = {
    BitEncoding.allComparisons += 1
    val firstComp = ((encoding1 ^ encoding2) & mask)
    java.lang.Long.bitCount((firstComp & BitEncoding.upperBits) | ((firstComp << 1) & BitEncoding.upperBits))

  }

}

