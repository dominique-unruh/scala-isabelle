package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle.{DInt, DList, DObject, DString, Data}
import de.unruh.isabelle.control.{Isabelle, IsabelleException, IsabelleProtocolException, OperationCollection}
import de.unruh.isabelle.mlvalue.{MLRetrieveFunction, MLStoreFunction, MLValue}

import scala.concurrent.{ExecutionContext, Future}
import Implicits.{termConverter, theoryConverter, thmConverter, typConverter}
import jdk.jfr.Experimental

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
  final case class PThm(name: String, serial: Long, prop: Term, types: Option[List[Typ]], proofMlValue: MLValue[Proofterm]) extends Proofterm {
    def proof(implicit isabelle: Isabelle, executionContext: ExecutionContext): Proofterm = proofMlValue.retrieveNow
    def fullProof(theory: Theory)(implicit isabelle: Isabelle, executionContext: ExecutionContext): Proofterm =
      Ops.reconstruct_proof(theory.mlValue, prop.mlValue, proofMlValue).retrieveNow
  }
  object PThm {
    def apply(thm: Thm)(implicit isabelle: Isabelle, executionContext: ExecutionContext): PThm = {
      @tailrec
      def strip(prf: Proofterm): PThm = prf match {
        case prf : PThm => prf
        case AppP(prf, _) => strip(prf)
        case Appt(prf, _) => strip(prf)
        case prf =>
          throw IsabelleException(s"Unexpected proofterm while looking for PThm: $prf")
      }
      strip(thm.proofOf)
    }
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
    import Proofterm.converter
    import MLValue.compileFunction
    // TODO: PThm is only partially represented
    val proof_of = compileFunction[Thm, Proofterm]("Thm.proof_of")
    val reconstruct_proof = compileFunction[Theory, Term, Proofterm, Proofterm](
      "fn (thy, t, prf) => Proofterm.reconstruct_proof thy t prf")

    val retrieve = MLRetrieveFunction[Proofterm]("""
      let fun dlist_opt f NONE = DList []
            | dlist_opt f (SOME x) = DList [f x]
          fun f MinProof = DInt 0
            | f (PBound i) = DList [DInt 1, DInt i]
            | f (Abst (name,SOME typ,prf)) = DList [DInt 2, DString name, DObject (E_Typ typ), f prf]
            | f (Abst (name,NONE,prf)) = DList [DInt 2, DString name, f prf]
            | f (AbsP (name,SOME t,prf)) = DList [DInt 3, DString name, DObject (E_Term t), f prf]
            | f (AbsP (name,NONE,prf)) = DList [DInt 3, DString name, f prf]
            | f (prf % SOME t) = DList [DInt 4, f prf, DObject (E_Term t)]
            | f (prf % NONE) = DList [DInt 4, f prf]
            | f (prf %% prf') = DList [DInt 5, f prf, f prf']
            | f (Hyp t) = DList [DInt 6, DObject (E_Term t)]
            | f (PAxm (name, t, SOME Ts)) = DList [DInt 7, DString name, DObject (E_Term t), DList (map (DObject o E_Typ) Ts)]
            | f (PAxm (name, t, NONE)) = DList [DInt 7, DString name, DObject (E_Term t)]
            | f (OfClass (T, class)) = DList [DInt 8, DObject (E_Typ T), DString class]
            | f (Oracle (name, t, SOME Ts)) = DList [DInt 9, DString name, DObject (E_Term t), DList (map (DObject o E_Typ) Ts)]
            | f (Oracle (name, t, NONE)) = DList [DInt 9, DString name, DObject (E_Term t)]
            | f (PThm ({name,serial,prop,types,...}, body)) =
                 DList [DInt 10, DInt serial, DString name, dlist_opt (DList o map (DObject o E_Typ)) types,
                        DObject (E_Term prop), DObject (E_Proofterm (Proofterm.thm_body_proof_open body))]
        in f end
        """)
//    val store = MLStoreFunction[Proofterm]("""fn DInt 0 => MinProof""")
  }

  override protected def newOps(implicit isabelle:  Isabelle, ec:  ExecutionContext) = new Ops

  implicit object converter extends MLValue.Converter[Proofterm] {
    private def dataToProofterm(data: Data)(implicit isabelle: Isabelle, ec: ExecutionContext) : Proofterm = {
      def term(id: Isabelle.ID) = MLValue.unsafeFromId[Term](id).retrieveNow
      def typ(id: Isabelle.ID) = MLValue.unsafeFromId[Typ](id).retrieveNow
      def typList(ids: Data) = ids match { case DList(list@_*) => list.map { case DObject(id) => typ(id) }.toList }
      def dlistOpt[A](f: Data => A, dlist: Data) = dlist match {
        case DList() => None
        case DList(x) => Some(f(x))
      }
      data match {
        case DInt(0) => MinProof
        case DList(DInt(1), DInt(i)) => PBound(i.toInt)
        case DList(DInt(2), DString(name), DObject(typId), prf) => Abst(name, Some(typ(typId)), dataToProofterm(prf))
        case DList(DInt(2), DString(name), prf) => Abst(name, None, dataToProofterm(prf))
        case DList(DInt(3), DString(name), DObject(termId), prf) => AbsP(name, Some(term(termId)), dataToProofterm(prf))
        case DList(DInt(3), DString(name), prf) => AbsP(name, None, dataToProofterm(prf))
        case DList(DInt(4), prf, DObject(termId)) => Appt(dataToProofterm(prf), Some(term(termId)))
        case DList(DInt(4), prf) => Appt(dataToProofterm(prf), None)
        case DList(DInt(5), prf1, prf2) => AppP(dataToProofterm(prf1), dataToProofterm(prf2))
        case DList(DInt(6), DObject(termId)) => Hyp(term(termId))
        case DList(DInt(7), DString(name), DObject(termId), typs: DList) =>
          PAxm(name, term(termId), Some(typList(typs)))
        case DList(DInt(7), DString(name), DObject(termId)) => PAxm(name, term(termId), None)
        case DList(DInt(8), DObject(typId), DString(clazz)) => OfClass(typ(typId), clazz)
        case DList(DInt(9), DString(name), DObject(termId), DList(typs@_*)) =>
          Oracle(name, term(termId), Some(typs.map { case DObject(id) => typ(id) }.toList))
        case DList(DInt(9), DString(name), DObject(termId)) => Oracle(name, term(termId), None)
        case DList(DInt(10), DInt(serial), DString(name), types, DObject(propId), DObject(proofId)) =>
          PThm(name = name, serial = serial, prop = term(propId),
            types = dlistOpt(typList, types), proofMlValue = MLValue.unsafeFromId[Proofterm](proofId))
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
