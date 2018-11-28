package main.scala

import java.io.File

import com.typesafe.scalalogging.LazyLogging
import input.SequenceGenerator
import org.slf4j.LoggerFactory
import output.{DiskWriter, OutputManager}
import picocli.CommandLine
import recipe.RecipeContainer
import transforms.ReadPosition
import transforms.ReadPosition.ReadPosition
import picocli.CommandLine._
import ch.qos.logback.classic.{Level, Logger}

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
@Command(name = "SCIMaul", version = Array("1.0"), sortOptions = false,
  description = Array("@|bold SCIMaul|@ @|underline Process single-cell sequencing reads into |@ single cells"))
class Main extends Runnable with LazyLogging {

  @Option(names = Array("-fq1", "--fastq1"), required = true, paramLabel = "FILE", description = Array("the first read fastq file"))
  private var fastq1: File = new File("")

  @Option(names = Array("-fq2", "--fastq2"), paramLabel = "FILE", description = Array("the second read fastq file"))
  private var fastq2: File = new File("")

  @Option(names = Array("-bc1", "--barcode1"), paramLabel = "FILE", description = Array("the first index fastq file"))
  private var barcode1: File = new File("")

  @Option(names = Array("-bc2", "--barcode2"), paramLabel = "FILE", description = Array("the second index fastq file"))
  private var barcode2: File = new File("")

  @Option(names = Array("-rc", "--recipe"), required = true, paramLabel = "FILE", description = Array("the recipe file"))
  private var recipeFile: File = new File("")

  @Option(names = Array("-out", "--outputDir"), required = true, paramLabel = "FILE", description = Array("the output directory"))
  private var outputDir: File = new File("")

  @Option(names = Array("-buffer", "--buffer"), paramLabel = "INT", description = Array("the number of reads we should buffer before writing to the output"))
  private var bufferSize: Int = 100

  @Option(names = Array("-maxFiles", "--maxFiles"), paramLabel = "INT", description = Array("the number of files we allow the filesystem to have open at one time"))
  private var fileHandleLimit: Int = 1000

  @Option(names = Array("-h", "--help"), usageHelp = true, description = Array("print this help and exit"))
  private var helpRequested: Boolean = false

  @Option(names = Array("-V", "--version"), versionHelp = true, description = Array("print version info and exit"))
  private var versionRequested: Boolean = false

  @Option(names = Array("-v", "--verbose"), description = Array("output additional processing information"))
  private var verbose: Boolean = false

  // *********************************************************************************
  // setup the logging
  System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT]:%4$s:(%2$s): %5$s%n")

  override def run(): Unit = {
    if (helpRequested) {
      new CommandLine(this).usage(System.err)
    } else if (versionRequested) {
      new CommandLine(this).printVersionHelp(System.err)
    } else {
      // setup the logging level
      if (verbose)
        LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger].setLevel(Level.DEBUG)
      else
        LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger].setLevel(Level.INFO)

      DiskWriter.maxOpenFiles = fileHandleLimit

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

        if (readsProcessed % 100000 == 0) {
          val dimensionMatchString = outputManager.foundReadsCountsPerDimension.map{case(dim,count) => dim.name + " = " + Main.quickRound(count,readsProcessed) + "%"}.mkString(", ")
          val unmatchedPCT = Main.quickRound(outputManager.unassignedReads ,readsProcessed)
          logger.info("Processed " + readsProcessed + " reads so far; " + (readsProcessed - outputManager.unassignedReads) +
            " reads were assigned (" + (100.0 - unmatchedPCT) + "%), assiged reads after each dimension: " + dimensionMatchString + "; error-correcting map size: " + outputManager.errorPoolSize())
        }
      }

      // close everything up
      outputManager.close()
      DiskWriter.close()
    }
  }

}


object Main {
  def main(args: Array[String]) {
    CommandLine.run(new Main(), System.err, args: _*)
  }

  def quickRound(v: Int, total: Int) = (((v.toDouble / total.toDouble) * 100).round / 100.toDouble) * 100.0
}