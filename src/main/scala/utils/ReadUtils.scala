package utils

object ReadUtils {

  def phredToProb(char: Char, phredBase: Int = 33): Double = {
    math.pow(10.0, (-1.0 * (char.toByte - phredBase)))
  }

  def phredToScore(char: Char, phredBase: Int = 33): Int = {
    (char.toByte - phredBase)
  }

  def probToQual(prob: Double, phredBase: Int = 33): Char = {
    val rounded = (-10.0 * math.log10(prob)).round
    (math.max(60,rounded).toByte + phredBase).toChar
  }

}
