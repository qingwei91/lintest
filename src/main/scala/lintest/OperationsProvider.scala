package lintest

import cats.data.NonEmptyList
import cats.parse.Parser.not

import java.io.File
import scala.collection.mutable
import scala.io.Source
import cats.syntax.all.*

import scala.collection.mutable.ArrayBuffer

trait OperationsProvider[Op] {
  def getMinimalOps: mutable.ArrayBuffer[Op]
  // this should be immutable
  def popMinimalOpOfThread(tid: Int): OperationsProvider[Op]
}

object OperationsProvider {

  import cats.parse.Parser
  import cats.parse.{Rfc5234, Numbers}

  val numberList: Parser[List[Int]] =
    (Parser.char('[') *> Parser.until(Parser.char(']')) <* Parser.char(']')).map(_.split(" ").toList.map(_.toInt))

  type Ugh = Wrapper[List[Int]] | TimeoutExp.type | Unit
  val singleOrMoreNumber: Parser[Ugh] =
    ((Rfc5234.digit.rep <* Parser.end).map(cLs => cLs.toList.mkString.toInt :: Nil) | numberList)
      .map(Wrapper(_): Ugh): Parser[Ugh]

  val opResult: Parser[Ugh] =
    singleOrMoreNumber
      .orElse[Ugh](Parser.string(":timed-out").as[Ugh](TimeoutExp))
      .orElse[Ugh](Parser.string("nil").as[Ugh](()))

  val jepsenLogBoilerplate            = Parser.string("INFO") *> Rfc5234.wsp.rep *> Parser.string("jepsen.util - ")
  val jepsenEventType: Parser[String] = Parser.stringIn(":ok" :: ":invoke" :: ":fail" :: ":info" :: Nil)

  val jepsenLogParser: Parser[JepsenRegistryOperation] = (
    jepsenLogBoilerplate *> Numbers.digits.map(_.toInt) <* Rfc5234.wsp.rep,
    jepsenEventType <* Rfc5234.wsp.rep,
    Parser.until(Rfc5234.wsp) <* Rfc5234.wsp.rep,
    opResult: Parser[Ugh]
  ).tupled.map { case (tid, eventType, operationName, payload: Ugh) =>
    JepsenRegistryOperation(tid, eventType, operationName, payload)
  }

  case class JepsenRegistryOperation(
      tid: Int,
      eventType: String,
      operationName: String,
      result: Wrapper[List[Int]] | TimeoutExp.type | Unit
  ) extends Operation[List[Int]]

  def fromJepsenFormat(file: File): OperationsProvider[JepsenRegistryOperation] = {
    val partialOps   = mutable.Map.empty[Int, JepsenRegistryOperation]
    val completedOps = mutable.Map.empty[Int, mutable.ArrayBuffer[JepsenRegistryOperation]]
    val src          = Source.fromFile(file)

    try {
      for (ln <- src.getLines()) {
        // assume invoke and completion always occurs in the right order
        jepsenLogParser.parseAll(ln) match {
          case Left(err) =>
            println(ln)
            throw new RuntimeException(err.toString)
          case Right(parsedOp) =>
            partialOps.get(parsedOp.tid) match {
              case Some(partialOp) =>
                partialOps.remove(parsedOp.tid)

                val completedOp = partialOp.copy(result = parsedOp.result)

                completedOps.get(parsedOp.tid) match {
                  case Some(exists) => exists.append(completedOp)
                  case None         => completedOps.update(partialOp.tid, ArrayBuffer.from(partialOp :: Nil))
                }

              case None =>
                assert(parsedOp.eventType == ":invoke")
                partialOps.addOne(parsedOp.tid, parsedOp)
            }
        }
      }
      // ditch partial ops so that we only ever deal with complete history
      new OperationsProvider[JepsenRegistryOperation] {
        override def getMinimalOps: ArrayBuffer[JepsenRegistryOperation] =
          ArrayBuffer.from(completedOps.values.flatMap { historyPerThread =>
            historyPerThread.headOption
          })

        override def popMinimalOpOfThread(tid: Int): OperationsProvider[JepsenRegistryOperation] = {
          completedOps.updateWith(tid) {
            case Some(value) => value.drop(1).some
            case None        => throw new Exception(s"$tid should exists")
          }
          this
        }
      }
    } finally {
      src.close()
    }
  }
}
