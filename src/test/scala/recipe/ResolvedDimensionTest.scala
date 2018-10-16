package recipe

import org.scalatest.{FlatSpec, Matchers}
import recipe.sequence.Sequence
import transforms.{ReadPosition, SequenceType}
import transforms.ReadPosition.ReadPosition
import transforms.SequenceType.SequenceType

class ResolvedDimensionTest extends FlatSpec with Matchers {
  "ResolvedDimension" should "get sorted correctly" in {

    var arrayOfDimensions = Array[ResolvedDimension]()

    val dim1 = ResolvedDimension("dim1", ReadPosition.Index1, 0, 10, SequenceType.Index, Array[Sequence](), 1)
    val dim2 = ResolvedDimension("dim2", ReadPosition.Index1, 10, 10, SequenceType.Index, Array[Sequence](), 1)

    arrayOfDimensions :+= dim1
    arrayOfDimensions :+= dim2

    val sortedVersion = arrayOfDimensions.sorted

    // we want to apply dim2 first, as it's later in the read (to dim1's offset values are still valid)
    ((sortedVersion(0).name) should be ("dim2"))
  }

  "ResolvedDimension" should "not mis-sort sequences on different reads" in {

    var arrayOfDimensions = Array[ResolvedDimension]()

    // now they're on different reads, and 'Read1' is higher in the enumerated order, so the sorted array should be the
    // same as the unsorted version
    val dim1 = ResolvedDimension("dim1", ReadPosition.Read1, 0, 10, SequenceType.Index, Array[Sequence](), 1)
    val dim2 = ResolvedDimension("dim2", ReadPosition.Index1, 10, 10, SequenceType.Index, Array[Sequence](), 1)

    arrayOfDimensions :+= dim1
    arrayOfDimensions :+= dim2

    val sortedVersion = arrayOfDimensions.sorted

    ((sortedVersion(0).name) should be ("dim1"))
  }
}