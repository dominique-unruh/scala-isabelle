package de.unruh.isabelle.control

import java.nio.file.Paths
import de.unruh.isabelle.control.Isabelle.{DList, DString, Data}
import de.unruh.isabelle.mlvalue.MLValue

import scala.Console.err
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.reflect.runtime.currentMirror

// Implicits
import scala.tools.reflect.ToolBox
import de.unruh.isabelle.mlvalue.Implicits._

object ConnectToRunningIsabelle {

  def main(args: Array[String]): Unit = {
    implicit val executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
    if (args.length != 2) err.println("Expecting two arguments: inputPipe outputPipe")

    val toolbox = Future {
      currentMirror.mkToolBox()
    }

    def commandHandler(data: Data): Unit = data match {
      case DList(DString(scala), args) =>
        val scala2 =
          s"""{ (_isabelle: de.unruh.isabelle.control.Isabelle, data: de.unruh.isabelle.control.Isabelle.Data) =>
implicit val isabelle = _isabelle; implicit val executionContext = _isabelle.executionContext; {
$scala
}; () }"""
        println(scala2)
        val tb = Await.result(toolbox, Duration.Inf)
        val command = tb.eval(tb.parse(scala2)).asInstanceOf[(Isabelle, Data) => Unit]
        command(isabelle, args)
      case _ => throw new RuntimeException(s"Unexpected command from Isabelle: $data")
    }

    lazy val setup = Isabelle.SetupRunning(
      inputPipe = Paths.get(args(0)), outputPipe = Paths.get(args(1)),
      isabelleCommandHandler = commandHandler)

    implicit lazy val isabelle: Isabelle = new Isabelle(setup)
    isabelle

    println("\n[STARTED]")
    var count = 1

    //    MLValue.init()
    val test = MLValue.compileValue[Int]("123")
    assert(test.retrieveNow == 123)

    while (true) {
      Thread.sleep(10000)
      println(s"Ping $count...")
      count += 1
    };
  }
}
