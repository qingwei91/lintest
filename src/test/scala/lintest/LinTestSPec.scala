package lintest

import weaver.*

import java.io.File

object LinTestSPec extends SimpleIOSuite {
  pureTest("LinTest.sequential should work") {
    OperationsProvider.fromJepsenFormat(new File("test_data/jepsen/etcd_000.log"))
    ???
  }
}
