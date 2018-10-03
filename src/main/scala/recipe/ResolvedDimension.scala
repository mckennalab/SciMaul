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
                             sequences: Array[Sequence])

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


