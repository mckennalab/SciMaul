package output

import java.io.File

import main.scala.ReadContainer
import recipe._
import recipe.sequence.{Sequence, SequenceCorrector}
import transforms.ReadPosition.ReadPosition
import transforms.{ReadTransform, TransformFactory}

import scala.collection.mutable

class OutputManager(recipeContainer: RecipeContainer, basePath: File, bufferSize: Int, readType: Array[ReadPosition]) {

  // setup a cell container, where we map a dimension onto a cell
  val coordinateToCell = new mutable.HashMap[String,OutputCell]()

  val dimensionToCorrector = new mutable.LinkedHashMap[ResolvedDimension, SequenceCorrector]()
  val dimensionToTransform = new mutable.LinkedHashMap[ResolvedDimension, ReadTransform]()

  recipeContainer.resolvedDimensions.foreach{dim => {
    dimensionToCorrector(dim) = new SequenceCorrector(dim)
    dimensionToTransform(dim) = TransformFactory.dimensionToTransform(dim)
  }}

  /**
    * add a read to the appropriate cell
    * @param readContainer
    */
  def addRead(readContainer: ReadContainer): Unit = {

    var read = readContainer

    val dimensions  = new mutable.ArrayBuffer[ResolvedDimension]()
    val coordinates = new mutable.ArrayBuffer[Sequence]()

    recipeContainer.resolvedDimensions.foreach{dim => {
      if (dimensionToTransform(dim).isDimensioned) {
        dimensions += dim
        val transform = dimensionToTransform(dim)

        val transformed = transform.transform(read)

        // correct the sequence to the known coordinate
        val corrected = dimensionToCorrector(dim).correctSequence(transformed.sequence)

        coordinates += corrected.sequence

      } else {
        dimensions += dim
        val transform = dimensionToTransform(dim)
        read = transform.transform(read).readContainer
      }
    }}

    // we now have a corrected cell ID, make a coordinate, ask for a string, and
    // output that cell
    val coordinate = new Coordinate(dimensions.toArray,coordinates.toArray)
    val coordinateString = coordinate.coordinateString()

    if (coordinateToCell contains coordinateString)
      coordinateToCell(coordinateString).addRead(read)
    else {
      val path = CellPathGenerator.outputPath(basePath,coordinate)
      coordinateToCell(coordinateString) = new OutputCell(coordinate,path,bufferSize,readType)
    }
  }
}
