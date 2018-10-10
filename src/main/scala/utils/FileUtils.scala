package utils

import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

object FileUtils {
  // read in compressed input and output streams with scala source commands
  def gis(s: String) = new GZIPInputStream(new BufferedInputStream(new FileInputStream(s)))
  def gosAppend(s: String) = new PrintWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(s, true))))
  def gos(s: String) = new PrintWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(s, false))))

}
