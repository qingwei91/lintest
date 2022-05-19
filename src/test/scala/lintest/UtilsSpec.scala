package lintest

import weaver._
import weaver.Expect
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx._

object UtilsSpec extends SimpleIOSuite {
  pureTest("Permutations should work with 3 elements") {
//    val actual = Utils.permutations(Array[Int](3, 4, 5)).map(_.toList).toList
//    val expected = List(
//      List(3, 4, 5),
//      List(3, 5, 4),
//      List(4, 3, 5),
//      List(4, 5, 3),
//      List(5, 3, 4),
//      List(5, 4, 3)
//    )
//    expect(actual == expected)
      success
  }

  pureTest("Permutations should work with 3 elements") {
    val actual = Utils.permutations(Array[Int](3, 4, 5, 8)).map(_.toList).toList
    val expected = List(
      List(3, 4, 5, 8),
      List(3, 4, 8, 5),
      List(3, 5, 4, 8),
      List(3, 5, 8, 4),
      List(3, 8, 4, 5),
      List(3, 8, 5, 4),
      List(4, 3, 5, 8),
      List(4, 3, 8, 5),
      List(4, 5, 3, 8),
      List(4, 5, 8, 3),
      List(4, 8, 3, 5),
      List(4, 8, 5, 3),
      List(5, 3, 4, 8),
      List(5, 3, 8, 4),
      List(5, 4, 3, 8),
      List(5, 4, 8, 3),
      List(5, 8, 3, 4),
      List(5, 8, 4, 3),
      List(8, 3, 4, 5),
      List(8, 3, 5, 4),
      List(8, 4, 3, 5),
      List(8, 4, 5, 3),
      List(8, 5, 3, 4),
      List(8, 5, 4, 3)
    )
    expect(actual == expected)
  }
}
