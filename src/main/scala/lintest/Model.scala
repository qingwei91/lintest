package lintest

trait Model[F[_], Op <: Operation] {
  
  def applyOp(op: Op): F[op.Res]
}

abstract trait Operation { type Res }