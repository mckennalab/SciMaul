package output

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
import transforms.ReadPosition.ReadPosition
import utils.FileUtils

import scala.collection.mutable
import scala.io.Source

object DiskWriter {

  var maxOpenFiles = 1000

  val fileHandleLookup = new scala.collection.mutable.LinkedHashMap[String, BufferedWriter]()
  val lowToHighFileActivity = new collection.mutable.PriorityQueue[QueueOrder]()

  val everWritten = new mutable.HashMap[String,Boolean]()

  /**
    * write the read contents out to the specified file
    * @param out a container with the file to write to, the reads to write, and which position the file is (read1, etc)
    */
  def write(out: OutputReads): Unit = {

    val path = out.path.getAbsolutePath

    if (!(fileHandleLookup contains path)) {
      while (lowToHighFileActivity.size >= maxOpenFiles) {
        val fileToDrop = lowToHighFileActivity.dequeue()
        fileHandleLookup(fileToDrop.path).flush()
        fileHandleLookup(fileToDrop.path).close()
        fileHandleLookup.remove(fileToDrop.path)
      }
      lowToHighFileActivity.enqueue(new QueueOrder(path, out.reads.size))
      fileHandleLookup(path) = new BufferedWriter(new FileWriter(path, true))

    }
    BufferedOutputCell.writeFastqRecords(out.reads, out.read, fileHandleLookup(path))
    everWritten(path) = true
  }

  /**
    * close any and all remaining open files
    */
  def close(): Unit = {
    fileHandleLookup.foreach { case (fl, bw) => bw.flush(); bw.close() }
  }

}

case class OutputReads(path: File, reads: Array[ReadContainer], read: ReadPosition)

class QueueOrder(mPath: String, mCount: Int) extends Ordering[QueueOrder] {
  val path = mPath
  val count = mCount
  override def compare(x: QueueOrder, y: QueueOrder): Int = y.count - x.count
}
object QueueOrder {
  implicit def QueueOrderOrdering: Ordering[QueueOrder] = Ordering.by(-1 * _.count)
}