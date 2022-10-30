package de.unruh.isabelle.pure.exceptions

import de.unruh.isabelle.control
import de.unruh.isabelle.control.Isabelle.{DInt, DList, DObject, DString, Data, ID}
import de.unruh.isabelle.control.{Isabelle, IsabelleMLException, IsabelleMiscException, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{MLFunction2, MLValue}
import de.unruh.isabelle.pure.{Context, Cterm, Term, Theory, Thm, Typ}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

/** Contains:
 *  - [[Converter]]s for exceptions raised in ML code (represented as [[IsabelleMLException]]s), see [[MLException.simpleIsabelleMLExceptionConverter]] and [[MLException.distinguishingIsabelleMLExceptionConverter]])
 *  - An [[control.ExceptionManager]] that makes [[Isabelle]] raise ML exceptions as subtypes of [[IsabelleMLException]] for certain well-known
 *    exceptions such as `TERM`, `ERROR`, etc. See [[MLException.ExceptionManager]]. */
object MLException extends OperationCollection {
  /**
   * [[MLValue.Converter]] for type [[IsabelleMLException]][A].
   *
   *  - ML type: `exn`.
   *  - Encoding of exception `e` as an exception: `e` (that is, the exception is directly stored
   *    without any encoding in the object table which is possible since the object table stores
   *    all values encoded as exceptions).
   *
   * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
   *
   *      Override the function [[recognize]] if you want the returned exception to be some more
   *      informative subclass of [[IsabelleMLException]].
   *
   *      Two preconfigured instantiations for importing exist:
   *      [[simpleIsabelleMLExceptionConverter]] and [[distinguishingIsabelleMLExceptionConverter]], see there.
   */
  abstract class IsabelleMLExceptionConverter extends Converter[IsabelleMLException] {
    final override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = "exn"
    final override def retrieve(value: MLValue[IsabelleMLException])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[IsabelleMLException] = {
      for (id <- value.id;
           exn = IsabelleMLException.unsafeFromId(isabelle, id);
           exn2 <- recognize(exn))
        yield exn2
    }

    final override def store(value: IsabelleMLException)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[IsabelleMLException] = MLValue.unsafeFromId(value.id)
    final override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = "(fn e : exn => e)"
    final override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = "(fn e : exn => e)"

    /** This function can replace the retrieved exception (`exception`) by a more informative exception object
     * (e.g., a subclass, or an [[IsabelleMLException]] with a different message). The returned object
     * must have the same [[IsabelleMLException.id id]] and [[IsabelleMLException.isabelle]] fields as `exception`.
     * If no additional information is to be added, simply return `exception` unchanged.
     * @param exception The exception to be transformed */
    def recognize(exception: IsabelleMLException)(implicit ec: ExecutionContext) : Future[IsabelleMLException]
  }

  /** Preconfigured instance of [[IsabelleMLExceptionConverter]]. It simply returns an [[IsabelleMLException]] without
   * any extra processing. Use by importing it. */
  implicit object simpleIsabelleMLExceptionConverter extends IsabelleMLExceptionConverter {
    def recognize(exception: IsabelleMLException)(implicit ec: ExecutionContext) : Future[IsabelleMLException] = Future.successful(exception)
  }

  /** Preconfigured instance of [[IsabelleMLExceptionConverter]].
   * For certain known kind of ML exceptions (such as `TERM _` etc.), it returns subclasses of [[IsabelleMLException]]
   * (such as [[TermMLException]]).
   * It uses [[recognizeException]] to recognize the exception, see there for a list of all supported exceptions.
   * Use by importing it. */
  implicit object distinguishingIsabelleMLExceptionConverter extends IsabelleMLExceptionConverter {
    override def recognize(exception: IsabelleMLException)(implicit ec: ExecutionContext): Future[IsabelleMLException] =
      recognizeException(exception)
  }

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  //noinspection TypeAnnotation
  protected class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {
    val recognizeException = MLValue.compileFunction[Data, Data](
      """fn DObject exn => case exn of
        |  ERROR message => DList [DString "ERROR", DString message]
        |  | Fail message => DList [DString "Fail", DString message]
        |  | THEORY (msg, thys) => DList (DString "THEORY" :: DString msg :: map (fn thy => DObject (E_Theory thy)) thys)
        |  | TERM (msg, terms) => DList (DString "TERM" :: DString msg :: map (fn t => DObject (E_Term t)) terms)
        |  | CTERM (msg, cts) => DList (DString "CTERM" :: DString msg :: map (fn ct => DObject (E_Cterm ct)) cts)
        |  | THM (msg, i, thms) => DList (DString "THM" :: DString msg :: DInt i :: map (fn thm => DObject (E_Thm thm)) thms)
        |  | TYPE (msg, Ts, ts) => DList [DString "TYPE", DString msg,
        |                                 DList (map (fn T => DObject (E_Typ T)) Ts), DList (map (fn t => DObject (E_Term t)) ts)]
        |  | Match => DList [DString "Match"]
        |  | _ => DList [DString "unknown"]
        |  """.stripMargin)
  }

  /** Takes an [[IsabelleMLException]] and analyses whether it contains one of several known ML exceptions for
   * which there are more informative subclasses of [[IsabelleMLException]]. If so, returns a new more informative
   * exception object that contains the same exception.
   *
   * Known exception types are:
   *  - [[ErrorMLException]] for `ERROR msg`
   *  - [[FailMLException]] for `Fail msg`
   *  - [[TheoryMLException]] for `THEORY (msg, theories)`
   *  - [[TermMLException]] for `TERM (msg, terms)`
   *  - [[CtermMLException]] for `CTERM (msg, cterms)`
   *  - [[ThmMLException]] for `THM (msg, index, thms)`
   *  - [[TypeMLException]] for `TYPE (msg, typs, terms)`
   *  - [[MatchMLException]] for `Match`
   *    In all other cases, the exception is returned unchanged. (You can change this with the `fallback` argument.)
   *
   * @param exception The exception to transform
   * @param fallback A function for transforming the exception if it is not any of the above
   * */
  def recognizeException(exception: IsabelleMLException, fallback: IsabelleMLException => IsabelleMLException = identity)
                        (implicit ec: ExecutionContext): Future[IsabelleMLException] = {
    val id = exception.id
    implicit val isabelle: Isabelle = exception.isabelle
    for (DList(DString(typ), args @_*) <- Ops.recognizeException(DObject(id)).retrieve)
      yield typ match {
        case "ERROR" => val Seq(DString(msg)) = args; new ErrorMLException(isabelle, id, msg)
        case "Fail" => val Seq(DString(msg)) = args; new FailMLException(isabelle, id, msg)
        case "THEORY" =>
          val Seq(DString(msg), thys @_*) = args
          val thys2 = for (DObject(thy) <- thys) yield MLValue.unsafeFromId[Theory](thy).retrieveNow
          new TheoryMLException(isabelle, id, msg, thys2 : _*)
        case "TERM" =>
          val Seq(DString(msg), terms @_*) = args
          val terms2 = for (DObject(t) <- terms) yield MLValue.unsafeFromId[Term](t).retrieveNow
          new TermMLException(isabelle, id, msg, terms2 : _*)
        case "CTERM" =>
          val Seq(DString(msg), cterms @_*) = args
          val cterms2 = for (DObject(t) <- cterms) yield MLValue.unsafeFromId[Cterm](t).retrieveNow
          new CtermMLException(isabelle, id, msg, cterms2 : _*)
        case "THM" =>
          val Seq(DString(msg), DInt(i), thms @_*) = args
          val thms2 = for (DObject(thm) <- thms) yield MLValue.unsafeFromId[Thm](thm).retrieveNow
          new ThmMLException(isabelle, id, msg, i, thms2 : _*)
        case "TYPE" =>
          val Seq(DString(msg), DList(typs @_*), DList(terms @_*)) = args
          val typs2 = for (DObject(typ) <- typs) yield MLValue.unsafeFromId[Typ](typ).retrieveNow
          val terms2 = for (DObject(t) <- terms) yield MLValue.unsafeFromId[Term](t).retrieveNow
          new TypeMLException(isabelle, id, msg, typs2, terms2)
        case "Match" => new MatchMLException(isabelle, id)
        case "unknown" =>
          if (fallback==null) null; else fallback(exception)
        case _ => assert(assertion = false, "unreachable code"); null
      }
  }

  /** An implementation of [[control.ExceptionManager]] (for controlling how exceptions thrown in ML code are passed
   * through to Scala) that returns more specific subclasses of [[IsabelleMLException]] for certain exceptions
   * (see [[recognizeException]] for the known exceptions).
   * Install it via [[Isabelle.SetupGeneral.exceptionManager]].
   **/
  class ExceptionManager(isabelle: Isabelle) extends control.ExceptionManager {
    implicit val isa: Isabelle = isabelle
    import scala.concurrent.ExecutionContext.Implicits.global
    private var context : Context = _

    def setContext(ctxt: Context): Unit = context = ctxt

    override def createException(id: ID): Exception =
      Await.result(recognizeException(IsabelleMLException.unsafeFromId(isabelle, id)), Duration.Inf)

    private var messageOfException: MLFunction2[Option[Context], Data, String] = _
    def messageOf(id: ID): String = try {
      if (messageOfException==null) {
        // Race conditions are possible but harmless here: if `messageOfException` is initialized at the same time in another thread,
        // one of the two values will end up being used and the other gets garbage collected.
        messageOfException = MLValue.compileFunction[Option[Context], Data, String](
        "fn (ctxt, DObject exn) => message_of_exn ctxt exn")
      }
      messageOfException(Option(context), DObject(id)).retrieveNow
    } catch {
      case _ : IsabelleMLException =>
        throw IsabelleMiscException("IsabelleMLException thrown in code for getting message of an IsabelleMLException")
    }
  }
}

/** Represents an ML exception `ERROR msg`. See [[MLException.ExceptionManager]] for a way to make [[Isabelle]] raise such exceptions instead of generic [[IsabelleMLException]]s. */
final class ErrorMLException private[exceptions](override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String) extends IsabelleMLException(isabelle, id)
/** Represents an ML exception `Fail msg`. See [[MLException.ExceptionManager]] for a way to make [[Isabelle]] raise such exceptions instead of generic [[IsabelleMLException]]s. */
final class FailMLException private[exceptions](override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String) extends IsabelleMLException(isabelle, id)
/** Represents an ML exception `THEORY (msg, theories)`. See [[MLException.ExceptionManager]] for a way to make [[Isabelle]] raise such exceptions instead of generic [[IsabelleMLException]]s. */
final class TheoryMLException private[exceptions](override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String, val theories: Theory*) extends IsabelleMLException(isabelle, id)
/** Represents an ML exception `TERM (msg, terms)`. See [[MLException.ExceptionManager]] for a way to make [[Isabelle]] raise such exceptions instead of generic [[IsabelleMLException]]s. */
final class TermMLException private[exceptions](override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String, val terms: Term*) extends IsabelleMLException(isabelle, id)
/** Represents an ML exception `TYPE (msg, typs, terms)`. See [[MLException.ExceptionManager]] for a way to make [[Isabelle]] raise such exceptions instead of generic [[IsabelleMLException]]s. */
final class TypeMLException private[exceptions](override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String, val typs: Seq[Typ], val terms: Seq[Term]) extends IsabelleMLException(isabelle, id)
/** Represents an ML exception `CTERM (msg, cterms)`. See [[MLException.ExceptionManager]] for a way to make [[Isabelle]] raise such exceptions instead of generic [[IsabelleMLException]]s. */
final class CtermMLException private[exceptions](override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String, val cterms: Cterm*) extends IsabelleMLException(isabelle, id)
/** Represents an ML exception `THM (msg, index, theorems)`. See [[MLException.ExceptionManager]] for a way to make [[Isabelle]] raise such exceptions instead of generic [[IsabelleMLException]]s. */
final class ThmMLException private[exceptions](override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String, val index: Long, val theorems: Thm*) extends IsabelleMLException(isabelle, id)
/** Represents an ML exception `Match`. See [[MLException.ExceptionManager]] for a way to make [[Isabelle]] raise such exceptions instead of generic [[IsabelleMLException]]s. */
final class MatchMLException private[exceptions](override val isabelle: Isabelle, override val id: Isabelle.ID) extends IsabelleMLException(isabelle, id)
