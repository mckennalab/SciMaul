package algorithms.dynamic

import breeze.linalg.DenseMatrix

/**
  * The most basic dynamic programming alignment -- the Needleman Wunsch global alignment with linear indel score.
  */
class NeedlemanWunsch(sequenceA: String, sequenceB: String, matchScore: Double, mismatchScore: Double, delScore: Double) {

  val matrix = new DenseMatrix[Double](sequenceA.size + 1, sequenceB.size + 1)
  val trace  = new TracebackMatrix(sequenceA.size + 1, sequenceB.size + 1)

  (1 until sequenceA.size + 1).foreach{ index1 => matrix(index1,0) = matrix(index1 - 1, 0) + delScore}
  (1 until sequenceB.size + 1).foreach{ index2 => matrix(0,index2) = matrix(0, index2 - 1) + delScore}

  // fill in the score matrix
  (1 until sequenceA.size + 1).foreach{ index1 => {
    (1 until sequenceB.size + 1).foreach {index2 => {

      val matchedScore = if (sequenceA(index1 - 1) == sequenceB(index2 - 1)) matchScore else mismatchScore

      val scores = Array[Double](
        matrix(index1 - 1, index2 - 1) + (matchedScore),
        matrix(index1, index2 - 1) + delScore,
        matrix(index1 - 1, index2) + delScore)

      val max = scores.max
      val index = scores.indexOf(max)

      matrix(index1, index2) = max
      //println("(" + index1 + "," + index2 + ") " + matchedScore + " max = " + max + " index = " + index + " scores " + scores.mkString(","))
      index match {
        case 0 => trace.set(index1, index2, Matched())
        case 1 => trace.set(index1, index2, GapA())
        case 2 => trace.set(index1, index2, GapB())
      }
    }}
  }}

  val emissionMap = EmissionState.knownStates.map{state => (state,matrix)}.toMap
  val traceMap = EmissionState.knownStates.map{state => (state,trace)}.toMap
  def emissionMapping(state: EmissionState): DenseMatrix[Double] = emissionMap(state.str)
  def traceMapping(state: EmissionState): TracebackMatrix = traceMap(state.str)

  def alignment: Alignment = TracebackMatrix.tracebackGlobalAlignment(sequenceA,sequenceB,emissionMapping,traceMapping)
}
