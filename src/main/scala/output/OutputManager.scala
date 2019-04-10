package output

import java.io.File

import main.scala.ReadContainer
import recipe.{RecipeContainer, ResolvedDimension}
import transforms.ReadPosition.ReadPosition

import scala.collection.mutable

trait OutputManager {
  def addRead(readContainer: ReadContainer): Unit
  def close(): Unit
  def outputSummaries(): Unit
  def errorPoolSize(): Int
  def getReadsPerDimension: mutable.LinkedHashMap[ResolvedDimension,Int]
  def unAssignedReadCount: Int
}

object OutputManger {

}
