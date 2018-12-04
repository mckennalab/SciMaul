package algorithms.dynamic

/**
  * This 'very simple' Needleman Wunsch makes the following assumptions:
  * - We want to align 'totalAligned' bases of the sequence, and gaps and mismatches cost 0
  * - Aligned bases score 1
  * - We don't care about the aligned sequence, just the score
  * @param sequenceSize
  */
class VerySimpleNeedlemanWunsch(seqA: String) {

  @inline def setXY(x: Int, y: Int, value: Int) = { alignedMatrix(x + (matrixSide * y)) = value }
  @inline def getXY(x: Int, y: Int) = alignedMatrix(x + (matrixSide * y))

  val sequenceA = seqA
  val matrixSide = sequenceA.size + 1
  val alignedMatrix = new Array[Int]((matrixSide) * (matrixSide))

  val sequenceAArray = sequenceA.toCharArray

  def errorsToScore(error: Int): Int = sequenceA.size - ((error * 2) + 1) // allow a mismatch + one shift
  def scoreToErrors(score: Int): Int = (math.floor(sequenceA.size - score) / 2.0).toInt // allow a mismatch + one shift

  def alignedScore(sequenceB: String, matched: Int = 1, error: Int = -1): Int = {
    val sequenceBArray = sequenceB.toCharArray

    val scoreArray = new Array[Int](3)

    var index1 = 0
    var index2 = 0
    while (index1 < sequenceAArray.size) {
      index2 = 0
      while (index2 < sequenceBArray.size) {
        scoreArray(0) = getXY(index1,index2) + (if (sequenceAArray(index1) == sequenceBArray(index2)) matched else error)
        scoreArray(1) = getXY(index1 + 1,index2) + error
        scoreArray(2) = getXY(index1,index2 + 1) + error

        // weirdly finding the max of scorearray was taking a lot of time, use a complex if to make it faster
        if (scoreArray(0) > scoreArray(1))
          if (scoreArray(0) > scoreArray(2))
            setXY(index1 + 1,index2 + 1,scoreArray(0))
          else
            setXY(index1 + 1,index2 + 1,scoreArray(2))
        else
          if (scoreArray(1) > scoreArray(2))
            setXY(index1 + 1,index2 + 1,scoreArray(1))
          else
            setXY(index1 + 1,index2 + 1,scoreArray(2))

        index2 += 1
      }
      index1 += 1
    }
    getXY(index1,index2)
  }
}
