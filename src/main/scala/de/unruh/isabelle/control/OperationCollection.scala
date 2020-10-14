package de.unruh.isabelle.control

import org.log4s
import org.log4s.Logger

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

/**
  * This is a utility trait for handling a common use case when working with [[Isabelle]]. We illustrate this with an example:
  *
  * Say we want to create a library with functions that can operate on floating point numbers on the Isabelle side.
  * (For simplicity, in this example we
  * will provide only on operation: converting strings to reals.)
  * For this, we need to declare an exception type `E_Real` for storing reals (ML code: `exception E_Real of real`)
  * and we need to compile and store the code converting strings to reals.
  *
  * The very first approach to this problem would be the following:
  * {{{
  * object Real {
  *   private val isabelle : Isabelle = ???
  *   isabelle.executeMLCodeNow("exception E_Real of real")
  *   private val fromStringID : Future[Isabelle.ID] = // Converts 'E_String str' into 'E_Real real'
  *     isabelle.storeValue("E_Function (fn E_String str => E_Real (Option.valOf (Real.fromString str)))")
  *   def fromString(string: String)(implicit ec: ExecutionContext) : Future[Isabelle.ID] = for (
  *       strId <- isabelle.storeString(string);
  *       fromStr <- fromStringID;
  *       real <- isabelle.applyFunction(fromStr, strId))
  *     yield real
  * }
  * }}}
  * Here `isabelle.executeMLCodeNow` sets up the required exception type, and `fromStringID` contains the (ID of the) compiled ML code for converting strings to ints.
  * And `fromString` is the user-facing function converting a string to an ID of a real (on the ML side).
  *
  * The problem here is that to we need an instance of [[Isabelle]] to perform those operations.
  * In the example above we simply wrote `val isabelle = ???` because we did not know where to get it from.
  * An obvious solution would be to make `Real` a class with an `isabelle: Isabelle` parameter.
  * However, that would make it less convenient to use `Real`: We need to explicitly create the `Real` and
  * keep track of it. Especially if the code that is responsible for the `Real` instance is not the code
  * that creates the [[Isabelle]] instance, this might be compilcated.
  *
  * A more user-friendly solution is therefore to keep `Real` as an object
  * and to pass the [[Isabelle]] instance as an implicit parameter to `Real.fromString`. However, this means `isabelle`
  * is not available outside `Real.fromString`, so we have to perform the initialization inside `fromString`:
  * {{{
  * object Real {
  *   def fromString(string: String)(implicit isabelle: Isabelle, ec: ExecutionContext) : Future[Isabelle.ID] = {
  *     isabelle.executeMLCodeNow("exception E_Real of real")
  *     val fromStringID : Future[Isabelle.ID] =
  *       isabelle.storeValue("E_Function (fn E_String str => E_Real (Option.valOf (Real.fromString str)))")
  *     for (
  *       strId <- isabelle.storeString(string);
  *       fromStr <- fromStringID;
  *       real <- isabelle.applyFunction(fromStr, strId))
  *     yield real
  *   }
  * }
  * }}}
  * This has two problems, however: First, the initialization code is executed every time when `fromString` is executed.
  * Since this involves invoking the ML compiler each time, this should be avoided. (Rule of thumb: ML code should
  * only occur in one-time initializations.) Even worse: by executing the ML code `exception E_Real of real`, we
  * actually create a different incompatible exception type `E_Real` each time (and override the name space element
  * `E_Real` each time). This will lead to failing code (at least if we would extend our example to actually use
  * the created real values). Also, in more complex examples, we might want several functions (not just `fromString`) to
  * share the same setup. To achieve this, we need global variables to track whether the initialization code has already
  * been executed and to store `fromStringID`, and – if we don't want our code to fail in the presence of several
  * simultaneous instances of [[Isabelle]] – keep track for which instances of [[Isabelle]] the initialization has
  * happened already.
  *
  * All this is made easy by the [[OperationCollection]] trait. The above code can be rewritten as follows:
  * {{{
  * object Real extends OperationCollection {
  *   override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  *   protected class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {
  *     isabelle.executeMLCodeNow("exception E_Real of real")
  *     val fromStringID : Future[Isabelle.ID] = // Converts 'E_String str' into 'E_Real real'
  *       isabelle.storeValue("E_Function (fn E_String str => E_Real (Option.valOf (Real.fromString str)))")
  *   }
  *   def fromString(string: String)(implicit isabelle: Isabelle, ec: ExecutionContext) : Future[Isabelle.ID] = for (
  *       strId <- isabelle.storeString(string);
  *       fromStr <- Ops.fromStringID;
  *       real <- isabelle.applyFunction(fromStr, strId))
  *     yield real
  * }
  * }}}
  * Note that we have defines an inner '''class''' Ops that performs the initialization and may depend on the
  * [[Isabelle]] instance `isabelle`. Yet we use it (in `fromStr <- Ops.fromStringID`) as if it were an '''object'''.
  * The trait [[OperationCollection]] makes this possible. Under the hood, `Ops` when used like an object
  * is a function with implicit parameters that creates a new `class Ops` instance only when needed (i.e., when a previously
  * unknown [[Isabelle]] instance is used).
  *
  * In general, [[OperationCollection]] is used with the following boilerplate:
  * {{{
  * object ObjectName extends OperationCollection {
  *   override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  *   protected class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {
  *     // arbitrary initialization code that is specific to the Isabelle instance `isabelle`
  *   }
  *   // code that uses Ops like an object
  * }
  * }}}
  * Note the following:
  * - Ops must be not be called differently
  * - In `protected class Ops`, `protected` can be replaced by something weaker if the operations should be accessible
  *   outside the current object (e.g. `protected[packagename]`)
  * - When `Obs` is used like an object, implicit of types [[Isabelle]] and [[scala.concurrent.ExecutionContext]] must be in scope
  * - The function `newOps` must be defined exactly as specified here
  */
