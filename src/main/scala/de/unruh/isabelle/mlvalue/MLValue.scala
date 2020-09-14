package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle.{DInt, DList, DObject, DString, Data, ID}
import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Implicits.{booleanConverter, intConverter, listConverter, longConverter, mlValueConverter, optionConverter, stringConverter, tuple2Converter, tuple3Converter, tuple4Converter, tuple5Converter, tuple6Converter, tuple7Converter}
import MLValue.{Converter, Ops, logger}
import org.log4s
import scalaz.Id.Id

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

/** A type safe wrapper for values stored in the Isabelle process managed by [[control.Isabelle Isabelle]].
 *
 * As explained in the documentation of [[control.Isabelle Isabelle]], the Isabelle process has an object store of values,
 * and the class [[control.Isabelle.ID Isabelle.ID]] is a reference to an object in that object store. However, values of different
 * types share the same object store. Thus [[control.Isabelle.ID Isabelle.ID]] is not type safe: there are compile time guarantees
 * that the value referenced by that ID has a specific type.
 *
 * [[MLValue]][A] is a thin wrapper around an ID [[id]] (more precisely, a future holding an ID). It is guaranteed
 * that [[id]] references a value in the Isabelle process of an ML type corresponding to `A` (or throw an exception).
 * (It is possible to violate this guarantee by type-casting the [[MLValue]] though.) For supported types `A`,
 * it is possible to automatically translate a Scala value `x` of type `A` into an `MLValue[A]` (which behind the scenes
 * means that `x` is transferred to the Isabelle process and added to the object store) and also to automatically
 * translate an `MLValue[A]` back to a Scala value of type `A`. Supported types are [[scala.Int Int]], [[scala.Long Long]],
 * [[scala.Boolean Boolean]], [[scala.Unit Unit]], [[java.lang.String String]], and lists and tuples (max. 7 elements)
 * of supported types. (It is also possible for `A` to be the type `MLValue[...]`, see [[MLValueConverter]] for
 * explanations (TODO add explanations).) It is possible to
 * add support for other types, see [[MLValue.Converter]] for instructions (TODO add instructions). Using this
 * mechanism, support for the terms, types, theories, contexts, and theorems has been added in package
 * [[de.unruh.isabelle.pure]].
 *
 * In more detail:
 *  - For any supported Scala type `A` there should be a unique corresponding ML type `a`.
 *    (This is not enforced if we add support for new types, but if we violate this, we loose type safety.)
 *  - Several Scala types `A` are allowed to correspond to the same ML type `a`. (E.g., both [[scala.Long Long]] and [[scala.Int Int]]
 *    correspond to the unbounded `int` in ML.)
 *  - For each supported type `A`, the following must be specified (via an implicit [[MLValue.Converter]]):
 *    - an encoding of `a` as an exception (to be able to store it in the object store)
 *    - ML functions to translate between `a` and exceptions and back
 *    - retrieve function: how to retrieve an exception encoding an ML value of type `a` and translate it into an `A` in Scala
 *    - store function: how to translate an `A` in Scala into an an exception encoding an ML value of type `a` and store it in the
 *      object store.
 *  - `MLValue(x)` automatically translates `x:A` into a value of type `a` (using the retrieve function) and returns
 *    an `MLValue[A]`.
 *  - If `m : MLValue[A]`, then `m.`[[retrieve]] (asynchronous) and `m.`[[retrieveNow]] (synchronous) decode the ML
 *    value in the object store and return a Scala value of type `A`.
 *  - ML code that operates on values in the Isabelle process can be specified using [[MLValue.compileValue]]
 *    and [[MLValue.compileFunction]]. This ML code directly operates on the corresponding ML type `a` and
 *    does not need to consider the encoding of ML values as exceptions or how ML types are serialized to be transferred
 *    to Scala (all this is handled automatically behind the scenes using the information provided by the implicit
 *    [[MLValue.Converter]]).
 *  - To be able to use the automatic conversions etc., converters need to be imported for supported types.
 *    The converters provided by this package can be imported by `import [[de.unruh.isabelle.mlvalue.MLValue.Implicits._]]`.
 *
 * // TODO examples
 *
 * // TODO add package documentation that points to this documentation
 *
 * @param id an ID referencing an object in the Isabelle process
 * @tparam A the Scala type corresponding to the ML type of the value referenced by [[id]]
 * */
