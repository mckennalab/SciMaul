package recipe

case class Dimension(name: String,
                     read: String,
                     start: Int,
                     length: Int,
                     use: String,
                     sequences: Option[String],
                     maxerror: Int = 1,
                     align: Boolean = false)
