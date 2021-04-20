package example
import de.unruh.isabelle.control.{Isabelle, IsabelleException, IsabelleProtocolException}
import de.unruh.isabelle.control.Isabelle.{Setup, SetupRunning}
import de.unruh.isabelle.pure.{Context, Term}
import example.MyFunctions._
import _root_.isabelle.Scala.{Fun, Functions}
import _root_.isabelle.Scala_Project

import java.nio.file.Path
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

class MyFunctions extends Functions(echo, log, initializeScalaIsabelle, accessScalaIsabelle)
object MyFunctions {
  private val isabellePromise = Promise[Isabelle]()
  implicit def isabelle: Isabelle =
    isabellePromise.future.value.getOrElse {
      throw IsabelleProtocolException("Isabelle process was not initialized.")
    }.get

  object echo extends Fun("reverse") {
    override val here: Scala_Project.Here = Scala_Project.here
    override def apply(arg: String): String = arg.reverse
  }

  object initializeScalaIsabelle extends Fun("initializeScalaIsabelle") {
    override def here: Scala_Project.Here = Scala_Project.here
    val alreadyConnected = "(* Scala-isabelle already connected. *)"
    override def apply(arg: String): String = {
      assert(arg.isEmpty)
      if (!isabellePromise.isCompleted)
        isabellePromise.synchronized {
          if (!isabellePromise.isCompleted) {
            val protocolSetupMLCode = Promise[String]()
            val setup = SetupRunning(protocolSetupMLCode)
            val isa: Isabelle = new Isabelle(setup)
            isabellePromise.success(isa)
            Await.result(protocolSetupMLCode.future, Duration.Inf)
          } else alreadyConnected
        }
      else alreadyConnected
    }
  }

  object log extends Fun("log") {
    override val here: Scala_Project.Here = Scala_Project.here
    override def apply(arg: String): String = {
      LogWindow.log(arg)
      "no return value"
    }
  }

  object accessScalaIsabelle extends Fun("accessScalaIsabelle") {
    override def here: Scala_Project.Here = Scala_Project.here
    override def apply(arg: String): String = {
      val ctxt = Context("Main")
      val term = Term(ctxt, arg)
      term.pretty(ctxt)
    }
  }
}