class MLValue[A] private[isabelle](val id: Future[Isabelle.ID]) {
  def logError(message: => String)(implicit executionContext: ExecutionContext): this.type = {
    id.onComplete {
      case Success(_) =>
      case Failure(exception) => logger.error(exception)(message)
    }
    this
  }

  def debugInfo(implicit isabelle: Isabelle, ec: ExecutionContext): String =
    Ops.debugInfo[A](this).retrieveNow

  def stateString: String = id.value match {
    case Some(Success(_)) => ""
    case Some(Failure(_)) => " (failed)"
    case None => " (loading)"
  }

  def isReady: Boolean = id.isCompleted

  @inline def retrieve(implicit converter: Converter[A], isabelle: Isabelle, ec: ExecutionContext): Future[A] =
    converter.retrieve(this)

  @inline def retrieveNow(implicit converter: Converter[A], isabelle: Isabelle, ec: ExecutionContext): A =
    Await.result(retrieve, Duration.Inf)

  def function[D, R](implicit ev: MLValue[A] =:= MLValue[D => R]): MLFunction[D, R] =
    new MLFunction(id)

  def function2[D1, D2, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2)) => R]): MLFunction2[D1, D2, R] =
    new MLFunction2(id)

  def function3[D1, D2, D3, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2, D3)) => R]): MLFunction3[D1, D2, D3, R] =
    new MLFunction3(id)

  def function4[D1, D2, D3, D4, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2, D3, D4)) => R]): MLFunction4[D1, D2, D3, D4, R] =
    new MLFunction4(id)

  def function5[D1, D2, D3, D4, D5, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2, D3, D4, D5)) => R]): MLFunction5[D1, D2, D3, D4, D5, R] =
    new MLFunction5(id)

  def function6[D1, D2, D3, D4, D5, D6, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2, D3, D4, D5, D6)) => R]): MLFunction6[D1, D2, D3, D4, D5, D6, R] =
    new MLFunction6(id)

  def function7[D1, D2, D3, D4, D5, D6, D7, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2, D3, D4, D5, D6, D7)) => R]): MLFunction7[D1, D2, D3, D4, D5, D6, D7, R] =
    new MLFunction7(id)

  @inline def insertMLValue[C[_],B](implicit ev: A =:= C[B]): MLValue[C[MLValue[B]]] = this.asInstanceOf[MLValue[C[MLValue[B]]]]
  @inline def removeMLValue[C[_],B](implicit ev: A =:= C[MLValue[B]]): MLValue[C[B]] = this.asInstanceOf[MLValue[C[B]]]
}

class MLFunction[D,R] private[isabelle](id: Future[ID]) extends MLValue[D => R](id) {
  def apply(arg: MLValue[D])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    new MLValue(
      for (fVal <- this.id;
           xVal <- arg.id;
           fx <- isabelle.applyFunctionOld(fVal, xVal))
        yield fx
    )
  }

  def apply(arg: D)(implicit isabelle: Isabelle, ec: ExecutionContext, converter: Converter[D]): MLValue[R] =
    apply(MLValue(arg))
}

class MLFunction2[D1, D2, R] private[isabelle](id: Future[ID]) extends MLFunction[(D1, D2), R](id) {
  def apply(arg1: D1, arg2: D2)
           (implicit isabelle: Isabelle, ec: ExecutionContext, converter1: Converter[D1], converter2: Converter[D2]): MLValue[R] =
    apply((arg1,arg2))
  def apply(arg1: MLValue[D1], arg2: MLValue[D2])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    type C1[X] = Tuple2[X,MLValue[D2]]
    type C2[X] = Tuple2[D1,X]
    apply(MLValue((arg1, arg2)).removeMLValue[C1, D1].removeMLValue[C2, D2])
  }
}

