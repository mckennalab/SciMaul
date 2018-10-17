package output

import java.io.{File, PrintWriter}

import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
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
  //val system = ActorSystem("DiskWriterSystem")

  //val helloActor = system.actorOf(Props[DiskWriter], name = "DiskActor")

  def write(out: OutputReads): Unit = {
    //helloActor ! out
    BufferedOutputCell.writeReadToFastqFile(out.path,out.reads,out.read)
  }
}

case class OutputReads(path: File, reads: Array[ReadContainer], read: ReadPosition)