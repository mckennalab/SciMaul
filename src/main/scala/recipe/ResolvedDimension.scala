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
                             sequences: Array[Sequence],
                             maxError: Int,
                             allowAlignmentCorrection: Boolean,
                             mask: String,
                             drop: Int) extends Ordered[ResolvedDimension] {

  /**
    * we need the dimensions to be sorted, so that we always apply transforms within a read from right
    * to left. This way the indexes dont change as we mutate the read
    * @param that the other dimension
    * @return the int comparing the two dimensions
    */
  override def compare(that: ResolvedDimension): Int = {
    if (that.read == this.read)
      return that.start - this.start
    this.read.compare(that.read)
  }
}

object ResolvedDimension {

  /**
    * generate a ResolvedDimension object from a Dimension object
    * @param barcode the barcode object, read in from a Recipe file
    * @param sequences the sequences that go with that Barcode
    * @return a ResolvedDimension representation of this barcode and sequences
    */
  def apply(barcode: Dimension, sequences: Array[Sequence]): ResolvedDimension = {

    val mask = if (barcode.mask.isDefined) {
      barcode.mask.get
    } else {
      "*" * barcode.length
    }


    ResolvedDimension(barcode.name,
      ReadPosition.fromString(barcode.read),
      barcode.start,
      barcode.length,
      SequenceType.fromString(barcode.use),
      sequences,
      barcode.maxerror,
      barcode.align,
      mask,
      barcode.drop)
  }
}


