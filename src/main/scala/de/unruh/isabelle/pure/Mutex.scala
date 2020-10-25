package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.{MLValue, MLValueWrapper}
import de.unruh.isabelle.mlvalue.MLValue.{compileFunction, compileFunction0, compileValue}

import scala.concurrent.ExecutionContext

/** Represents a mutex (ML type `Mutex.mutex`) in the Isabelle process.
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 *
 * An implict [[mlvalue.MLValue.Converter MLValue.Converter]] can be imported from [[Implicits]]`._`. The representation
 * of a mutex `m` as an ML exception is `E_Mutex m`.
 */
final class Mutex private [Mutex](val mlValue : MLValue[Mutex]) extends MLValueWrapper[Mutex] {
  override def toString: String = "mutex" + mlValue.stateString
}

object Mutex extends MLValueWrapper.Companion[Mutex] {
  override protected val mlType = "Mutex.mutex"
  override protected val predefinedException: String = "E_Mutex"

  override protected def instantiate(mlValue: MLValue[Mutex]): Mutex = new Mutex(mlValue)

  //noinspection TypeAnnotation
  protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) extends super.Ops {
    lazy val createMutex = compileFunction0[Mutex]("Mutex.mutex")
  }

  /** Creates a new mutex */
  def apply()(implicit isabelle: Isabelle, ec: ExecutionContext) : Mutex = Ops.createMutex().retrieveNow

  /** Returns a fragment of ML code that exectutes the ML code `code` with the mutex `mutex`.
   * `mutex` is assumed to be the name of an ML value that holds the mutex.
   *
   * Example:
   * {{{
   * val thyMutex = Mutex()
   * val useThyWithMutex = MLValue.compileFunction[Mutex, String, Unit](
   *         s"fn (mutex,theoryName) => ${Mutex.wrapWithMutex("mutex", "Thy_Info.use_thy name")}")
   * }}}
   * Then `useThyWithMutex(mutex, "Test.Test")` loads the theory `Test.Test` in Isabelle but makes sure only
   * a single instance is running concurrently (with the locking happening in Isabelle using `mutex`), since
   * `Thy_Info.use_thy` is not thread safe. (In practice, better use [[Theory.mutex]] as the mutex instead
   * of your own in this specific case.)
   **/
  def wrapWithMutex(mutex: String, code: String) =
    s"let val _ = Mutex.lock $mutex val result = ($code) handle e => (Mutex.unlock $mutex; Exn.reraise e) val _ = Mutex.unlock $mutex in result end"

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops
}