class MLFunction3[D1, D2, D3, R] private[isabelle](id: Future[ID]) extends MLFunction[(D1, D2, D3), R](id) {
  def apply(arg1: D1, arg2: D2, arg3: D3)
           (implicit isabelle: Isabelle, ec: ExecutionContext,
            converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3]): MLValue[R] =
    apply((arg1,arg2,arg3))
  def apply(arg1: MLValue[D1], arg2: MLValue[D2], arg3: MLValue[D3])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    apply(MLValue((arg1,arg2,arg3)).asInstanceOf[MLValue[(D1,D2,D3)]])
  }
}

class MLFunction4[D1, D2, D3, D4, R] private[isabelle](id: Future[ID]) extends MLFunction[(D1, D2, D3, D4), R](id) {
  def apply(arg1: D1, arg2: D2, arg3: D3, arg4: D4)
           (implicit isabelle: Isabelle, ec: ExecutionContext,
            converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3], converter4: Converter[D4]): MLValue[R] =
    apply((arg1,arg2,arg3,arg4))
  def apply(arg1: MLValue[D1], arg2: MLValue[D2], arg3: MLValue[D3], arg4: MLValue[D4])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    apply(MLValue((arg1,arg2,arg3,arg4)).asInstanceOf[MLValue[(D1,D2,D3,D4)]])
  }
}

class MLFunction5[D1, D2, D3, D4, D5, R] private[isabelle](id: Future[ID]) extends MLFunction[(D1, D2, D3, D4, D5), R](id) {
  def apply(arg1: D1, arg2: D2, arg3: D3, arg4: D4, arg5: D5)
           (implicit isabelle: Isabelle, ec: ExecutionContext,
            converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3], converter4: Converter[D4],
            converter5: Converter[D5]): MLValue[R] =
    apply((arg1,arg2,arg3,arg4,arg5))
  def apply(arg1: MLValue[D1], arg2: MLValue[D2], arg3: MLValue[D3], arg4: MLValue[D4], arg5: MLValue[D5])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    apply(MLValue((arg1,arg2,arg3,arg4,arg5)).asInstanceOf[MLValue[(D1,D2,D3,D4,D5)]])
  }
}

class MLFunction6[D1, D2, D3, D4, D5, D6, R] private[isabelle](id: Future[ID]) extends MLFunction[(D1, D2, D3, D4, D5, D6), R](id) {
  def apply(arg1: D1, arg2: D2, arg3: D3, arg4: D4, arg5: D5, arg6: D6)
           (implicit isabelle: Isabelle, ec: ExecutionContext,
            converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3], converter4: Converter[D4],
            converter5: Converter[D5], converter6: Converter[D6]): MLValue[R] =
    apply((arg1,arg2,arg3,arg4,arg5,arg6))
  def apply(arg1: MLValue[D1], arg2: MLValue[D2], arg3: MLValue[D3], arg4: MLValue[D4], arg5: MLValue[D5], arg6: MLValue[D6])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    apply(MLValue((arg1,arg2,arg3,arg4,arg5,arg6)).asInstanceOf[MLValue[(D1,D2,D3,D4,D5,D6)]])
  }
}

class MLFunction7[D1, D2, D3, D4, D5, D6, D7, R] private[isabelle](id: Future[ID]) extends MLFunction[(D1, D2, D3, D4, D5, D6, D7), R](id) {
  def apply(arg1: D1, arg2: D2, arg3: D3, arg4: D4, arg5: D5, arg6: D6, arg7: D7)
           (implicit isabelle: Isabelle, ec: ExecutionContext,
            converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3], converter4: Converter[D4],
            converter5: Converter[D5], converter6: Converter[D6], converter7: Converter[D7]): MLValue[R] =
    apply((arg1,arg2,arg3,arg4,arg5,arg6,arg7))
  def apply(arg1: MLValue[D1], arg2: MLValue[D2], arg3: MLValue[D3], arg4: MLValue[D4], arg5: MLValue[D5], arg6: MLValue[D6], arg7: MLValue[D7])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    apply(MLValue((arg1,arg2,arg3,arg4,arg5,arg6,arg7)).asInstanceOf[MLValue[(D1,D2,D3,D4,D5,D6,D7)]])
  }
}

