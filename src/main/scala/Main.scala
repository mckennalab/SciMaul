package main.scala

import _root_.algorithms.BarcodeEditDistance
import _root_.algorithms.stats.OverlapCounts
import java.io.{BufferedInputStream, File, PrintWriter}

import net.sf.picard.fastq.{FastqReader, FastqRecord, FastqWriter, FastqWriterFactory}
import java.util.logging.SimpleFormatter

import main.scala.stats._
import java.util.logging.{Level, Logger}

import input.SequenceGenerator
import main.scala.algorithms.ReadTrimmer
import recipe.RecipeContainer
import transforms.ReadPosition
import transforms.ReadPosition.ReadPosition

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
object Main extends App {
  val NOTAREALFILENAME = "/0192348102jr10234712930h8j19p0hjf129-348h512935"
  // please don't make a file with this name
  val NOTAREALFILE = new File(NOTAREALFILENAME)

  // parse the command line arguments
  val parser = new scopt.OptionParser[Config]("Maul") {
    head("Maul", "1.0")

    // *********************************** Read Inputs *******************************************************
    opt[File]("inFQ1") required() valueName ("<file>") action { (x, c) => c.copy(inFastq1 = x)} text ("out first end reads FASTQ")
    opt[File]("inFQ2") valueName ("<file>") action { (x, c) => c.copy(inFastq2 = x)} text ("the second end reads FASTQ")
    opt[File]("inBarcodeFQ1") valueName ("<file>") action { (x, c) => c.copy(inBarcodeFQ1 = x)} text ("The fastq file with the first set of barcodes")
    opt[File]("inBarcodeFQ2") valueName ("<file>") action { (x, c) => c.copy(inBarcodeFQ2 = x)} text ("The fastq file with the second set of barcodes")
    opt[File]("recipe") valueName ("<file>") action { (x, c) => c.copy(recipe = x)} text ("A file containing the RT barcodes")
    opt[File]("outputDirectory") required() valueName ("<file>") action { (x, c) => c.copy(outDir = x)} text ("where to place calls for each cell, in their own folder")

    // some general command-line setup stuff
    note("Split the reads into output fastq files\n")
    help("help") text ("prints the usage information you see here")
  }

  // *********************************** Run *******************************************************
  // run the actual read processing -- our argument parser found all of the parameters it needed
  parser.parse(args, Config()) map {
    config => {
      // WHY IS setting up logging the worst part of this whole program? Still too much Java left in Scala...
      System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT]:%4$s:(%2$s): %5$s%n")
      val logger = Logger.getLogger("MainProcessing")
      logger.setLevel(Level.INFO)

      val perWhat = 1000000 // output status for every X reads -- the associated message text may have to change as well

      // setup the input reader
      val reads = new SequenceGenerator(Map[ReadPosition,File](
        (ReadPosition.Read1  -> config.inFastq1),
        (ReadPosition.Read2  -> config.inFastq2),
        (ReadPosition.Index1 -> config.inBarcodeFQ1),
        (ReadPosition.Index2 -> config.inBarcodeFQ2)))

      // read in the recipe file, and load up any barcode definition files
      val recipe = new RecipeContainer(config.recipe.getAbsolutePath)

      // create an output object


      // create read transforms from the recipe we've loaded




      // process each read into the output object

    }
  } getOrElse {
    Console.println("Unable to parse the command line arguments you passed in, please check that your parameters are correct")
  }

}

/*
 * the configuration class, it stores the user's arguments from the command line, also set defaults here
 */
case class Config(inFastq1: File = new File(Main.NOTAREALFILENAME),
                  inFastq2: File = new File(Main.NOTAREALFILENAME),
                  inBarcodeFQ1: File = new File(Main.NOTAREALFILENAME),
                  inBarcodeFQ2: File = new File(Main.NOTAREALFILENAME),
                  outDir: File = new File(Main.NOTAREALFILENAME),
                  recipe: File = new File(Main.NOTAREALFILENAME)
                  )

