package de.unruh.isabelle.control

import java.nio.file.{Path, Paths}

import scala.Console.err

object ConnectToRunningIsabelle {
  def main(args: Array[String]): Unit = {
    if (args.length != 2) err.println("Expecting two arguments: inputPipe outputPipe")
    val setup = Isabelle.SetupRunning(inputPipe = Paths.get(args(0)), outputPipe = Paths.get(args(1)))
    val isabelle = new Isabelle(setup)
    while (true) wait();
  }
}
