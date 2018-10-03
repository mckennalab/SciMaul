package output


sealed trait OutputType { def extension: String; def name: String  }

case object SAM extends OutputType { def extension = ".sam"; def name = "SAM" }
case object Fastq extends OutputType { def extension = ".sam"; def name = "SAM" }


