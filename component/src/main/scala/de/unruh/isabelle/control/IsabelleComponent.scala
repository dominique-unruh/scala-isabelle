package de.unruh.isabelle.control
import de.unruh.isabelle.control.IsabelleComponent._
import _root_.isabelle.Scala.{Fun, Fun_String, Fun_Strings, Functions}
import _root_.isabelle.{Bytes, Scala_Project}
import de.unruh.isabelle.control.Isabelle.{ID, Setup, SetupRunning}
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

  /** Creates an [[MLValue]] based on an underlying ID referencing the object store in the Isabelle process.
   * The ID is given as an integer (and not as an [[ID]] object).
   * See [[Isabelle]] for an explanation about the object store.
   * This function is highly unsafe:
   *
   * * It is not checked whether the object stored at ID `id` is indeed of type `A`
   *
   * * It is not checked whether `id` references an object in the object store
   *
   * * By creating an [[MLValue]] from `id`, `id` will be owned by that [[MLValue]].
   *   That is, if the [[MLValue]] is garbage collected, the `id` will be deallocated.
   *   Thus `id` be an refer to an object ID used anywhere else.
   *
   * This function must only be used in the following way:
   * On the Isabelle/ML side, use `Control_Isabelle.addToObjects exn` where `exn` is an exception encoding the object to be transmitted (cf.~[[Isabelle]]).
   * This returns an integer `id`.
   * On the Scala side, invoke `unsafeMLValueFromNumericID(id)`.
   * Do not use `id` elsewhere (except possibly in debug outputs etc.), and do not call `unsafeMLValueFromNumericID(id)` twice with the same ID.
   * */
  def unsafeMLValueFromNumericID[A](id: Long): MLValue[A] = MLValue.unsafeFromId[A](new ID(id, isabelle))
}

