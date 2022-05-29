package lintest

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

object Utils {
  def factorial(n: Int, acc: Int = 1): Int = n match {
    case 0 => 1 * acc
    case 1 => 1 * acc
    case 2 => 2 * acc
    case o => factorial(o - 1, acc * o)
  }

  /**
   * This algorithm is not very efficient, and its not easy to parallelize
   * We can only parallelize easily by partition, and that depends on no of elements to start with
   * Due to its recursive nature, this method involves significant copying which can be inefficient,
   * the biggest offender is when we call the recurse method, we need to clone the remaining elements before passing it
   * to the method, and then perform a removal.
   *
   * Cloning is needed because we cannot share the data structure that needs to be mutated differently
   * One potential speed up we can try is to introduce a concept of masking, then perhaps we can avoid mutating the original arrays
   * But that will still allocate memory on each pass as the mask has to be different.
   *
   * TODO: can we make this non-recursive??
   * TODO: consider implement Heap's algorithm instead (note its not parallelizable)
   * @param stuffs
   * @tparam A
   * @return
   */
  def recursivePermutations[A: ClassTag](stuffs: Array[A]): Array[Array[A]] = {
    val n      = stuffs.length
    val f      = factorial(n)
    val matrix = Array.ofDim[A](f, n)

    def recurse(
        remaining: ArrayBuffer[A],
        topLeftX: Int,
        topLeftY: Int,
        bottomRightX: Int,
        bottomRightY: Int
    ): Unit = {
      if (remaining.isEmpty) {
        return ()
      }

      if (remaining.length == 1) {
        // here topLeft and bottomRight should be the same point
        matrix(topLeftX)(topLeftY) = remaining(0)
        return ()
      }

      if (remaining.length == 2) {
        // here topLeft and bottomRight should be on diagonal
        matrix(topLeftX)(topLeftY) = remaining(0)
        matrix(topLeftX)(bottomRightY) = remaining(1)
        matrix(bottomRightX)(topLeftY) = remaining(1)
        matrix(bottomRightX)(bottomRightY) = remaining(0)
        return ()
      }
      val noOfPartition = remaining.length
      val partitionSize = factorial(noOfPartition) / noOfPartition

      // this is parallelizable, while we modify a shared data structure, we never modify the same element
      // and assignments are atomic, so it is safe to parallelize this
      for (partitionNo <- 0 until noOfPartition) {
        val partitionStartX = partitionNo * partitionSize + topLeftX
        for (j <- 0 until partitionSize) {
          matrix(partitionStartX + j)(topLeftY) = remaining(partitionNo)
        }

        val nextRemaining = remaining.clone()
        nextRemaining.remove(partitionNo)

        val nextTopX    = partitionStartX
        val nextTopY    = topLeftY + 1
        val nextBottomX = nextTopX + partitionSize - 1

        // this basically never change, it always point to the last col
        val nextBottomY = bottomRightY
        recurse(
          nextRemaining,
          topLeftX = nextTopX,
          topLeftY = nextTopY,
          bottomRightX = nextBottomX,
          bottomRightY = nextBottomY
        )

        ()
      }
    }

    recurse(ArrayBuffer.from(stuffs), 0, 0, f - 1, n - 1)
    matrix
  }
}
