package output

import java.io._
import java.util.zip.GZIPOutputStream

import com.typesafe.scalalogging.LazyLogging
import main.scala.ReadContainer
import recipe._
import recipe.sequence.{Sequence, SequenceCorrector}
import stats.CellStats
import transforms.ReadPosition.ReadPosition
import transforms.{ReadPosition, ReadTransform, TransformFactory}

import scala.collection.mutable


/**
  * this class manages a single output stream of reads
  * @param recipeContainer the recipe container
  * @param basePath the base path for cell output
  * @param bufferSize how many reads can a cell hold in memory?
  * @param readTypes the types of reads (READ1, READ2, etc)
  * @param keepUnassignedReads do we want to hold onto unassigned reads
  *
  *                            TODO: THIS class is way too complicated, and
  *                            needs to be cleaned out badly
  */
class SingleFileOutputManger(recipeContainer: RecipeContainer,
                             basePath: File,
                             readTypes: Array[ReadPosition],
                             keepUnassignedReads: Boolean,
                             outputAllReads: Boolean,
                             compressedOutput: Boolean) extends OutputManager with LazyLogging {

  // setup the output files
  val readOutput = readTypes.map{ readType => {
    val outputFile = new File(basePath + File.separator + BufferedOutputCell.cellName + BufferedOutputCell.suffixSeparator + ReadPosition.fileExtension(readType, true))
    assert(!outputFile.exists(), "We wont overwrite old data; please remove the existing data file: " + outputFile.getAbsolutePath + " (and probably others)")
    val out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile)), "UTF-8"))
    (readType, out)
  }}.toMap

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

  // keep a map of coordinates to CellStats for any read that we see
  val coordinateToCell = new mutable.LinkedHashMap[String,CellStats]()


  /**
    * add a read to the appropriate cell
    * @param readContainer
    */
  def addRead(readContainer: ReadContainer): Unit = {
    val dimensions  = new Array[ResolvedDimension](dimensionToCorrectorAndTransform.size)
    val coordinates = new Array[Sequence](foundReadsCountsPerDimension.size)
    var readUnassigned = false

    var read = readContainer

    var index = 0
    var coordinateDim = 0

    // for speed, a while loop
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

    if (!readUnassigned || outputAllReads) {
      // we now have a corrected cell ID, make a coordinate, ask for a string, and save stats on that cell
      val coordinate = new Coordinate(dimensions, coordinates)
      val coordinateString = coordinate.coordinateString()

      if (!(coordinateToCell contains coordinateString)) {
        coordinateToCell(coordinateString) = new CellStats(coordinate,readTypes)
      }
      coordinateToCell(coordinateString).addRead(read)

      val readArray = Array[ReadContainer](read)
      readOutput.foreach{case(position,writer) => BufferedOutputCell.writeFastqRecords(readArray, position, writer)}
    }
    if (readUnassigned)
      unassignedReads += 1
  }

  def errorPoolSize(): Int = {
    dimensionToCorrectorAndTransform.map{case(d,c,t) => c.sequenceMapping.size}.sum
  }

  def close(): Unit = {
    logger.info("Closing cell output files")
    readOutput.foreach{case(pos,writer) => writer.close()}
    outputSummaries()
  }

  def outputSummaries(): Unit = {
    logger.info("Generating master statistics file")

    val output = new PrintWriter(basePath.getAbsoluteFile + File.separator + "runStatistics.txt")
    output.write("cell\tstatistic\tread\tvalue\n")

    coordinateToCell.foreach{case(id,stats) => {
      stats.cellStats.foreach{st => {
        st.name.zip(st.stat).foreach{case(name,stat) => {
          output.write(stats.name + "\t" + name + "\t" + st.read + "\t" + stat + "\n")
        }}
      }}
    }}

    output.write("Unknown\treadCount\tall\t" + unassignedReads + "\n")

    output.close()
  }

  override def getReadsPerDimension: mutable.LinkedHashMap[ResolvedDimension, Int] = foundReadsCountsPerDimension

  override def unAssignedReadCount: Int = unassignedReads
}
