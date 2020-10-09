package de.unruh.isabelle.control

import java.nio.file.{Path, Paths}

import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.mlvalue.Implicits._

import scala.Console.err
import scala.concurrent.ExecutionContext.Implicits.global

object ConnectToRunningIsabelle {
  def main(args: Array[String]): Unit = {
    if (args.length != 2) err.println("Expecting two arguments: inputPipe outputPipe")
    val setup = Isabelle.SetupRunning(inputPipe = Paths.get(args(0)), outputPipe = Paths.get(args(1)))
    implicit val isabelle: Isabelle = new Isabelle(setup)
    println("\n[STARTED]")
    var count = 1

    MLValue.init()
    val test = MLValue.compileValue[Int]("123")
    assert(test.retrieveNow == 123)

    while (true) {
      Thread.sleep(10000)
      println(s"Ping $count...")
      count += 1
    };
  }
}
