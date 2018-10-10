package recipe

import recipe.sequence.Sequence
import transforms.{ReadPosition, SequenceType}
import transforms.ReadPosition.ReadPosition
import transforms.SequenceType.SequenceType


case class ResolvedDimension(name: String,
                             read: ReadPosition,
                             start: Int,
                             length: Int,
                             typeOf: SequenceType,
                             sequences: Array[Sequence]) extends Ordered[ResolvedDimension] {

  /**
    * we need the dimensions to be sorted, so that we always apply transforms within a read from right
    * to left. This way the indexes dont change as we mutate the read
    * @param that the other dimension
    * @return the int comparing the two dimensions
    */
  override def compare(that: ResolvedDimension): Int = {
    if (that.read == this.read)
      return this.start - that.start
    this.read.compare(that.read)
  }
}

object ResolvedDimension {

  def apply(barcode: Dimension, sequences: Array[Sequence]): ResolvedDimension = {
    ResolvedDimension(barcode.name,
      ReadPosition.fromString(barcode.read),
      barcode.start,
      barcode.length,
      SequenceType.fromString(barcode.use),
      sequences)
  }
}