trait OperationCollection {
  import OperationCollection._

  /** A type that should be overwritten by a class Ops that contains instance specific initialization code */
  protected type Ops

  /** Should construct an instance of type [[Ops]] */
  protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops

  /** Data structure optimized for very few (usually exactly 1) entries */
  private var opsInstances: List[(Isabelle, Ops)] = Nil

  private def addInstance(isabelle: Isabelle, ec: ExecutionContext): Ops = synchronized {
    def add() = {
      logger.debug(s"Adding Ops instance in ${getClass.getName} for $isabelle")
      assert(isabelle != null, "Isabelle is null")
      assert(ec != null, "Execution context is null")
      isabelle.checkDestroyed()
      val ops = newOps(isabelle, ec)
      opsInstances = (isabelle, ops) :: opsInstances.filterNot(_._1.isDestroyed)
      ops
    }

    // Searching again, in case of a race condition that added this instance while we did not have a lock
    @tailrec
    def get(instances: List[(Isabelle, Ops)]): Ops = instances match {
      case Nil => add()
      case (isabelle2, ops) :: rest =>
        if (isabelle2 == isabelle) ops
        else get(rest)
    }

    get(opsInstances)
  }

  /** Returns an instance of type [[Ops]]. It is guaranteed that for each instance `isabelle`, exactly one
    * instance of `Obs` is created (using the `ec` from the first such invocation).
    * (If you see this doc string in a class different from [[OperationCollection]] but no definition of the
    * class [[Ops]], treat this function as if it was private.)
    */
  def Ops(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = {
    @tailrec
    def get(instances: List[(Isabelle, Ops)]): Ops = instances match {
      case (isabelle2, ops) :: rest =>
        if (isabelle2 == isabelle) ops
        else get(rest)
      case Nil => addInstance(isabelle, ec)
    }

    get(opsInstances)
  }

  /** Makes sure an [[Ops]] instance for the instance `isabelle` is initialized.
    * This is useful when code needs to be sure that the global initialization inside the [[Ops]] class
    * has happened (e.g., declarations of ML types via [[Isabelle.executeMLCodeNow]]) even if it does not access
    * any of the fields in the [[Ops]] class.
    *
    * Can safely be called several times with the same `isabelle` and/or `executionContext`.
    */
  def init()(implicit isabelle: Isabelle, executionContext: ExecutionContext): Unit =
    Ops
}

private object OperationCollection {
  private val logger: Logger = log4s.getLogger
}