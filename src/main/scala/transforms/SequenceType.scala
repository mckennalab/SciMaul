package transforms

import transforms.ReadPosition._

object SequenceType extends Enumeration {
  type SequenceType  = Value
  val Index, UMI, Static = Value

  def fromString(str: String): SequenceType = str.toUpperCase match {
    case "INDEX" => Index
    case "UMI" => UMI
    case "STATIC" => Static
    case _ => throw new IllegalStateException("Cant parse SequenceType from: " + str)
  }

  def isDimension(typ: SequenceType): Boolean = typ match {
    case Index => true
    case UMI => false
    case Static => false
  }
}
