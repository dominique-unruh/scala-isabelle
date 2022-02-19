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

// DOCUMENT
class Exn protected (isabelle: Isabelle, id: ID) extends IsabelleMLException(isabelle, id)

// DOCUMENT
object Exn extends OperationCollection {
  def unsafeFromId(id: ID)(implicit isabelle: Isabelle) : Exn = new Exn(isabelle, id)
  def apply(exception: IsabelleMLException) = new Exn(exception.isabelle, exception.id)

  class ExnConverter extends Converter[Exn] {
    final override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = "exn"
    final override def retrieve(value: MLValue[Exn])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Exn] = {
      for (id <- value.id;
           exn = Exn.unsafeFromId(id);
           exn2 <-recognize(exn))
        yield exn2
    }

    final override def store(value: Exn)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Exn] = MLValue.unsafeFromId(value.id)
    final override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = "(fn e : exn => e)"
    final override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = "(fn e : exn => e)"

    def recognize(exception: Exn)(implicit isabelle: Isabelle, ec: ExecutionContext) : Future[Exn] = Future.successful(exception)
  }

  implicit object simpleExnConverter extends ExnConverter
  implicit object distinguishingExnConverter extends ExnConverter {
    override def recognize(exception: Exn)(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Exn] =
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

  def recognizeException(exception: IsabelleMLException, fallback: IsabelleMLException => Exn = Exn(_))
                        (implicit isabelle: Isabelle, ec: ExecutionContext): Future[Exn] = {
    val id = exception.id
    for (DList(DString(typ), args @_*) <- Ops.recognizeException(DObject(id)).retrieve)
      yield typ match {
        case "ERROR" => val Seq(DString(msg)) = args; new ErrorMLExn(isabelle, id, msg)
        case "Fail" => val Seq(DString(msg)) = args; new FailMLExn(isabelle, id, msg)
        case "THEORY" =>
          val Seq(DString(msg), thys @_*) = args
          val thys2 = for (DObject(thy) <- thys) yield MLValue.unsafeFromId[Theory](thy).retrieveNow
          new TheoryMLExn(isabelle, id, msg, thys2 : _*)
        case "TERM" =>
          val Seq(DString(msg), terms @_*) = args
          val terms2 = for (DObject(t) <- terms) yield MLValue.unsafeFromId[Term](t).retrieveNow
          new TermMLExn(isabelle, id, msg, terms2 : _*)
        case "CTERM" =>
          val Seq(DString(msg), cterms @_*) = args
          val cterms2 = for (DObject(t) <- cterms) yield MLValue.unsafeFromId[Cterm](t).retrieveNow
          new CtermMLExn(isabelle, id, msg, cterms2 : _*)
        case "THM" =>
          val Seq(DString(msg), DInt(i), thms @_*) = args
          val thms2 = for (DObject(thm) <- thms) yield MLValue.unsafeFromId[Thm](thm).retrieveNow
          new ThmMLExn(isabelle, id, msg, i, thms2 : _*)
        case "TYPE" =>
          val Seq(DString(msg), DList(typs @_*), DList(terms @_*)) = args
          val typs2 = for (DObject(typ) <- typs) yield MLValue.unsafeFromId[Typ](typ).retrieveNow
          val terms2 = for (DObject(t) <- terms) yield MLValue.unsafeFromId[Term](t).retrieveNow
          new TypeMLExn(isabelle, id, msg, typs2, terms2)
        case "Match" => new MatchMLExn(isabelle, id)
        case "unknown" =>
          if (fallback==null) null; else fallback(exception)
        case _ => assert(assertion = false, "unreachable code"); null
      }
  }

  class ExceptionManager(isabelle: Isabelle) extends control.ExceptionManager {
    implicit val isa: Isabelle = isabelle
    import scala.concurrent.ExecutionContext.Implicits.global
    private var context : Context = _

    def setContext(ctxt: Context): Unit = context = ctxt

    override def createException(id: ID): Exception =
      Await.result(recognizeException(Exn.unsafeFromId(id)), Duration.Inf)

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

final class ErrorMLExn private[exceptions] (override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String) extends Exn(isabelle, id)
final class FailMLExn private[exceptions] (override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String) extends Exn(isabelle, id)
final class TheoryMLExn private[exceptions] (override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String, val theories: Theory*) extends Exn(isabelle, id)
final class TermMLExn private[exceptions] (override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String, val terms: Term*) extends Exn(isabelle, id)
final class TypeMLExn private[exceptions](override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String, val typs: Seq[Typ], val terms: Seq[Term]) extends Exn(isabelle, id)
final class CtermMLExn private[exceptions] (override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String, val cterms: Cterm*) extends Exn(isabelle, id)
final class ThmMLExn private[exceptions] (override val isabelle: Isabelle, override val id: Isabelle.ID, val msg: String, val index: Long, val theorems: Thm*) extends Exn(isabelle, id)
final class MatchMLExn private[exceptions] (override val isabelle: Isabelle, override val id: Isabelle.ID) extends Exn(isabelle, id)
