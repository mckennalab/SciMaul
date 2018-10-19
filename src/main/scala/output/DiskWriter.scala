package output

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
import org.apache.commons.io.FileUtils
import transforms.ReadPosition.ReadPosition
/*

commented out for now
class DiskWriter extends Actor {
  override def receive: Receive = {
    case e: OutputReads => BufferedOutputCell.writeReadToFastqFile(e.path,e.reads,e.read)
  }
}
*/
object DiskWriter {

  var maxOpenFiles = 1000

  val fileHandleLookup = new scala.collection.mutable.LinkedHashMap[String,BufferedWriter]()
  val lowToHighFileActivity = new collection.mutable.PriorityQueue[QueueOrder]()


  def write(out: OutputReads): Unit = {
    val outputString = BufferedOutputCell.readsToFastqString(out.reads,out.read)

    val path = out.path.getAbsolutePath
    if (!(fileHandleLookup contains path)) {
        while (lowToHighFileActivity.size >= maxOpenFiles) {
          val fileToDrop = lowToHighFileActivity.dequeue()
          fileHandleLookup(fileToDrop.path).flush()
          fileHandleLookup(fileToDrop.path).close()
          fileHandleLookup.remove(fileToDrop.path)
          //println("Dropping path " + fileToDrop.path + " with count " + fileToDrop.count + " size = " + lowToHighFileActivity.size)
        }
      lowToHighFileActivity.enqueue(new QueueOrder(path,out.reads.size))
      fileHandleLookup(path) = new BufferedWriter(new FileWriter(path,true))

    }
    fileHandleLookup(path).append(outputString)
  }

  def close(): Unit = {
    fileHandleLookup.foreach{case(fl,bw) => bw.flush(); bw.close()}
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