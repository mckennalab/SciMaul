package output

import java.io.File

import main.scala.ReadContainer
import recipe.RecipeContainer
import transforms.ReadPosition.ReadPosition

trait OutputManager {
  def addRead(readContainer: ReadContainer): Unit
  def close(): Unit
  def outputSummaries(): Unit
  def errorPoolSize(): Int
}

object OutputManger {

}