class MLStoreFunction[A](val id: Future[ID]) {
  def apply(data: Data)(implicit isabelle: Isabelle, ec: ExecutionContext, converter: Converter[A]): MLValue[A] =
    new MLValue(isabelle.applyFunction(this.id, data).map { case DObject(id) => id})
  def apply(data: Future[Data])(implicit isabelle: Isabelle, ec: ExecutionContext, converter: Converter[A]): MLValue[A] =
    new MLValue(for (data <- data; DObject(id) <- isabelle.applyFunction(this.id, data)) yield id)
}

object MLStoreFunction {
  def apply[A](ml: String)(implicit isabelle: Isabelle, converter: Converter[A]) : MLStoreFunction[A] =
    new MLStoreFunction(isabelle.storeValue(s"E_Function (D_Object o (${converter.valueToExn}) o ($ml))"))
}

class MLRetrieveFunction[A](val id: Future[ID]) {
  def apply(id: ID)(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Isabelle.Data] =
    isabelle.applyFunction(this.id, DObject(id))
  def apply(id: Future[ID])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Isabelle.Data] =
    for (id <- id; data <- apply(id)) yield data
  def apply(value: MLValue[A])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Data] =
    apply(value.id)
}

object MLRetrieveFunction {
  def apply[A](ml: String)(implicit isabelle: Isabelle, converter: Converter[A]) : MLRetrieveFunction[A] =
    new MLRetrieveFunction(isabelle.storeValue(s"E_Function (fn D_Object x => ($ml) ((${converter.exnToValueProtected}) x))"))
}

object MLValue extends OperationCollection {
  def matchFailExn(name: String) =
    s""" exn => error ("Match failed in ML code generated for $name: " ^ string_of_exn exn)"""

  def matchFailData(name: String) =
    s""" data => error ("Match failed in ML code generated for $name: " ^ string_of_data data)"""

