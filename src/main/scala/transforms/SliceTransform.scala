package transforms
import htsjdk.samtools.fastq.FastqRecord
import main.scala.ReadContainer
import recipe.ResolvedDimension

class SliceTransform(dim: ResolvedDimension) extends ReadTransform {
  val mDim = dim

  override def name: String = "{SL." + dim.name + "." + dim.read + "." + dim.start + "." + dim.length + "}"

  override def description: String = "SL." + dim.name + "." + dim.read + "." + dim.start + "." + dim.length

  override def transform(read: ReadContainer): TransforedReadAndDimension = {
    ReadContainer.sliceAndAnnototate(read,dim.start,dim.length,name,dim.read,keepSequenceInRead)
  }

  override def dimension: Option[ResolvedDimension] = Some(dim)

  override def isDimensioned: Boolean = SequenceType.isDimension(dim.typeOf)

  def keepSequenceInRead: Boolean = false
}


class SliceAndKeepTransform(dim: ResolvedDimension) extends SliceTransform(dim) {
  override def keepSequenceInRead: Boolean = false
}