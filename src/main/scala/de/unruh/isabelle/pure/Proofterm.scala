package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle.{DInt, DList, DObject, DString, Data}
import de.unruh.isabelle.control.{Isabelle, IsabelleMiscException, IsabelleMLException, IsabelleProtocolException, OperationCollection}
import de.unruh.isabelle.mlvalue.{MLRetrieveFunction, MLStoreFunction, MLValue, MLValueWrapper, Version}

import scala.concurrent.{ExecutionContext, Future}
import Implicits.{positionConverter, termConverter, theoryConverter, thmConverter, typConverter}
import org.jetbrains.annotations.ApiStatus.Experimental

import scala.annotation.tailrec

/** Support for Isabelle proofterms. '''Experimental and incomplete.'''
 * May throw [[scala.NotImplementedError NotImplementedError]] and change without notice. Not documented.
 * */
@Experimental
sealed trait Proofterm

/** Support for Isabelle proofterms. '''Experimental and incomplete.'''
 * May throw [[scala.NotImplementedError NotImplementedError]] and change without notice. Not documented.
 * */
@Experimental
object Proofterm extends OperationCollection {
  case object MinProof extends Proofterm
  final case class AppP(proof1: Proofterm, proof2: Proofterm) extends Proofterm
  final case class Appt(proof: Proofterm, term: Option[Term]) extends Proofterm
  final case class AbsP(name: String, term: Option[Term], proof: Proofterm) extends Proofterm
  final case class Abst(name: String, typ: Option[Typ], proof: Proofterm) extends Proofterm
  final case class Hyp(term: Term) extends Proofterm
  final case class PAxm(name: String, term: Term, typ: Option[List[Typ]]) extends Proofterm
  final case class PBound(index: Int) extends Proofterm
  final case class OfClass(typ: Typ, clazz: String) extends Proofterm
  final case class Oracle(name: String, term: Term, typ: Option[List[Typ]]) extends Proofterm

  final case class PThm(header: ThmHeader, body: ThmBody) extends Proofterm {
    def proof(implicit isabelle: Isabelle, executionContext: ExecutionContext): Proofterm =
      // I do not know why it needs the explicit Proofterm.converter here but I got compilation errors without it.
      body.proofOpenMlValue.retrieveNow(Proofterm.converter, implicitly, implicitly)

    def fullProof(theory: Theory)(implicit isabelle: Isabelle, executionContext: ExecutionContext): Proofterm =
      Ops.reconstruct_proof(theory.mlValue, header.prop.mlValue, body.proofOpenMlValue).retrieveNow
  }
  object PThm {
    def apply(thm: Thm)(implicit isabelle: Isabelle, executionContext: ExecutionContext): PThm = {
      @tailrec
      def strip(prf: Proofterm): PThm = prf match {
        case prf : PThm => prf
        case AppP(prf, _) => strip(prf)
        case Appt(prf, _) => strip(prf)
        case prf =>
          throw IsabelleMiscException(s"Unexpected proofterm while looking for PThm: $prf")
      }
      strip(thm.proofOf)
    }
  }

  final case class ThmHeader(serial: Long, pos: List[Position], theoryName: String, name: String, prop: Term, types: Option[List[Typ]])
  final class ThmBody(val mlValue: MLValue[ThmBody]) extends MLValueWrapper[ThmBody] {
    def proofOpenMlValue(implicit isabelle: Isabelle, executionContext: ExecutionContext): MLValue[Proofterm] =
      Ops.thm_body_proof_open(this)
  }
  object ThmBody extends MLValueWrapper.Companion[ThmBody] {
    override protected val mlType: String = "Proofterm.thm_body"
    override protected def instantiate(mlValue: MLValue[ThmBody]): ThmBody = new ThmBody(mlValue)
  }