  private val logger = log4s.getLogger

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext) : Ops = new Ops()
  //noinspection TypeAnnotation
  protected[mlvalue] class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {
    isabelle.executeMLCodeNow("exception E_List of exn list; exception E_Bool of bool; exception E_Option of exn option")

    val unitValue = MLValue.compileValueRaw[Unit]("E_Int 0")

    val retrieveTuple2 =
      MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing])]("fn (a,b) => D_List [D_Object a, D_Object b]")
    /*@inline def retrieveTuple2[A,B]: MLRetrieveFunction[(MLValue[A], MLValue[B])] =
      retrieveTuple2_.asInstanceOf[MLRetrieveFunction[(MLValue[A], MLValue[B])]]*/
    private val storeTuple2_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing])](s"fn D_List [D_Object a, D_Object b] => (a,b) | ${matchFailData("storeTuple2")}")
    @inline def storeTuple2[A,B]: MLStoreFunction[(MLValue[A], MLValue[B])] =
      storeTuple2_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B])]]

    val retrieveTuple3: MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])] =
      MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](s"fn (a,b,c) => D_List [D_Object a, D_Object b, D_Object c]")
    private val storeTuple3_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](s"fn D_List [D_Object a, D_Object b, D_Object c] => (a,b,c) | ${matchFailData("storeTuple3")}")
    @inline def storeTuple3[A,B,C]: MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C])] =
      storeTuple3_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C])]]

    val retrieveTuple4: MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])] =
      MLRetrieveFunction(s"fn (a,b,c,d) => D_List [D_Object a, D_Object b, D_Object c, D_Object d]")
    private val storeTuple4_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](s"fn D_List [D_Object a, D_Object b, D_Object c, D_Object d] => (a,b,c,d) | ${matchFailData("storeTuple4")}")
    @inline def storeTuple4[A,B,C,D] =
      storeTuple4_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D])]]

    val retrieveTuple5: MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])] =
      MLRetrieveFunction(s"fn (a,b,c,d,e) => D_List [D_Object a, D_Object b, D_Object c, D_Object d, D_Object e]")
    private val storeTuple5_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](
        s"fn D_List [D_Object a, D_Object b, D_Object c, D_Object d, D_Object e] => (a,b,c,d,e) | ${matchFailData("storeTuple5")}")
    @inline def storeTuple5[A,B,C,D,E] =
      storeTuple5_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D], MLValue[E])]]

    val retrieveTuple6: MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])] =
      MLRetrieveFunction(s"fn (a,b,c,d,e,f) => D_List [D_Object a, D_Object b, D_Object c, D_Object d, D_Object e, D_Object f]")
    private val storeTuple6_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](
        s"fn D_List [D_Object a, D_Object b, D_Object c, D_Object d, D_Object e, D_Object f] => (a,b,c,d,e,f) | ${matchFailData("storeTuple6")}")
    @inline def storeTuple6[A,B,C,D,E,F] =
      storeTuple6_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D], MLValue[E], MLValue[F])]]

    val retrieveTuple7: MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])] =
      MLRetrieveFunction(s"fn (a,b,c,d,e,f,g) => D_List [D_Object a, D_Object b, D_Object c, D_Object d, D_Object e, D_Object f, D_Object g]")
    private val storeTuple7_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](
        s"fn D_List [D_Object a, D_Object b, D_Object c, D_Object d, D_Object e, D_Object f, D_Object g] => (a,b,c,d,e,f,g) | ${matchFailData("storeTuple7")}")
    @inline def storeTuple7[A,B,C,D,E,F,G] =
      storeTuple7_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D], MLValue[E], MLValue[F], MLValue[G])]]

    val retrieveInt = MLRetrieveFunction[Int]("D_Int")
    val storeInt = MLStoreFunction[Int]("fn D_Int i => i")
    val retrieveLong = MLRetrieveFunction[Long]("D_Int")
    val storeLong = MLStoreFunction[Long]("fn D_Int i => i")

    val retrieveString: MLRetrieveFunction[String] = MLRetrieveFunction[String]("D_String")
    val storeString: MLStoreFunction[String] = MLStoreFunction[String]("fn D_String str => str")

//    val boolToInt : MLFunction[Boolean, Int] = MLValue.compileFunction[Boolean, Int]("fn true => 1 | false => 0")
    val boolTrue : MLValue[Boolean] = MLValue.compileValue("true")
    val boolFalse : MLValue[Boolean] = MLValue.compileValue("false")
    val retrieveBool : MLRetrieveFunction[Boolean] =
      MLRetrieveFunction("fn true => D_Int 1 | false => D_Int 0")

    private val optionNone_ = MLValue.compileValueRaw[Option[_]]("E_Option NONE")
    def optionNone[A]: MLValue[Option[A]] = optionNone_.asInstanceOf[MLValue[Option[A]]]
    private val optionSome_ = MLValue.compileFunctionRaw[Nothing, Option[Nothing]]("E_Option o SOME")
    def optionSome[A]: MLFunction[A, Option[A]] = optionSome_.asInstanceOf[MLFunction[A, Option[A]]]
    val retrieveOption : MLRetrieveFunction[Option[MLValue[Nothing]]] =
      MLRetrieveFunction("fn NONE => D_List [] | SOME x => D_List [D_Object x]")


    val retrieveList : MLRetrieveFunction[List[MLValue[Nothing]]] =
      MLRetrieveFunction("D_List o map D_Object")
    val storeList : MLStoreFunction[List[MLValue[Nothing]]] =
      MLStoreFunction(s"fn D_List list => map (fn D_Object obj => obj | ${matchFailData("storeList.map")}) list | ${matchFailData("storeList")}")

