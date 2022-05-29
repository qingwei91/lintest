package lintest

trait Model[Op[_] <: Operation[_], Res] {
  def applyOp(op: Op[Res]): (Res, Model[Op, Res])
}

abstract trait Operation[Res] {
  def result: Wrapper[Res] | TimeoutExp.type | Unit
  def tid: Int
}

case object TimeoutExp

case class Wrapper[T](res: T)