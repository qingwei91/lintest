package lintest

object LinTest {
  def check[F[_], OP <: Operation](
      sequentialModel: Model[F, OP],
      modelUnderTest: Model[F, OP],
      operationsProvider: OperationsProvider[F, OP]
  ) = {
    ???
  }
}
