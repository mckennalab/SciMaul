package scala.output


import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import output.CellPathGenerator
import recipe.{Coordinate, ResolvedDimension}
import recipe.sequence.Sequence
import transforms.{ReadPosition, SequenceType}

class CellPathGeneratorTest extends FlatSpec with Matchers {
  "CellPathGenerator" should "generate a basic path correctly" in {

    val base = "test_data/test_base/"
    val baseDir = new File(base)
    val fakeDim1 = ResolvedDimension("name1",ReadPosition.Read1, 0, 10, SequenceType.Index, Array[Sequence](), 1, true, "", 0)
    val fakeDim2 = ResolvedDimension("name2",ReadPosition.Read1, 0, 10, SequenceType.Index, Array[Sequence](), 1, true, "", 0)
    val dims = Array[ResolvedDimension](fakeDim1,fakeDim2)

    val seq1 = Sequence("A1","AAAAAAAAAA")
    val seq2 = Sequence("A2","TTTTTTTTTT")
    val seqs = Array[Sequence](seq1,seq2)

    val fakeCoordinate = new Coordinate(dims,seqs)

    val path = CellPathGenerator.outputPath(baseDir,fakeCoordinate)
    val expectedResult = new File(base + File.separator + seq1.name + File.separator + seq2.name)

    ((path.getAbsolutePath) should be (expectedResult.getAbsolutePath))
  }
}