/*    private val listCons_  =
      MLValue.compileFunctionRaw[(Nothing,List[Nothing]), List[Nothing]]("fn E_Pair (x, E_List xs) => E_List (x::xs)")
        .function2[Nothing, List[Nothing], List[Nothing]]
    def listCons[A]: MLFunction2[A, List[A], List[A]] =
      listCons_.asInstanceOf[MLFunction2[A, List[A], List[A]]]
    private val listNil_ : MLValue[List[_]] = MLValue.compileValueRaw("E_List []")
    def listNil[A]: MLValue[List[A]] = listNil_.asInstanceOf[MLValue[List[A]]]
    val listIsNil_ : MLFunction[List[_], Boolean] =
      MLValue.compileFunctionRaw[List[_], Boolean]("fn E_List [] => E_Bool true | E_List _ => E_Bool false")
    def listIsNil[A]: MLFunction[List[A], Boolean] = listIsNil_.asInstanceOf[MLFunction[List[A], Boolean]]
    val destCons_ : MLFunction[List[_], (_,List[_])] = MLValue.compileFunctionRaw[List[_], (_,List[_])]("fn E_List (x::xs) => E_Pair (x, E_List xs)")
    def destCons[A]: MLFunction[List[A], (A, List[A])] = destCons_.asInstanceOf[MLFunction[List[A], (A,List[A])]]*/

    val debugInfo_ : MLFunction[MLValue[Nothing], String] =
      compileFunctionRaw[MLValue[Nothing], String]("E_String o string_of_exn")
    def debugInfo[A]: MLFunction[MLValue[A], String] = debugInfo_.asInstanceOf[MLFunction[MLValue[A], String]]
  }

  abstract class Converter[A] {
    def retrieve(value: MLValue[A])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[A]
    def store(value: A)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[A]
    val exnToValue : String
    val valueToExn : String
    final def exnToValueProtected = s"""fn e => (($exnToValue) e handle Match => error ("Match failed in exnToValue (" ^ string_of_exn e ^ ")"))"""
  }

  @inline def apply[A](value: A)(implicit conv: Converter[A], isabelle: Isabelle, executionContext: ExecutionContext) : MLValue[A] =
    conv.store(value)

  def compileValueRaw[A](ml: String)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[A] =
    new MLValue[A](isabelle.storeValue(ml)).logError(s"""Error while compiling value "$ml":""")

  def compileValue[A](ml: String)(implicit isabelle: Isabelle, ec: ExecutionContext, converter: Converter[A]): MLValue[A] =
    compileValueRaw[A](s"(${converter.valueToExn}) ($ml)")

  def compileFunctionRaw[D, R](ml: String)(implicit isabelle: Isabelle, ec: ExecutionContext): MLFunction[D, R] =
    new MLFunction[D,R](isabelle.storeValue(s"E_Function (fn D_Object x => ($ml) x |> D_Object)")).logError(s"""Error while compiling function "$ml":""")

  def compileFunction[D, R](ml: String)(implicit isabelle: Isabelle, ec: ExecutionContext, converterA: Converter[D], converterB: Converter[R]): MLFunction[D, R] =
    compileFunctionRaw(s"(${converterB.valueToExn}) o ($ml) o (${converterA.exnToValueProtected})")

  def compileFunction[D1, D2, R](ml: String)
                                 (implicit isabelle: Isabelle, ec: ExecutionContext,
                                 converter1: Converter[D1], converter2: Converter[D2], converterR: Converter[R]): MLFunction2[D1, D2, R] =
    compileFunction[(D1,D2), R](ml).function2

  def compileFunction[D1, D2, D3, R](ml: String)
                                    (implicit isabelle: Isabelle, ec: ExecutionContext,
                                     converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3], converterR: Converter[R]): MLFunction3[D1, D2, D3, R] =
    compileFunction[(D1,D2,D3), R](ml).function3

  def compileFunction[D1, D2, D3, D4, R](ml: String)
                                    (implicit isabelle: Isabelle, ec: ExecutionContext,
                                     converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3], converter4: Converter[D4], converterR: Converter[R]): MLFunction4[D1, D2, D3, D4, R] =
    compileFunction[(D1,D2,D3,D4), R](ml).function4

  def compileFunction[D1, D2, D3, D4, D5, R](ml: String)
                                        (implicit isabelle: Isabelle, ec: ExecutionContext,
                                         converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3],
                                         converter4: Converter[D4], converter5: Converter[D5], converterR: Converter[R]): MLFunction5[D1, D2, D3, D4, D5, R] =
    compileFunction[(D1,D2,D3,D4,D5), R](ml).function5

  def compileFunction[D1, D2, D3, D4, D5, D6, R](ml: String)
                                                (implicit isabelle: Isabelle, ec: ExecutionContext,
                                                 converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3],
                                                 converter4: Converter[D4], converter5: Converter[D5], converter6: Converter[D6], converterR: Converter[R]): MLFunction6[D1, D2, D3, D4, D5, D6, R] =
    compileFunction[(D1,D2,D3,D4,D5,D6), R](ml).function6

  def compileFunction[D1, D2, D3, D4, D5, D6, D7, R](ml: String)
                                                    (implicit isabelle: Isabelle, ec: ExecutionContext,
                                                     converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3],
                                                     converter4: Converter[D4], converter5: Converter[D5], converter6: Converter[D6],
                                                     converter7: Converter[D7], converterR: Converter[R]): MLFunction7[D1, D2, D3, D4, D5, D6, D7, R] =
    compileFunction[(D1,D2,D3,D4,D5,D6,D7), R](ml).function7


  object Implicits {
    @inline implicit val booleanConverter: BooleanConverter.type = BooleanConverter
    @inline implicit val intConverter: IntConverter.type = IntConverter
    @inline implicit val longConverter: LongConverter.type = LongConverter
    @inline implicit val unitConverter: UnitConverter.type = UnitConverter
    @inline implicit val stringConverter: StringConverter.type = StringConverter
    @inline implicit def listConverter[A](implicit converter: Converter[A]): ListConverter[A] = new ListConverter()(converter)
    @inline implicit def optionConverter[A](implicit converter: Converter[A]): OptionConverter[A] = new OptionConverter()(converter)
    @inline implicit def tuple2Converter[A,B](implicit a: Converter[A], b: Converter[B]): Tuple2Converter[A, B] = new Tuple2Converter(a,b)
    @inline implicit def tuple3Converter[A,B,C](implicit a: Converter[A], b: Converter[B], c: Converter[C]): Tuple3Converter[A, B, C] = new Tuple3Converter(a,b,c)
    @inline implicit def tuple4Converter[A,B,C,D](implicit a: Converter[A], b: Converter[B], c: Converter[C], d: Converter[D]): Tuple4Converter[A, B, C, D] = new Tuple4Converter(a,b,c,d)
    @inline implicit def tuple5Converter[A,B,C,D,E](implicit a: Converter[A], b: Converter[B], c: Converter[C], d: Converter[D], e: Converter[E]): Tuple5Converter[A, B, C, D, E] = new Tuple5Converter(a,b,c,d,e)
    @inline implicit def tuple6Converter[A,B,C,D,E,F](implicit a: Converter[A], b: Converter[B], c: Converter[C], d: Converter[D], e: Converter[E], f: Converter[F]): Tuple6Converter[A, B, C, D, E, F] = new Tuple6Converter(a,b,c,d,e,f)
    @inline implicit def tuple7Converter[A,B,C,D,E,F,G](implicit a: Converter[A], b: Converter[B], c: Converter[C], d: Converter[D], e: Converter[E], f: Converter[F], g: Converter[G]): Tuple7Converter[A,B,C,D,E,F,G] = new Tuple7Converter(a,b,c,d,e,f,g)
    @inline implicit def mlValueConverter[A]: MLValueConverter[A] = new MLValueConverter[A]
  }
}





