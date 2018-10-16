package output

import java.io.{File, PrintWriter}

import com.typesafe.scalalogging.LazyLogging
import main.scala.ReadContainer
import recipe._
import recipe.sequence.{Sequence, SequenceCorrector}
import transforms.ReadPosition.ReadPosition
import transforms.{ReadTransform, TransformFactory}

import scala.collection.mutable

class OutputManager(recipeContainer: RecipeContainer, basePath: File, bufferSize: Int, readTypes: Array[ReadPosition]) extends LazyLogging {

  // setup a cell container, where we map a dimension onto a cell
  val coordinateToCell = new mutable.LinkedHashMap[String,BufferedOutputCell]()
  val cellOfTheUnknown = new BufferedOutputCell(new Coordinate(Array[ResolvedDimension](),Array[Sequence]()),
    basePath,
    10000, // a larger buffer, as we'll write to this cell often
    readTypes,
    "unknownReads"
  )

  val dimensionToCorrectorAndTransform = recipeContainer.resolvedDimensions.map{dim => {
    (dim, new SequenceCorrector(dim),TransformFactory.dimensionToTransform(dim))
  }}.toArray


  // we want to know what dimension is failing to match up to our lists: here we setup recording the assigned reads per dimension
  val foundReadsCountsPerDimension = new mutable.LinkedHashMap[ResolvedDimension,Int]()
  dimensionToCorrectorAndTransform.map{ dim => {
    if (dim._3.isDimensioned)
      foundReadsCountsPerDimension(dim._1) = 0
  }}

  // for unassigned reads, in the end we want to print out the most common unassigned sequences we saw
  val confusingBarcodes = recipeContainer.resolvedDimensions.map{dim => {
    (dim, new mutable.LinkedHashMap[String,Int]())
  }}.toArray

  var unassignedReads = 0

  /**
    * add a read to the appropriate cell
    * @param readContainer
    */
  def addRead(readContainer: ReadContainer): Unit = {

    var read = readContainer

    val dimensions  = new mutable.ArrayBuffer[ResolvedDimension]()
    val coordinates = new mutable.ArrayBuffer[Sequence]()
    var readUnassigned = false

    var index = 0 // for speed, a while loop
    while (index < dimensionToCorrectorAndTransform.size) {

      if (dimensionToCorrectorAndTransform(index)._3.isDimensioned) {
        dimensions += dimensionToCorrectorAndTransform(index)._1

        val transformed = dimensionToCorrectorAndTransform(index)._3.transform(read)

        // correct the sequence to the known coordinate
        val seqOfInterest = transformed.sequence.get
        val corrected = dimensionToCorrectorAndTransform(index)._2.correctSequence(seqOfInterest) // a bit unsafe, but if it's dimensioned we should always have a sequence

        if (corrected.isDefined) {
          foundReadsCountsPerDimension(dimensionToCorrectorAndTransform(index)._1) += 1
          coordinates += corrected.get.sequence
        } else {
          confusingBarcodes(index)._2(seqOfInterest) = confusingBarcodes(index)._2.getOrElse(seqOfInterest,0) + 1
          readUnassigned = true
        }

      } else {
        dimensions += dimensionToCorrectorAndTransform(index)._1
        read = dimensionToCorrectorAndTransform(index)._3.transform(read).readContainer

      }

      index += 1
    }

    if (!readUnassigned) {

      // we now have a corrected cell ID, make a coordinate, ask for a string, and
      // output that cell
      val coordinate = new Coordinate(dimensions.toArray, coordinates.toArray)
      val coordinateString = coordinate.coordinateString()

      if (coordinateToCell contains coordinateString) {
        coordinateToCell(coordinateString).addRead(read)
      } else {
        val path = CellPathGenerator.outputPath(basePath, coordinate)
        coordinateToCell(coordinateString) = new BufferedOutputCell(coordinate, path, bufferSize, readTypes)
        coordinateToCell(coordinateString).addRead(read)
      }
    } else {
      unassignedReads += 1
      cellOfTheUnknown.addRead(read)
    }


  }

  def close(): Unit = {
    logger.info("Closing cell output files")
    coordinateToCell.foreach{case(id,cell) => cell.close()}
    cellOfTheUnknown.close()

    logger.info("Generating master statistics file")

    val output = new PrintWriter(basePath.getAbsoluteFile + File.separator + "runStatistics.txt")
    output.write("cell\tstatistic\tread\tvalue\n")

    coordinateToCell.foreach{case(id,cell) => {
      cell.stats.cellStats.foreach{st => {
        st.name.zip(st.stat).foreach{case(name,stat) => {
          output.write(cell.stats.name + "\t" + name + "\t" + st.read + "\t" + stat + "\n")
        }}
      }}
    }}
    cellOfTheUnknown.stats.cellStats.foreach{st => {
      st.name.zip(st.stat).foreach{case(name,stat) => {
        output.write("Unknown\t" + name + "\t" + stat + "\n")
      }}
    }}

    // output the top barcode
    confusingBarcodes.foreach{case(dim,seqMap) => {
      val sortedEntries = mutable.LinkedHashMap(seqMap.toSeq.filter{case(k,v) => v > 5000}.sortWith(_._2 > _._2):_*)
      sortedEntries.foreach{case(seq,count) => {
        output.write("UnknownIndex\t" + seq + "\t" + dim.read + "\t" + count + "\n")
      }}
    }}

    output.close()
  }
}
