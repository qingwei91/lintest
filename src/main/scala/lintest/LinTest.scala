package lintest

import cats.data.Chain
import scala.collection.mutable
import scala.reflect.ClassTag

object LinTest {
  def sequential[Op[_] <: Operation[_], Res](
      sequentialModel: Model[Op, Res],
      operationsProvider: OperationsProvider[Op[Res]]
  )(implicit CT: ClassTag[Op[Res]]): Option[Chain[Op[Res]]] = {

    /** 0. If there's no operation, return success
      * 1. Get operation A where A finished earliest among all operations from [[operationsProvider]]
      * 2. Get all operations that are concurrent with A, call CA
      * 3. Compute all permutations of CA
      * 4. For each permutation, feed into [[sequentialModel]], and [[modelUnderTest]]
      *    a. If both model return the same results, go to step 1
      *       b. If not, move to next permutation
      *       c. if all permutation exhausted, fail
      */

    val stack  = mutable.Stack.empty[Config[Op, Res]]
    val minOps = operationsProvider.getMinimalOps

    // initialize
    for (i <- 0 to minOps.length) {
      val nextMinOps = minOps.clone()
      nextMinOps.remove(i)
      stack.push(Config(minOps(i), operationsProvider, Chain.empty, sequentialModel))
    }

    var success: Option[Chain[Op[Res]]] = None
    while (stack.nonEmpty && success.isEmpty) {
      val Config(toLinearize, opsProvider, linearized, model) = stack.pop()
      val (exp, nextModel)                                    = model.applyOp(toLinearize)

      toLinearize.result match {
        case Wrapper(actual) =>
          if (actual == exp) {
            val updatedProvider = opsProvider.popMinimalOpOfThread(toLinearize.tid)
            val nextMinOps      = updatedProvider.getMinimalOps
            if (nextMinOps.isEmpty) {
              success = Some(linearized.append(toLinearize))
            } else {
              val updatedLin = linearized.append(toLinearize)
              nextMinOps.foreach { op =>
                stack.push(Config(op, updatedProvider, updatedLin, nextModel))
              }
            }
          }
        case TimeoutExp =>
          val updatedProvider = opsProvider.popMinimalOpOfThread(toLinearize.tid)
          val nextMinOps      = updatedProvider.getMinimalOps
          if (nextMinOps.isEmpty) {
            success = Some(linearized.append(toLinearize))
          } else {
            val updatedLin = linearized.append(toLinearize)
            updatedProvider.getMinimalOps.foreach { op =>
              // when op timed out, there are 2 possibilities, either the operation has taken effect, or it hasnt
              // so we need to
              stack.push(Config(op, updatedProvider, updatedLin, nextModel))
              stack.push(Config(op, updatedProvider, updatedLin, model))
            }
          }
        case () =>
          // this indicate the history of [[toLinearize.tid]] is incomplete, and we can assume it works and there
          // should be no more operation from the same tid
          val updatedProvider = opsProvider.popMinimalOpOfThread(toLinearize.tid)
          val nextMinOps      = updatedProvider.getMinimalOps
          if (nextMinOps.isEmpty) {
            success = Some(linearized.append(toLinearize))
          } else {
            val updatedLin = linearized.append(toLinearize)
            nextMinOps.foreach { op =>
              stack.push(Config(op, updatedProvider, updatedLin, nextModel))
            }
          }
      }
    }

    success
  }
  case class Config[Op[_] <: Operation[_], Res](
      toLinearize: Op[Res],
      opsProvider: OperationsProvider[Op[Res]],
      linearized: Chain[Op[Res]],
      referenceModel: Model[Op, Res]
  )
}
