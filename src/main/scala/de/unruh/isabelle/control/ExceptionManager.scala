package de.unruh.isabelle.control

import de.unruh.isabelle.control.Isabelle.{DObject, DString, ID}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/** Handles the conversion of exceptions from the Isabelle process (in ML) to Scala/JVM exceptions.
 * An [[ExceptionManager]] is always associated with a given [[Isabelle]] instance.
 * You can set the [[ExceptionManager]] using the [[de.unruh.isabelle.control.Isabelle.SetupGeneral.exceptionManager]] option.
 * The current instance can be accessed as [[Isabelle.exceptionManager]].
 * Own implementation can start by subclassing [[DefaultExceptionManager]] (but do not have to).
 */
trait ExceptionManager {
  def createException(id: ID): Exception
  def messageOf(id: ID): String
}

/** Default implementation of [[ExceptionManager]].
 * Produces messages for all exceptions by pretty printing them in Isabelle with a `Pure` context.
 * Generated exceptions are always [[IsabelleMLException]] instances (no exception specific subclasses).
 *
 * See [[de.unruh.isabelle.pure.Exn.ExceptionManager]] for an alternative [[ExceptionManager]] that supports subclasses.
 **/
class DefaultExceptionManager(isabelle: Isabelle) extends ExceptionManager {
  private var messageOfException: ID = _
  def messageOf(id: ID): String = try {
    if (messageOfException==null) {
      // Race conditions are possible but harmless here: if `messageOfException` is initialized at the same time in another thread,
      // one of the two values will end up being used and the other gets garbage collected.
      messageOfException = Await.result(isabelle.storeValue("E_Function (fn DObject exn => DString (message_of_exn NONE exn))"), Duration.Inf)
    }

    Await.result(isabelle.applyFunction(messageOfException, DObject(id)), Duration.Inf) match {
      case DString(message) => message
      case _ => assert(assertion = false, "Unreachable code in pattern match"); null
    }
  } catch {
    case _ : IsabelleMLException =>
      throw IsabelleMiscException("IsabelleMLException thrown in code for getting message of an IsabelleMLException")
  }

  override def createException(id: ID): Exception = IsabelleMLException.unsafeFromId(isabelle, id)
}
