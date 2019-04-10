package transforms

/**
  * an Enumeration for read name / positions. The name isn't great,
  * but it doesn't conflict with a number of other 'ReadType' definitions
  */
object ReadPosition extends Enumeration {
  type ReadPosition = Value
  val Read1 = Value("read1")
  val Read2 = Value("read2")
  val Index1 = Value("index1")
  val Index2 = Value("index2")
  val All = Value("all")

  def fromString(str: String): ReadPosition = str.toUpperCase match {
    case "READ1" => Read1
    case "READ2" => Read2
    case "INDEX1" => Index1
    case "INDEX2" => Index2
    case "ALL" => All
    case _ => throw new IllegalStateException("Cant parse ReadPosition from: " + str)
  }

  def fileExtension(readPosition: ReadPosition, isCompressed: Boolean): String = readPosition match {
    case Read1 => "read1.fq" + (if (isCompressed) ".gz" else "")
    case Read2 => "read2.fq" + (if (isCompressed) ".gz" else "")
    case Index1 => "index1.fq" + (if (isCompressed) ".gz" else "")
    case Index2 => "index2.fq" + (if (isCompressed) ".gz" else "")
    case All => "all.fq" + (if (isCompressed) ".gz" else "")
  }
}
