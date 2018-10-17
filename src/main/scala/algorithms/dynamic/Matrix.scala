package algorithms.dynamic


import breeze.linalg.DenseMatrix

object Matrix {

  /**
    * pad out an integer to a set string width with left alignment
    * @param i the integer to pad
    * @param padv the amount of padding
    * @return a left aligned string
    */
  def padInt(i: Int, padv: Int): String = {
    String.format("%1$" + padv + "s", i.toString)
  }

  /**
    * pad out an double to a set string width with left alignment
    * @param i the double to pad
    * @param padv the amount of padding
    * @return a left aligned string
    */
  def padDouble(i: Double, padv: Int): String = {
    " " + String.format("%1$" + padv + "s", i.toString).slice(0,6)
  }

  /**
    * pad a string with left alignment
    * @param st the string to pad
    * @param padv the amount of padding
    * @return a left aligned string
    */
  def padString(st: String, padv: Int): String = {
    String.format("%1$" + padv + "s", st)
  }

  /**
    * print out a matrix to the console
    * @param matrix the matrix to print
    * @param seq1 the row sequence
    * @param seq2 the column sequence
    * @param pad the padding
    */
  def printMatrix(matrix: DenseMatrix[Double], seq1: String, seq2: String, pad: Int = 7): Unit = {
    println(Matrix.padInt(-1, pad) + (0 until matrix.cols).map { i => Matrix.padString(if (i == 0) "0" else seq2(i-1).toString, pad) }.mkString(""))
    (0 until matrix.rows).foreach { row => {
      print(Matrix.padString(if (row == 0) "0" else seq1(row - 1).toString, pad))
      (0 until matrix.cols).foreach { col => {
        print(Matrix.padDouble(matrix(row,col), pad))
      }
      }
      println()
    }
    }
  }

  /**
    * find the max in this row, from 0 until endCol
    * @param matrix the matrix
    * @param row the row to look in
    * @param endCol finds the max from 0 to endCol within this row
    * @return the max value
    */
  def maxInRowUntilColX(matrix: DenseMatrix[Double], row: Int, endCol: Int): Int = {
    var maxIndex = 0
    (0 until endCol).foreach{col => if (matrix(row,col) > matrix(row,maxIndex)) maxIndex = col}
    maxIndex
  }

  /**
    * find the max in this col, from 0 until endRow
    * @param matrix the matrix
    * @param col the col to look in
    * @param endRow finds the max from 0 to endRow within this column
    * @return the max value
    */
  def maxInColUntilRowX(matrix: DenseMatrix[Double], col: Int, endRow: Int): Int = {
    var maxIndex = 0
    (0 until endRow).foreach{row => if (matrix(row,col) > matrix(maxIndex,col)) maxIndex = row}
    maxIndex
  }
}

object ScoreMatrix {

  def matchInitialization(rows: Int, cols: Int): DenseMatrix[Double] = {
    val ret = new DenseMatrix[Double](rows, cols)
    ret(0, 0) = 1.0
    ret
  }

  def initializeMatrices(rows: Int, cols: Int): MatrixPack = {
    val matched = matchInitialization(rows, cols)
    MatrixPack(matched, new DenseMatrix[Double](rows, cols), new DenseMatrix[Double](rows, cols), new TracebackMatrix(rows, cols))
  }
}

case class MatrixPack(matchMatrix: DenseMatrix[Double],
                      insertMatrix: DenseMatrix[Double],
                      deletionMatrix: DenseMatrix[Double],
                      tracebackMatrix: TracebackMatrix)