  //  def fromThm(thm: Thm)(implicit isabelle: Isabelle, executionContext: ExecutionContext): Proofterm = Ops.proof_of(thm).retrieveNow
/*  def fromThm(context: Context, name: String, fullProof: Boolean = false)
             (implicit isabelle: Isabelle, executionContext: ExecutionContext): (Term, Proofterm) = {
    @tailrec
    def strip(prf: Proofterm): (Term, MLValue[Proofterm]) = prf match {
      case PThm(name2, prop, _, proof2) =>
        if (name2 == name)
          (prop, proof2)
        else
          throw IsabelleException(s"Unexpected PThm when searching for proof of theorem $name (wrong name): $prf")
      case AppP(prf, _) => strip(prf)
      case Appt(prf, _) => strip(prf)
      case prf =>
        throw IsabelleException(s"Unexpected proofterm for theorem $name: $prf")
    }
    val thm = Thm(context, name)
    val proof = fromThm(thm)
    val (prop, proof2) = strip(proof)
    val proof3 = if (fullProof) Ops.reconstruct_proof(thm.theoryOf.mlValue, prop.mlValue, proof2) else proof2
    (prop, proof3.retrieveNow)
  }*/

  //noinspection TypeAnnotation
  protected class Ops(implicit isabelle: Isabelle, executionContext: ExecutionContext) {
    if (!Version.from2020)
      throw IsabelleMiscException("Proofterms are supported only for Isabelle >=2020, not " + Version.versionString)

    import Proofterm.converter
    import MLValue.compileFunction
    val proof_of = compileFunction[Thm, Proofterm]("Thm.proof_of")
    val reconstruct_proof = compileFunction[Theory, Term, Proofterm, Proofterm](
      "fn (thy, t, prf) => Proofterm.reconstruct_proof thy t prf")
    val thm_body_proof_open = compileFunction[ThmBody, Proofterm]("Proofterm.thm_body_proof_open")

    val ofClassName = if (Version.from2021) "PClass" else "OfClass"

    val retrieve = MLRetrieveFunction[Proofterm](s"""
      let fun opt f NONE = DList []
            | opt f (SOME x) = DList [f x]
          fun list f l = DList (map f l)
          val typ = DObject o E_Typ
          val term = DObject o E_Term
          val position = DObject o E_Position
          fun hdr {serial, pos, theory_name, name, prop, types} =
              DList[DInt serial, list position pos, DString theory_name, DString name, term prop, opt (list typ) types]
          fun f MinProof = DInt 0
            | f (PBound i) = DList [DInt 1, DInt i]
            | f (Abst (name,T,prf)) = DList [DInt 2, DString name, opt typ T, f prf]
            | f (AbsP (name,t,prf)) = DList [DInt 3, DString name, opt term t, f prf]
            | f (prf % t) = DList [DInt 4, f prf, opt term t]
            | f (prf %% prf') = DList [DInt 5, f prf, f prf']
            | f (Hyp t) = DList [DInt 6, term t]
            | f (PAxm (name, t, Ts)) = DList [DInt 7, DString name, term t, opt (list typ) Ts]
            | f ($ofClassName (T, class)) = DList [DInt 8, typ T, DString class]
            | f (Oracle (name, t, Ts)) = DList [DInt 9, DString name, term t, opt (list typ) Ts]
            | f (PThm (header, body)) = DList [DInt 10, hdr header, DObject (${ThmBody.exceptionName} body)]
        in f end
        """)
//    val store = MLStoreFunction[Proofterm]("""fn DInt 0 => MinProof""")
  }

  override protected def newOps(implicit isabelle:  Isabelle, ec:  ExecutionContext) = new Ops

