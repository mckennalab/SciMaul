package output

import java.io.{File, PrintWriter}
import com.typesafe.scalalogging.LazyLogging
import main.scala.ReadContainer
import recipe._
import recipe.sequence.{Sequence, SequenceCorrector}
import transforms.ReadPosition.ReadPosition
import transforms.{ReadTransform, TransformFactory}
import scala.collection.mutable


/**
  * this class manages the numerous cell read containers
  * @param recipeContainer the recipe container
  * @param basePath the base path for cell output
  * @param bufferSize how many reads can a cell hold in memory?
  * @param readTypes the types of reads (READ1, READ2, etc)
  * @param keepUnassignedReads do we want to hold onto unassigned reads
  *
  *                            TODO: THIS class is way too complicated, and
  *                            needs to be cleaned out badly
  */
class MultiFileOutputManager(recipeContainer: RecipeContainer,
                             basePath: File,
                             bufferSize: Int,
                             readTypes: Array[ReadPosition],
                             keepUnassignedReads: Boolean,
                             outputAllReads: Boolean) extends OutputManager with LazyLogging {

  // setup a cell containers
  val coordinateToCell = new mutable.LinkedHashMap[String,BufferedOutputCell]()
  val cellOfTheUnknown = new BufferedOutputCell(new Coordinate(Array[ResolvedDimension](),Array[Sequence]()),
    basePath,
    10000, // a larger buffer, as we'll write to this cell often
    readTypes,
    "unknownReads"
  )

  // map each of the dimensions we have to a sequence corrector and a transform on the read sequence
  val dimensionToCorrectorAndTransform = recipeContainer.resolvedDimensions.map{dim => {
    (dim, new SequenceCorrector(dim),TransformFactory.dimensionToTransform(dim))
  }}.toArray


  // counts reads that pass filter per dimension
  val foundReadsCountsPerDimension = new mutable.LinkedHashMap[ResolvedDimension,Int]()
  dimensionToCorrectorAndTransform.map{ dim => {
    if (dim._3.isDimensioned)
      foundReadsCountsPerDimension(dim._1) = 0
  }}

  // keep a simple tally
  var unassignedReads = 0



  /**
    * add a read to the appropriate cell
    * @param readContainer
    */
  def addRead(readContainer: ReadContainer): Unit = {
    val dimensions  = new Array[ResolvedDimension](dimensionToCorrectorAndTransform.size)
    val coordinates = new Array[Sequence](foundReadsCountsPerDimension.size)
    var readUnassigned = false

    var read = readContainer

    var index = 0 // for speed, a while loop
    var coordinateDim = 0 // for speed, a while loop
    while (index < dimensionToCorrectorAndTransform.size && !readUnassigned) {

      if (dimensionToCorrectorAndTransform(index)._3.isDimensioned) {
        dimensions(index) = dimensionToCorrectorAndTransform(index)._1

        val sliced = dimensionToCorrectorAndTransform(index)._3.transform(read)

        // correct the sequence to the known coordinate
        // a bit unsafe, but if it's dimensioned we should always have a sequence
        val corrected = dimensionToCorrectorAndTransform(index)._2.correctSequence(sliced)

        if (corrected.isDefined) {
          foundReadsCountsPerDimension(dimensionToCorrectorAndTransform(index)._1) += 1
          coordinates(coordinateDim) = corrected.get.sequence
          coordinateDim += 1
        } else {
          readUnassigned = true
        }

      } else {
        dimensions(index) = dimensionToCorrectorAndTransform(index)._1
        dimensionToCorrectorAndTransform(index)._3.transform(read)
      }

      index += 1
    }

    if (!readUnassigned) {

      // we now have a corrected cell ID, make a coordinate, ask for a string, and output that cell
      val coordinate = new Coordinate(dimensions, coordinates)
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
      if (keepUnassignedReads) {
        cellOfTheUnknown.addRead(read)
      }
    }


  }

  def errorPoolSize(): Int = {
    dimensionToCorrectorAndTransform.map{case(d,c,t) => c.sequenceMapping.size}.sum
  }

  def close(): Unit = {
    logger.info("Closing cell output files")
    coordinateToCell.foreach{case(id,cell) => cell.close()}
    if (keepUnassignedReads)
      cellOfTheUnknown.close(outputAllReads)
    outputSummaries()
  }

  def outputSummaries(): Unit = {
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
        output.write("Unknown\t" + name + "\tall\t" + stat + "\n")
      }}
    }}
    output.close()
  }

}
