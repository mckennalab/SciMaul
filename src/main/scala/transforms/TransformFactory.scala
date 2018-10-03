package transforms

import recipe.{Dimension, ResolvedDimension}

object TransformFactory {

  /**
    * convert a dimension to a read transform
    * @param dim the dimension to use
    * @return the read transform
    */
  def dimensionToTransform(dim: ResolvedDimension): ReadTransform = {
    dim.typeOf match {
      case SequenceType.Index => new SliceTransform(dim)
      case SequenceType.UMI => new SliceTransform(dim)
      case SequenceType.Static => new SliceAndKeepTransform(dim)
      case _ => throw new IllegalStateException("Unable to process " + dim.typeOf)
    }
  }
}