  implicit object converter extends MLValue.Converter[Proofterm] {
    private def opt[A](f: Data => A)(dlist: Data): Option[A] = dlist match {
      case DList() => None
      case DList(x) => Some(f(x))
    }

    private def obj[A : MLValue.Converter](data: Data)
                                          (implicit isabelle: Isabelle, executionContext: ExecutionContext) : A =
      data match {
        case DObject(id) => MLValue.unsafeFromId[A](id).retrieveNow
      }

    //    private def term(id: Isabelle.ID) = MLValue.unsafeFromId[Term](id).retrieveNow
    //    private def typ(id: Isabelle.ID) = MLValue.unsafeFromId[Typ](id).retrieveNow
    //    private def pos(id: Isabelle.ID) = MLValue.unsafeFromId[Position](id).retrieveNow
    private def list[A](f: Data => A)(dlist: Data) : List[A] = dlist match {
      case DList(list@_*) => list.map(f).toList
    }
    //    private def typList(ids: Data) = ids match { case DList(list@_*) => list.map { case DObject(id) => typ(id) }.toList }

    private def dataToThmHeader(data: Data)(implicit isabelle: Isabelle, executionContext: ExecutionContext) : ThmHeader = data match {
      case DList(DInt(serial), pos, DString(theoryName), DString(name), prop, types) =>
        ThmHeader(serial = serial, pos = list(obj[Position])(pos), theoryName = theoryName, name = name,
          prop = obj[Term](prop), opt(list(obj[Typ]))(types))
    }

    private def dataToProofterm(data: Data)(implicit isabelle: Isabelle, ec: ExecutionContext) : Proofterm = {
      data match {
        case DInt(0) => MinProof
        case DList(DInt(1), DInt(i)) => PBound(i.toInt)
        case DList(DInt(2), DString(name), typ, prf) => Abst(name, opt(obj[Typ])(typ), dataToProofterm(prf))
//        case DList(DInt(2), DString(name), prf) => Abst(name, None, dataToProofterm(prf))
        case DList(DInt(3), DString(name), term, prf) => AbsP(name, opt(obj[Term])(term), dataToProofterm(prf))
//        case DList(DInt(3), DString(name), prf) => AbsP(name, None, dataToProofterm(prf))
        case DList(DInt(4), prf, term) => Appt(dataToProofterm(prf), opt(obj[Term])(term))
//        case DList(DInt(4), prf) => Appt(dataToProofterm(prf), None)
        case DList(DInt(5), prf1, prf2) => AppP(dataToProofterm(prf1), dataToProofterm(prf2))
        case DList(DInt(6), term) => Hyp(obj[Term](term))
        case DList(DInt(7), DString(name), term, typs: DList) => PAxm(name, obj[Term](term), opt(list(obj[Typ]))(typs))
//        case DList(DInt(7), DString(name), DObject(termId)) => PAxm(name, term(termId), None)
        case DList(DInt(8), typ, DString(clazz)) => OfClass(obj[Typ](typ), clazz)
        case DList(DInt(9), DString(name), term, typs) => Oracle(name, obj[Term](term), opt(list(obj[Typ]))(typs))
//        case DList(DInt(9), DString(name), DObject(termId)) => Oracle(name, term(termId), None)
        case DList(DInt(10), header, body) => PThm(dataToThmHeader(header), obj[ThmBody](body))
//          PThm(name = name, serial = serial, prop = term(propId),
//            types = dlistOpt(typList, types), proofMlValue = MLValue.unsafeFromId[Proofterm](proofId))
        case _ => throw IsabelleProtocolException(s"Received unexpected data: $data")
      }
    }
/*    private def prooftermToData(prf: Proofterm) = prf match {
      case MinProof => DInt(0)
    }*/

    override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = "Proofterm.proof"
    override def retrieve(value:  MLValue[Proofterm])(implicit isabelle: Isabelle, ec:  ExecutionContext): Future[Proofterm] =
      Ops.retrieve(value).map(dataToProofterm)

    override def store(value:  Proofterm)(implicit isabelle:  Isabelle, ec: ExecutionContext): MLValue[Proofterm] =
      ???
//      Ops.store(prooftermToData(value))


    override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = "fn E_Proofterm prf => prf"
    override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = "E_Proofterm"
  }
}
