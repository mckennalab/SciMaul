package output

import java.io.{File, PrintWriter}

import akka.actor.{Actor, ActorSystem, Props}
import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
import transforms.ReadPosition.ReadPosition

class DiskWriter extends Actor {
  override def receive: Receive = {
    case e: OutputReads => OutputCell.writeReadToFastqFile(e.path,e.reads,e.read)
  }
}

object DiskWriter {
  //val system = ActorSystem("DiskWriterSystem")

  //val helloActor = system.actorOf(Props[DiskWriter], name = "DiskActor")

  def write(out: OutputReads): Unit = {
    //helloActor ! out
    OutputCell.writeReadToFastqFile(out.path,out.reads,out.read)
  }
}

case class OutputReads(path: File, reads: Array[ReadContainer], read: ReadPosition)