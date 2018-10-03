package scala.output


import java.io.File

import barcodes.FastBarcode
import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
import org.scalatest.{FlatSpec, Matchers}
import output.CellPathGenerator
import recipe.{Coordinate, ResolvedDimension}
import recipe.sequence.Sequence
import transforms.{ReadPosition, SequenceType}
import transforms.ReadPosition._
import transforms.SequenceType._

class CellPathGeneratorTest extends FlatSpec with Matchers {
  "CellPathGenerator" should "generate a basic path correctly" in {

    val base = "test_data/test_base/"
    val baseDir = new File(base)
    val fakeDim1 = ResolvedDimension("name1",ReadPosition.Read1, 0, 10, SequenceType.Index, Array[Sequence]())
    val fakeDim2 = ResolvedDimension("name2",ReadPosition.Read1, 0, 10, SequenceType.Index, Array[Sequence]())
    val dims = Array[ResolvedDimension](fakeDim1,fakeDim2)

    val seq1 = Sequence("A1","AAAAAAAAAA",FastBarcode.toFastBarcode("AAAAAAAAAA"))
    val seq2 = Sequence("A2","TTTTTTTTTT",FastBarcode.toFastBarcode("TTTTTTTTTT"))
    val seqs = Array[Sequence](seq1,seq2)

    val fakeCoordinate = new Coordinate(dims,seqs)

    val path = CellPathGenerator.outputPath(baseDir,fakeCoordinate)
    val expectedResult = new File(base + File.separator + seq1.sequence + File.separator + seq2.sequence)

    ((path.getAbsolutePath) should be (expectedResult.getAbsolutePath))
  }
}