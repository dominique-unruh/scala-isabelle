package de.unruh.isabelle.control

import de.unruh.isabelle.control.Isabelle.DInt
import de.unruh.isabelle.control.IsabelleTest.isabelle
import de.unruh.isabelle.mlvalue.MLValueTest.await

// With sockets:     1.58082ms
// With named pipes: 0.03339ms
// With sockets in Windows (VM): 0.56555ms
object Benchmark1 {
  def main(args: Array[String]): Unit = {
    val id = await(isabelle.storeValue("E_Function I"))
    println("Running warmup loop")
    for (i <- 1 to 10000) {
      await(isabelle.applyFunction(id, DInt(0)))
    }
    println("Warmup loop done")
    val count = 200000
    val time1 = System.currentTimeMillis()
    for (i <- 1 to count) {
      await(isabelle.applyFunction(id, DInt(0)))
    }
    val time2 = System.currentTimeMillis()
    val perOp = (time2 - time1) * 1.0 / count
    println(s"Time per op: ${perOp}ms")
  }
}
