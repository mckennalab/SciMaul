package main.scala

import java.io.{BufferedInputStream, File, PrintWriter}
import java.util.logging.{Level, Logger}

import input.SequenceGenerator
import output.OutputManager
import recipe.RecipeContainer
import transforms.ReadPosition
import transforms.ReadPosition.ReadPosition
import picocli.CommandLine._

import scala.collection.mutable

/**
  * created by aaronmck on October 9th, 2018
  *
  * Copyright (c) 2018, aaronmck
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
@Command(name = "SCIMaul", version = Array("1.0"),
  description = Array("@|bold SCIMaul|@ @|underline Process single-cell sequencing reads into |@ single cells"))
object Main extends App {

  @Option(names = Array("-fq1", "--fastq1"), required = true, paramLabel = "FILE", description = Array("the first fastq file"))
  private var fastq1: File = new File("")

  @Option(names = Array("-fq2", "--fastq2"), paramLabel = "FILE", description = Array("the count"))
  private var fastq2: File = new File("")

  @Option(names = Array("-bc1", "--barcode1"), paramLabel = "FILE", description = Array("the count"))
  private var barcode1: File = new File("")

  @Option(names = Array("-bc1", "--barcode2"), paramLabel = "FILE", description = Array("the count"))
  private var barcode2: File = new File("")

  @Option(names = Array("-rc", "--recipe"), required = true, paramLabel = "FILE", description = Array("the count"))
  private var recipeFile: File = new File("")

  @Option(names = Array("-out", "--outputDir"), required = true, paramLabel = "FILE", description = Array("the count"))
  private var outputDir: File = new File("")

  @Option(names = Array("-buffer", "--outputDir"), paramLabel = "FILE", description = Array("the count"))
  private var bufferSize: Int = 100

  // *********************************************************************************
  // setup the logging
  System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT]:%4$s:(%2$s): %5$s%n")
  val logger = Logger.getLogger("SCIMaul")
  logger.setLevel(Level.INFO)

  // *********************************************************************************
  // setup the inputs, mapping the read type to the input file if it exists
  val readTypeToFile = mutable.LinkedHashMap[ReadPosition, File]()
  readTypeToFile(ReadPosition.Read1) = fastq1
  if (fastq2.exists()) readTypeToFile(ReadPosition.Read2) = fastq2
  if (barcode1.exists()) readTypeToFile(ReadPosition.Index1) = barcode1
  if (barcode2.exists()) readTypeToFile(ReadPosition.Index2) = barcode2

  // *********************************************************************************
  // process the input reads into cells
  val reads = new SequenceGenerator(readTypeToFile)

  // read in the recipe file, and load up any barcode definition files
  val recipeManager = new RecipeContainer(recipeFile.getAbsolutePath)

  // create an output object
  val outputManager = new OutputManager(recipeManager, outputDir, bufferSize, readTypeToFile.keysIterator.toArray)

  // create read transforms from the recipe we've loaded
  var readsProcessed = 0
  while (reads.hasNext) {
    val nextRead = reads.next()
    outputManager.addRead(nextRead)
    readsProcessed += 1
    if (readsProcessed % 100000 == 0) logger.info("Processed " + readsProcessed + " reads so far")
  }

  // close everything up
  outputManager.close()

}

