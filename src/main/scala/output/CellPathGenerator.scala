package output

import java.io.File

import recipe.{Coordinate, ResolvedDimension}

/**
  * Create an output path for individual cells
  * @param coordinates
  */
object CellPathGenerator {

  /**
    * create an output path for a cell given it's barcode coordinates
    * @param basePath the base directory
    * @param coordinate the coordinates (in barcode space)
    * @return a directory path to put output into
    */
  def outputPath(basePath: File, coordinate: Coordinate): File = {
    // return an output folder location for this cell
    assert(basePath.exists() && basePath.isDirectory, "The output path " + basePath + " doesn't exist or isn't a folder")

    var finalDir = basePath.getAbsolutePath
    val subDirs = coordinate.coordinates.map{seq => seq}.mkString(File.separator)
    val newDir = new File(finalDir + File.separator + subDirs)

    // if it doesn't exist, make it
    if (!newDir.exists())
        newDir.mkdirs()

    newDir
  }
}
