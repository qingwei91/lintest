package lintest

trait OperationsProvider[F[_], Op] {
  def popFinishedEarliest: F[Option[Op]]
  def popAllConcurrentWith(op: Op): F[Array[Op]]
}
