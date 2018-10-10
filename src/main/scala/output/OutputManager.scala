package output

import java.io.File

import com.typesafe.scalalogging.LazyLogging
import main.scala.ReadContainer
import recipe._
import recipe.sequence.{Sequence, SequenceCorrector}
import transforms.ReadPosition.ReadPosition
import transforms.{ReadTransform, TransformFactory}

import scala.collection.mutable

class OutputManager(recipeContainer: RecipeContainer, basePath: File, bufferSize: Int, readTypes: Array[ReadPosition]) extends LazyLogging {

  // setup a cell container, where we map a dimension onto a cell
  val coordinateToCell = new mutable.HashMap[String,OutputCell]()
  val cellOfTheUnknown = new OutputCell(new Coordinate(Array[ResolvedDimension](),Array[Sequence]()),
    basePath,
    10000, // a larger buffer, as we'll write to this cell often
    readTypes,
    "unknownReads"
  )

  val dimensionToCorrector = new mutable.LinkedHashMap[ResolvedDimension, SequenceCorrector]()
  val dimensionToTransform = new mutable.LinkedHashMap[ResolvedDimension, ReadTransform]()

  recipeContainer.resolvedDimensions.foreach{dim => {
    dimensionToCorrector(dim) = new SequenceCorrector(dim)
    dimensionToTransform(dim) = TransformFactory.dimensionToTransform(dim)
  }}

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

    recipeContainer.resolvedDimensions.foreach{dim => {
      if (dimensionToTransform(dim).isDimensioned) {
        dimensions += dim
        val transform = dimensionToTransform(dim)

        val transformed = transform.transform(read)

        // correct the sequence to the known coordinate
        val corrected = dimensionToCorrector(dim).correctSequence(transformed.sequence.get) // a bit unsafe, but if it's dimensioned we should always have a sequence

        if (corrected.isDefined) {
          coordinates += corrected.get.sequence
        } else {
          readUnassigned = true
        }

      } else {
        dimensions += dim
        val transform = dimensionToTransform(dim)
        read = transform.transform(read).readContainer

      }
    }}

    if (!readUnassigned) {

      // we now have a corrected cell ID, make a coordinate, ask for a string, and
      // output that cell
      val coordinate = new Coordinate(dimensions.toArray, coordinates.toArray)
      val coordinateString = coordinate.coordinateString()

      if (coordinateToCell contains coordinateString) {
        coordinateToCell(coordinateString).addRead(read)
      } else {
        val path = CellPathGenerator.outputPath(basePath, coordinate)
        coordinateToCell(coordinateString) = new OutputCell(coordinate, path, bufferSize, readTypes)
        coordinateToCell(coordinateString).addRead(read)
      }
    } else {
      unassignedReads += 1
      if (unassignedReads % 1000 == 0) println("failed to assign " + unassignedReads + " reads so far")
      cellOfTheUnknown.addRead(read)
    }


  }

  def close(): Unit = {
    logger.info("CLosing cell output files")
    coordinateToCell.foreach{case(id,cell) => cell.close()}
    cellOfTheUnknown.close()
  }
}
