package de.unruh.isabelle.control
import de.unruh.isabelle.control.IsabelleComponent._
import _root_.isabelle.Scala.{Fun, Fun_String, Fun_Strings, Functions}
import _root_.isabelle.{Bytes, Scala_Project}
import de.unruh.isabelle.control.Isabelle.{Setup, SetupRunning}
import de.unruh.isabelle.control.{Isabelle, IsabelleException, IsabelleProtocolException}
import de.unruh.isabelle.mlvalue.MLValue

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import scala.concurrent.ExecutionContext.Implicits.global

class IsabelleComponent extends Functions(test, initializeScalaIsabelle) {
}

object IsabelleComponent {
  private val isabellePromise = Promise[Isabelle]()
  implicit lazy val isabelle: Isabelle =
    isabellePromise.future.value.getOrElse {
      throw IsabelleProtocolException("Isabelle process was not initialized.")
    }.get

  object test extends Fun_String("test") {
    override val here: Scala_Project.Here = Scala_Project.here
    override def apply(arg: String): String = {
      MLValue.compileValue[Int](arg).retrieveNow.toString
    }
  }

  object initializeScalaIsabelle extends Fun_String("initializeScalaIsabelle") {
    override def here: Scala_Project.Here = Scala_Project.here
    def alreadyConnected = throw new IllegalStateException("scala-isabelle is already connected")
    override def apply(arg: String): String = {
      assert(arg == "I know what I am doing!", "initializeScalaIsabelle called without magic argument")
      if (isabellePromise.isCompleted) alreadyConnected
      isabellePromise.synchronized {
        if (isabellePromise.isCompleted) alreadyConnected
        val initializationCodeForIsabelle = Promise[String]()
        val setup = SetupRunning(initializationCodeForIsabelle)
        val isa: Isabelle = new Isabelle(setup)
        isabellePromise.success(isa)
        Await.result(initializationCodeForIsabelle.future, Duration.Inf)
      }
    }
  }
}

