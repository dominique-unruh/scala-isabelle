package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle.{DInt, DList, DObject, DString, Data, ID}
import de.unruh.isabelle.control.{Isabelle, IsabelleMiscException, IsabelleMLException, OperationCollection}
import de.unruh.isabelle.mlvalue.Implicits._
import MLValue.{Converter, Ops, logger}
import de.unruh.isabelle.misc.FutureValue
import org.log4s
import scalaz.Id.Id

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

// Implicits
import de.unruh.isabelle.control.Isabelle.executionContext

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
  * explanations.) It is possible to
  * add support for other types, see [[MLValue.Converter]] for instructions. Using this
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
  *  - `[[MLValue.apply MLValue]](x)` automatically translates `x:A` into a value of type `a` (using the retrieve function) and returns
  *    an `MLValue[A]`.
  *  - If `m : MLValue[A]`, then `m.`[[retrieve]] (asynchronous) and `m.`[[retrieveNow]] (synchronous) decode the ML
  *    value in the object store and return a Scala value of type `A`.
  *  - ML code that operates on values in the Isabelle process can be specified using [[MLValue.compileValue]]
  *    and [[MLValue.compileFunction[D,R]* MLValue.compileFunction]]. This ML code directly operates on the
  *    corresponding ML type `a` and
  *    does not need to consider the encoding of ML values as exceptions or how ML types are serialized to be transferred
  *    to Scala (all this is handled automatically behind the scenes using the information provided by the implicit
  *    [[MLValue.Converter]]).
  *  - To be able to use the automatic conversions etc., converters need to be imported for supported types.
  *    The converters provided by this package can be imported by `import [[de.unruh.isabelle.mlvalue.Implicits]]._`.
  *  - [[MLValue]]s are asynchronous, i.e., they may finish the computation of the contained value after creation of the
  *    `MLValue` object. (Like a [[scala.concurrent.Future Future]].) In particular, exception thrown during the
  *    computation are not thrown during object creation. The value inside an [[MLValue]] can be retrieved
  *    using [[retrieveNow]] (or [[retrieve]]), this '''may''' wait for the computation to finish and force the exceptions
  *    to be thrown. However, instead the value returned by [[retrieveNow]] may also be an asynchronous value in the
  *    sense that it is created before the computation defining it has finished. Thus exceptions thrown in the
  *    computation of the [[MLValue]] may be delayed even further and only show upon later computations with the
  *    returned object. To make sure that the computation of the [[MLValue]] has completed, use the methods of
  *    [[misc.FutureValue FutureValue]] (which [[MLValue]] inherits), such as [[misc.FutureValue.force force]].
  *  - We have the convention that if `A` is '''not''' a subtype of [[misc.FutureValue FutureValue]], then [[retrieveNow]]
  *    must throw the exceptions raised during the computation of the `MLValue`, and [[retrieve]] must
  *    return a future that holds those exceptions. (That is, in this case the considerations of the preceding
  *    bullet point do not apply.) For example, if `MLValue[Unit].retrieveNow` or `MLValue[Int].retrieveNow` guarantee
  *    that all exceptions are thrown (i.e., if those function calls complete without exception, the underlying
  *    computation is guaranteed to be have completed without exception). If `MLValue[Term].retrieveNow`
  *    completes without exception, this is not guaranteed (because [[pure.Term Term]] is a subtype of [[misc.FutureValue FutureValue]]).
  *    But `MLValue[Term].force.retrieveNow` and `MLValue[Term].retrieveNow.force` both guarantee that all exceptions
  *    are thrown.
  *
  * Note: Some operations take an [[control.Isabelle Isabelle]] instance as an implicit argument. It is required that this instance
  *       the same as the one relative to which the MLValue was created.
  *
  * A full example:
  * {{{
  *     implicit val isabelle: Isabelle = new Isabelle(...)
  *
  *     // Create an MLValue containing an integer
  *     val intML : MLValue[Int] = MLValue(123)
  *     // 123 is now stored in the object store
  *
  *     // Fetching the integer back
  *     val int : Int = intML.retrieveNow
  *     assert(int == 123)
  *
  *     // The type parameter of MLValue ensures that the following does not compile:
  *     // val string : String = intML.retrieveNow
  *
  *     // We write an ML function that squares an integer and converts it into a string
  *     val mlFunction : MLFunction[Int, String] =
  *       MLValue.compileFunction[Int, String]("fn i => string_of_int (i*i)")
  *
  *     // We can apply the function to an integer stored in the Isabelle process
  *     val result : MLValue[String] = mlFunction(intML)
  *     // The result is still stored in the Isabelle process, but we can retrieve it:
  *     val resultHere : String = result.retrieveNow
  *     assert(resultHere == "15129")
  * }}}
  * Not that the type annotations in this example are all optional, the compiler infers them automatically.
  * We have included them for clarity only.
  *
  * @param id the ID of the referenced object in the Isabelle process
  * @tparam A the Scala type corresponding to the ML type of the value referenced by [[id]]
  */
class MLValue[A] protected (/** the ID of the referenced object in the Isabelle process */ val id: Future[Isabelle.ID])
  extends FutureValue {
  def logError(message: => String)(implicit isabelle: Isabelle): this.type = {
    implicit val executionContext = Isabelle.executionContext
    id.onComplete {
      case Success(_) =>
      case Failure(exception) => logger.error(exception)(message)
    }
    this
  }

  /** Returns a textual representation of the value in the ML process as it is stored in the object store
   * (i.e., encoded as an exception). E.g., an integer 3 would be represented as "E_Int 3". */
  def debugInfo(implicit isabelle: Isabelle): String =
    Ops.debugInfo[A](this).retrieveNow

  override def await: Unit = Await.result(id, Duration.Inf)
  override def someFuture: Future[Any] = id

  /** Retrieves the value referenced by this MLValue from the Isabelle process.
   *
   * In particular, the value in the Isabelle process (a value in ML) is translated to a Scala value.
   *
   * @return Future holding the value (as a Scala value) or an [[control.IsabelleException IsabelleException]] if the computation of that
   *         value or the transfer to Scala failed.
   * @param converter This converter specifies how the value is to be retrieved from the Isabelle process and
   *                  translated into a Scala value of type `A`
   * @param isabelle The [[control.Isabelle Isabelle]] instance holding the value. This must be the same `Isabelle` instance
   *                 relative to which the `MLValue` was created. (Otherwise unspecified data is returned or an
   *                 exception thrown.) In an application with only a single `Isabelle` instance that instance
   *                 can safely be declared as an implicit.
   */
  @inline def retrieve(implicit converter: Converter[A], isabelle: Isabelle): Future[A] =
    converter.retrieve(this)

  /** Like retrieve but returns the Scala value directly instead of a future (blocks till the computation
   * and transfer finish). */
  @inline def retrieveNow(implicit converter: Converter[A], isabelle: Isabelle): A =
    Await.result(retrieve, Duration.Inf)

  /** Returns this MLValue as an [[MLFunction]], assuming this MLValue has a type of the form `MLValue[D => R]`.
   * If this MLValue is `MLValue[D => R]`, it means it references a function value in the ML process. Converting it
   * to an `MLFunction <: MLValue` gives us access to additional methods for applying this function.
   * @see [[MLFunction]]
   */
  def function[D, R](implicit ev: MLValue[A] =:= MLValue[D => R]): MLFunction[D, R] =
    MLFunction.unsafeFromId(id)


  /** Analogous to [[function]] but for functions that take a unit-valye as argument, i.e., `this : MLValue[Unit => R]`.
   * @see [[MLFunction0]] */
  def function0[R](implicit ev: MLValue[A] =:= MLValue[Unit => R]) : MLFunction0[R] =
    MLFunction0.unsafeFromId(id)

  /** Analogous to [[function]] but for functions that take a pair as argument, i.e., `this : MLValue[((D1, D2)) => R]`.
   * @see [[MLFunction2]] */
  def function2[D1, D2, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2)) => R]): MLFunction2[D1, D2, R] =
    MLFunction2.unsafeFromId(id)

  /** Analogous to [[function]] but for functions that take a 3-tuple as argument, i.e., `this : MLValue[((D1, D2, D3)) => R]`.
   * @see [[MLFunction3]] */
  def function3[D1, D2, D3, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2, D3)) => R]): MLFunction3[D1, D2, D3, R] =
    MLFunction3.unsafeFromId(id)

  /** Analogous to [[function]] but for functions that take a 4-tuple as argument, i.e., `this : MLValue[((D1, D2, D3, D4)) => R]`.
   * @see [[MLFunction4]] */
  def function4[D1, D2, D3, D4, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2, D3, D4)) => R]): MLFunction4[D1, D2, D3, D4, R] =
    MLFunction4.unsafeFromId(id)

  /** Analogous to [[function]] but for functions that take a 5-tuple as argument, i.e., `this : MLValue[((D1, D2, D3, D4, D5)) => R]`.
   * @see [[MLFunction5]] */
  def function5[D1, D2, D3, D4, D5, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2, D3, D4, D5)) => R]): MLFunction5[D1, D2, D3, D4, D5, R] =
    MLFunction5.unsafeFromId(id)

  /** Analogous to [[function]] but for functions that take a 6-tuple as argument, i.e., `this : MLValue[((D1, D2, D3, D4, D5, D6)) => R]`.
   * @see [[MLFunction6]] */
  def function6[D1, D2, D3, D4, D5, D6, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2, D3, D4, D5, D6)) => R]): MLFunction6[D1, D2, D3, D4, D5, D6, R] =
    MLFunction6.unsafeFromId(id)

  /** Analogous to [[function]] but for functions that take a 7-tuple as argument, i.e., `this : MLValue[((D1, D2, D3, D4, D5, D6, D7)) => R]`.
   * @see [[MLFunction7]] */
  def function7[D1, D2, D3, D4, D5, D6, D7, R](implicit ev: MLValue[A] =:= MLValue[((D1, D2, D3, D4, D5, D6, D7)) => R]): MLFunction7[D1, D2, D3, D4, D5, D6, D7, R] =
    MLFunction7.unsafeFromId(id)

  /** Specialized type cast that inserts `MLValue[]` in arbitrary positions in the type parameter of this MLValue.
   * E.g., we can type cast `this : MLValue[List[X]]` to `MLValue[List[MLValue[X]]]` by invoking `this.insertMLValue[List,X]`
   * Such type casts are safe because the the way `MLValue[...]` is interpreted in the type parameter to `MLValue` (see
   * [[MLValueConverter]]). The same type cast could be achieved using `.asInstanceOf`, but
   * `insertMLValue` guarantees that no unsafe cast is accidentally performed.
   */
  @inline def insertMLValue[C[_],B](implicit ev: A =:= C[B]): MLValue[C[MLValue[B]]] = this.asInstanceOf[MLValue[C[MLValue[B]]]]
  /** Specialized type cast that removes `MLValue[]` in arbitrary positions in the type parameter of this MLValue.
   * E.g., we can type cast `this : MLValue[List[MLValue[X]]]` to `MLValue[List[X]]` by invoking `this.removeMLValue[List,X]`
   * Such type casts are safe because the the way `MLValue[...]` is interpreted in the type parameter to `MLValue` (see
   * [[MLValueConverter]]). The same type cast could be achieved using `.asInstanceOf`, but
   * `insertMLValue` guarantees that no unsafe cast is accidentally performed.
   */
  @inline def removeMLValue[C[_],B](implicit ev: A =:= C[MLValue[B]]): MLValue[C[B]] = this.asInstanceOf[MLValue[C[B]]]
}


/** A compiled ML function specifically transmitting values from Scala to Isabelle.
 *
 * The function is created by `val f = MLStoreFunction(ml)` where `ml` is ML code of type
 * `data -> a`, and `a` is the ML type corresponding to `A`.
 *
 * When `f(data)` is invoked in Scala (with `data` of type [[control.Isabelle.Data Data]]), the compiled ML function `ml`
 * is applied to `data` (in the Isabelle process), and the resulting value is stored in the object store,
 * and an [[MLValue]] containing the ID is returned.
 *
 * An [[MLStoreFunction]] is particularly useful for writing [[MLValue.Converter.store store]] methods
 * when writing an [[MLValue.Converter]].
 *
 * The behavior of an [[MLStoreFunction]]`[A]` is very similar to an [[MLFunction]]`[Data,A]`
 * but more efficient. And the [[MLStoreFunction]] additionally does not access the
 * [[MLValue.Converter.store store]] and [[MLValue.Converter.retrieve retrieve]] functions of the converter
 * that is passed as an implicit argument. This is important because we use the [[MLStoreFunction]] for writing
 * those functions in the first place.
 **/
class MLStoreFunction[A] private (val id: Future[ID]) {
  /** Calls the compile ML function on `data` in the Isabelle process and returns
   * an [[MLValue]] containing the result of that function. */
  def apply(data: Data)(implicit isabelle: Isabelle): MLValue[A] = {
    MLValue.unsafeFromId(isabelle.applyFunction(this.id, data).map {
      case DObject(id) => id
      case _ => throw IsabelleMiscException("MLStoreFunction")
    })
  }

  /** Like [[apply(data:de* apply(Data)]] but `data` can be a future.
   * The returned [[MLValue]] `mlVal` will then internally contain that future (i.e.,
   * for example `mlVal.`[[MLValue.retrieveNow retrieveNow]] will wait for `data` to complete first).
   **/
  def apply(data: Future[Data])(implicit isabelle: Isabelle): MLValue[A] =
    MLValue.unsafeFromId(for (data <- data; DObject(id) <- isabelle.applyFunction(this.id, data)) yield id)
}

object MLStoreFunction {
  /** Creates an [[MLStoreFunction]] from ML code `ml`.
   *
   * The ML code `ml` should have type `data -> a` where `a` is the ML type associated with `A` by the
   * implicit [[MLValue.Converter Converter]].
   *
   * This method will not invoke `converter.`[[MLValue.Converter.store store]] or
   * `converter.`[[MLValue.Converter.retrieve retrieve]].
   **/
  def apply[A](ml: String)(implicit isabelle: Isabelle, converter: Converter[A]) : MLStoreFunction[A] =
    new MLStoreFunction(isabelle.storeValue(s"E_Function (DObject o (${converter.valueToExn}) o ($ml))"))
}

/** A compiled ML function specifically transmitting values from Isabelle to Scala.
 *
 * The function is created by `val f = MLRetrieveFunction(ml)` where `ml` is ML code of type
 * `a -> data`, and `a` is the ML type corresponding to `A`.
 *
 * When `f(mlVal : MLValue[A])` is invoked in Scala, the compiled ML function `ml`
 * is applied to the ML value of type `a` referenced by `mlVal` in the object store in the Isabelle process.
 * The result (of ML type `data`) is then transferred back to Scala and returned (in a [[scala.concurrent.Future Future]]).
 *
 * An [[MLRetrieveFunction]] is particularly useful for writing [[MLValue.Converter.retrieve retrieve]] methods
 * when writing an [[MLValue.Converter]].
 *
 * The behavior of an [[MLRetrieveFunction]]`[A]` is very similar to an [[MLFunction]]`[A,Data]`
 * but more efficient. And the [[MLRetrieveFunction]] additionally does not access the
 * [[MLValue.Converter.store store]] and [[MLValue.Converter.retrieve retrieve]] functions of the converter
 * that is passed as an implicit argument. This is important because we use the [[MLRetrieveFunction]] for writing
 * those functions in the first place.
 **/
class MLRetrieveFunction[A] private (id: Future[ID]) {
/*
  private def apply(id: ID)(implicit isabelle: Isabelle): Future[Isabelle.Data] =
    isabelle.applyFunction(this.id, DObject(id))
  private def apply(id: Future[ID])(implicit isabelle: Isabelle): Future[Isabelle.Data] =
    for (id <- id; data <- isabelle.applyFunction(this.id, DObject(id))) yield data
*/

  /** Calls the compiled function on the value of ML type `a` referenced by `value`
   * and returns the result to the Isabelle process (in a [[scala.concurrent.Future Future]]). */
  def apply(value: MLValue[A])(implicit isabelle: Isabelle): Future[Data] =
    for (valueId <- value.id; data <- isabelle.applyFunction(this.id, DObject(valueId))) yield data
}

/** Creates an [[MLRetrieveFunction]] from ML code `ml`.
 *
 * The ML code `ml` should have type `a -> data` where `a` is the ML type associated with `A` by the
 * implicit [[MLValue.Converter Converter]].
 *
 * This method will not invoke `converter.`[[MLValue.Converter.store store]] or
 * `converter.`[[MLValue.Converter.retrieve retrieve]].
 **/
object MLRetrieveFunction {
  def apply[A](ml: String)(implicit isabelle: Isabelle, converter: Converter[A]) : MLRetrieveFunction[A] =
    new MLRetrieveFunction(isabelle.storeValue(s"E_Function (fn DObject x => ($ml) ((${converter.exnToValue}) x))"))
}

object MLValue extends OperationCollection {
  /** Unsafe operation for creating an [[MLValue]].
   * It is the callers responsibility to ensure that `id` refers to an ML value of the right type in the object store.
   * Using this function should rarely be necessary, except possibly when defining new [[Converter]]s.
   */
  def unsafeFromId[A](id: Future[Isabelle.ID]) = new MLValue[A](id)
  /** Same as [[unsafeFromId[A](id:scala* unsafeFromId(Future[ID])]], except
   * that `id` is given directly and not as a [[scala.concurrent.Future Future]]. */
  def unsafeFromId[A](id: Isabelle.ID): MLValue[A] = unsafeFromId[A](Future.successful(id))

  /** Utility method for generating ML code.
   * It returns an ML fragment that can be used as the fallback case when pattern matching exceptions,
   * the fragment raises an error with a description of the exception.
   *
   * Example:
   * Instead of ML code `"fn E_Int i => i"`, we can write `s"fn E_Int i => i | \${matchFailExn("my function")}"`
   * to get more informative error messages on pattern match failures.
   *
   * @param name A short description of the purpose of the match/ML function that is being written.
   *             Will be included in the error message.
   */
  @inline def matchFailExn(name: String) =
    s""" exn => error ("Match failed in ML code generated for $name: " ^ string_of_exn exn)"""

  /** Utlity method for generating ML code. Analogous to [[matchFailExn]], but for cases when we
   * pattern match a value of type `data`. */
  @inline def matchFailData(name: String) =
    s""" data => error ("Match failed in ML code generated for $name: " ^ string_of_data data)"""

  private val logger = log4s.getLogger

  override protected def newOps(implicit isabelle: Isabelle) : Ops = new Ops()

  //noinspection TypeAnnotation
  protected[mlvalue] class Ops(implicit val isabelle: Isabelle) {
    /*isabelle.executeMLCodeNow(
      """exception E_List of exn list
         exception E_Bool of bool
         exception E_Option of exn option
         exception E_Int of int
         exception E_String of string
         exception E_Pair of exn * exn""")*/

    val retrieveData = MLRetrieveFunction[Data]("I")
    val storeData = MLStoreFunction[Data]("I")

    val unitValue = MLValue.compileValueRaw[Unit]("E_Int 0")

    private val retrieveTuple2_ =
      MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing])]("fn (a,b) => DList [DObject a, DObject b]")
    @inline def retrieveTuple2[A,B]: MLRetrieveFunction[(MLValue[A], MLValue[B])] =
      retrieveTuple2_.asInstanceOf[MLRetrieveFunction[(MLValue[A], MLValue[B])]]
    private val storeTuple2_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing])](s"fn DList [DObject a, DObject b] => (a,b) | ${matchFailData("storeTuple2")}")
    @inline def storeTuple2[A,B]: MLStoreFunction[(MLValue[A], MLValue[B])] =
      storeTuple2_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B])]]

    private val retrieveTuple3_ : MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])] =
      MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](s"fn (a,b,c) => DList [DObject a, DObject b, DObject c]")
    def retrieveTuple3[A,B,C] = retrieveTuple3_.asInstanceOf[MLRetrieveFunction[(MLValue[A], MLValue[B], MLValue[C])]]
    private val storeTuple3_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](s"fn DList [DObject a, DObject b, DObject c] => (a,b,c) | ${matchFailData("storeTuple3")}")
    @inline def storeTuple3[A,B,C]: MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C])] =
      storeTuple3_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C])]]

    private val retrieveTuple4_ : MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])] =
      MLRetrieveFunction(s"fn (a,b,c,d) => DList [DObject a, DObject b, DObject c, DObject d]")
    def retrieveTuple4[A,B,C,D] = retrieveTuple4_.asInstanceOf[MLRetrieveFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D])]]
    private val storeTuple4_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](s"fn DList [DObject a, DObject b, DObject c, DObject d] => (a,b,c,d) | ${matchFailData("storeTuple4")}")
    @inline def storeTuple4[A,B,C,D] =
      storeTuple4_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D])]]

    private val retrieveTuple5_ : MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])] =
      MLRetrieveFunction(s"fn (a,b,c,d,e) => DList [DObject a, DObject b, DObject c, DObject d, DObject e]")
    def retrieveTuple5[A,B,C,D,E] = retrieveTuple5_.asInstanceOf[MLRetrieveFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D], MLValue[E])]]
    private val storeTuple5_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](
        s"fn DList [DObject a, DObject b, DObject c, DObject d, DObject e] => (a,b,c,d,e) | ${matchFailData("storeTuple5")}")
    @inline def storeTuple5[A,B,C,D,E] =
      storeTuple5_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D], MLValue[E])]]

    private val retrieveTuple6_ : MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])] =
      MLRetrieveFunction(s"fn (a,b,c,d,e,f) => DList [DObject a, DObject b, DObject c, DObject d, DObject e, DObject f]")
    def retrieveTuple6[A,B,C,D,E,F] = retrieveTuple6_.asInstanceOf[MLRetrieveFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D], MLValue[E], MLValue[F])]]
    private val storeTuple6_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](
        s"fn DList [DObject a, DObject b, DObject c, DObject d, DObject e, DObject f] => (a,b,c,d,e,f) | ${matchFailData("storeTuple6")}")
    @inline def storeTuple6[A,B,C,D,E,F] =
      storeTuple6_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D], MLValue[E], MLValue[F])]]

    private val retrieveTuple7_ : MLRetrieveFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])] =
      MLRetrieveFunction(s"fn (a,b,c,d,e,f,g) => DList [DObject a, DObject b, DObject c, DObject d, DObject e, DObject f, DObject g]")
    def retrieveTuple7[A,B,C,D,E,F,G] = retrieveTuple7_.asInstanceOf[MLRetrieveFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D], MLValue[E], MLValue[F], MLValue[G])]]
    private val storeTuple7_ =
      MLStoreFunction[(MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing], MLValue[Nothing])](
        s"fn DList [DObject a, DObject b, DObject c, DObject d, DObject e, DObject f, DObject g] => (a,b,c,d,e,f,g) | ${matchFailData("storeTuple7")}")
    @inline def storeTuple7[A,B,C,D,E,F,G] =
      storeTuple7_.asInstanceOf[MLStoreFunction[(MLValue[A], MLValue[B], MLValue[C], MLValue[D], MLValue[E], MLValue[F], MLValue[G])]]

    val retrieveInt = MLRetrieveFunction[Int]("DInt")
    val storeInt = MLStoreFunction[Int]("fn DInt i => i")
    val retrieveLong = MLRetrieveFunction[Long]("DInt")
    val storeLong = MLStoreFunction[Long]("fn DInt i => i")
    val retrieveBigInt = MLRetrieveFunction[BigInt]("fn i => DString (string_of_int i)")
    val storeBigInt = MLStoreFunction[BigInt]("fn DString s => Int.fromString s |> Option.valOf")

    val retrieveString: MLRetrieveFunction[String] = MLRetrieveFunction[String]("DString")
    val storeString: MLStoreFunction[String] = MLStoreFunction[String]("fn DString str => str")

    val boolTrue : MLValue[Boolean] = MLValue.compileValue("true")
    val boolFalse : MLValue[Boolean] = MLValue.compileValue("false")
    val retrieveBool : MLRetrieveFunction[Boolean] =
      MLRetrieveFunction("fn true => DInt 1 | false => DInt 0")

    private val optionNone_ = MLValue.compileValueRaw[Option[_]]("E_Option NONE")
    def optionNone[A]: MLValue[Option[A]] = optionNone_.asInstanceOf[MLValue[Option[A]]]
    private val optionSome_ = MLValue.compileFunction[MLValue[Nothing], Option[MLValue[Nothing]]]("SOME")
    def optionSome[A]: MLFunction[A, Option[A]] = optionSome_.asInstanceOf[MLFunction[A, Option[A]]]
    val retrieveOption_ : MLRetrieveFunction[Option[MLValue[Nothing]]] =
      MLRetrieveFunction("fn NONE => DList [] | SOME x => DList [DObject x]")
    def retrieveOption[A] = retrieveOption_.asInstanceOf[MLRetrieveFunction[Option[MLValue[A]]]]

    val retrieveList : MLRetrieveFunction[List[MLValue[Nothing]]] =
      MLRetrieveFunction("DList o map DObject")
    val storeList : MLStoreFunction[List[MLValue[Nothing]]] =
      MLStoreFunction(s"fn DList list => map (fn DObject obj => obj | ${matchFailData("storeList.map")}) list | ${matchFailData("storeList")}")

    val debugInfo_ : MLFunction[MLValue[Nothing], String] =
      compileFunction[MLValue[Nothing], String]("string_of_exn")
    def debugInfo[A]: MLFunction[MLValue[A], String] = debugInfo_.asInstanceOf[MLFunction[MLValue[A], String]]
  }

  /** An instance of this class describes the relationship between a Scala type `A`, the corresponding ML type `a`,
   * and the representation of values of type `a` as exceptions in the object store. To support new types,
   * a corresponding [[Converter]] object/class needs to be declared.
   *
   * We explain how a converter works using the example of [[IntConverter]].
   *
   * The first step is to decide which
   * Scala type and which ML type should be related. In this case, we choose [[scala.Int]] on the Scala side,
   * and `int` on the ML side.
   * We declare the correspondence using the [[mlType]] method:
   * {{{
   *   final object IntConverter extends MLValue.Converter[A] {
   *     override def mlType = "int"
   *     ...
   *   }
   * }}}
   *
   * Next, we have to decide how decide how code is converted from an `int` to an exception (so that it can be stored
   * in the object store). In this simple case, we first declare a new exception for holding integers:
   * {{{
   *   isabelle.executeMLCodeNow("exception E_Int of int")
   * }}}
   * This should be done globally (once per Isabelle instance). Declaring two (even identical) exceptions with the
   * same name `E_Int` must be avoided! See [[control.OperationCollection OperationCollection]]
   * for utilities how to manage this. (`E_Int` specifically does not need to be declared since it is predeclared.)
   *
   * We define the method [[valueToExn]] that returns the ML source code to convert an ML value of type `int` to an exception:
   * {{{
   * final object IntConverter extends MLValue.Converter[A] {
   *     ...
   *     override def valueToExn(implicit isabelle: Isabelle): String = "fn x => E_Int x"  // or equivalently: = "E_Int"
   *     ...
   *   }
   * }}}
   *
   * We also need to convert in the opposite direction:
   * {{{
   *   final object IntConverter extends MLValue.Converter[A] {
   *     ...
   *     override def exnToValue(implicit isabelle: Isabelle): String = "fn (E_Int x) => x"
   *     ...
   *   }
   * }}}
   * (Best add some meaningful exception in case of match failure, e.g., using boilerplate from [[MLValue.matchFailExn]].
   * Omitted for clarity in this example.)
   *
   * Next, we need to write a function for retrieving integer values from the object store (method [[retrieve]]).
   * It gets a `value : MLValue[Int]` as input and has to return a `Future[Int]` containing the stored integer. In principle,
   * there are not restrictions how this is done but the simplest way is the following approach:
   *  - Decide on an encoding of the integer `i` as a tree in the [[control.Isabelle.Data Data]] data structure. (In this case, simply
   *    [[control.Isabelle.DInt DInt]]`(i)` will do the trick.)
   *  - Define an [[MLRetrieveFunction]] `retrieveInt` that performs this encoding on the ML side, i.e.,
   *    we need to write ML code for a function of type `int -> data`. (`data` is the ML analogue of [[control.Isabelle.Data Data]], see the
   *    documentation of [[control.Isabelle.Data Data]].)
   *  - Retrieve the value by invoking `retrieveInt` (gives a `Future[Data]`)
   *  - Convert the resulting [[control.Isabelle.Data Data]] to an [[scala.Int Int]].
   * That is:
   * {{{
   *   final object IntConverter extends MLValue.Converter[A] {
   *     ...
   *     override def retrieve(value: MLValue[Int])
   *                          (implicit isabelle: Isabelle): Future[Int] = {
   *        val retrieveInt = MLRetrieveFunction[Int]("fn i => DInt i")   // compile retrieveInt
   *        for (data <- retrieveInt(value); // invoke retrieveInt to transfer from Isabelle
   *             DInt(long) = data)             // decode the data (simple pattern match)
   *          yield long.toInt   // return the integer
   *     }
   *     ...
   *   }
   * }}}
   * Note that `val retrieveInt = ...` was written inside the function `retrieve` for simplicity here. However,
   * since it invokes the ML compiler, it should be invoked only once (per [[control.Isabelle Isabelle]] instance, like the
   * [[control.Isabelle.executeMLCodeNow executeMLCodeNow]] above). See [[control.OperationCollection OperationCollection]] for an auxiliary class
   * helping to manage this.
   *
   * Finally, we also need a function [[store]] that transfers an integer into the Isabelle object store.
   * Again, the easiest way is to use the following steps:
   *  - Define an encoding of integers as [[control.Isabelle.Data Data]] trees (we use the same encoding as before)
   *  - Define an [[MLStoreFunction]] `storeInt` that decodes the data back to an int on the ML side, i.e.,
   *    we need to write ML code for a function of type `data -> int`.
   *  - Encode the integer to be stored as [[control.Isabelle.Data Data]]
   *  - Invoke storeInt to transfer the [[control.Isabelle.Data Data]] to ML and store the integer in the object store.
   * That is:
   * {{{
   *   final object IntConverter extends MLValue.Converter[A] {
   *     ...
   *     override def store(value: Int)(implicit isabelle: Isabelle): MLValue[Int] = {
   *       val storeInt = MLStoreFunction[Int]("fn DInt i => i")   // compile storeInt
   *       val data = DInt(value)       // encode the integer as Data
   *       Ops.storeInt(data)           // invoke storeInt to get the MLValue
   *     }
   *   }
   * }}}
   * Note that `val retrieveInt = ...` was written inside the function `retrieve` for simplicity here.
   * Like above for `storeInt`, this should be done only once (per [[control.Isabelle Isabelle]] instance).
   *
   * This concludes the definition of the [[Converter]]. Finally, the converter should be made available
   * as an implicit value. That is, we define in a suitable place
   * {{{
   *   implicit val intConverter = IntConverter
   * }}}
   * so that `intConverter` can be imported as an implicit where needed. (And if the converter
   * we constructed is not an object but a class taking other converters as arguments,
   * we instead write something like
   * {{{
   *   implicit def listConverter[A](implicit converter: Converter[A]): ListConverter[A] = new ListConverter()(converter)
   * }}}
   * or similar.)
   *
   * Notes
   *  - Several Scala types can correspond to the same ML type (e.g., [[scala.Int Int]] and
   *    [[scala.Long Long]] both correspond to `int`).
   *  - If the converters for two Scala types `A`,`B` additionally have the same encoding as exceptions (defined via [[valueToExn]],
   *    [[exnToValue]] in their [[Converter]]s), then [[MLValue]][A] and [[MLValue]][B] can be safely typecast into
   *    each other.
   *  - It is not recommended to have the same Scala type `A` for two different ML types `a1` and `a2` (or for the same
   *    ML type but with different encoding). First, this would mean that there have to be two different instances
   *    of [[Converter]]`[A]` available (which means Scala cannot automatically choose the right one). Second, it
   *    means that one has to be manually keep track which [[Converter]] was used for which value (no type safety).
   *  - The attentive reader will notice that we use [[MLRetrieveFunction.apply]] and [[MLStoreFunction.apply]]
   *    when defining the converter, but that these functions take the converter we are currently defining as an
   *    implicit argument! However, this cyclic dependency is not a problem because [[MLRetrieveFunction.apply]]
   *    and [[MLStoreFunction.apply]] never invoke [[store]] and [[retrieve]] from the converter, so we can call
   *    those `apply` functions from [[store]] and [[retrieve]].
   *  - In simple cases (when `A` is simply supposed to be a wrapper around a reference to a value in the Isabelle
   *    process, constructions of converters are simplified by using [[MLValueWrapper]] or [[AdHocConverter]].
   *
   * @tparam A the Scala type for which a corresponding ML type is declared
   */
  abstract class Converter[A] {
    /** Returns the ML type corresponding to `A`.
     *
     * This function should always return the same value, at least for the same `isabelle`. */
    def mlType(implicit isabelle: Isabelle) : String
    /** Given an [[mlvalue.MLValue]] `value`, retrieves and returns the value referenced by `value` in the Isabelle
     * object store.
     *
     * Must not invoke `value.`[[mlvalue.MLValue.retrieve retrieve]] or `value.`[[mlvalue.MLValue.retrieveNow retrieveNow]] because those functions
     * invoke `this.`[[retrieve]]. (But calling [[mlvalue.MLValue.retrieve retrieve]] or [[mlvalue.MLValue.retrieveNow retrieveNow]]
     * on other [[mlvalue.MLValue MLValue]]s is allowed as long as no cyclic dependencies are created.)
     **/
    def retrieve(value: MLValue[A])(implicit isabelle: Isabelle): Future[A]

    /** Given a `value : A`, transfers and stores `value` in the Isabelle object store and returns
     * an [[mlvalue.MLValue]] referencing the value in the object store.
     *
     * Must not invoke [[mlvalue.MLValue.apply MLValue]]`(value)` because that functions
     * invokes `this.`[[store]]. (But calling [[mlvalue.MLValue.apply MLValue]]`(...)`
     * on other values is allowed as long as no cyclic dependencies are created.)
     **/
    def store(value: A)(implicit isabelle: Isabelle): MLValue[A]

    /** Returns ML code for an (anonymous) function of type `exn -> a` that converts a value
     * encoded as an exception back into the original value.
     *
     * It is recommended that this function produces informative match failures in case of invalid inputs.
     * [[de.unruh.isabelle.mlvalue.MLValue.matchFailExn MLValue.matchFailExn]] is a helper function that facilitates this.
     *
     * This function should always return the same value, at least for the same `isabelle`. */
    def exnToValue(implicit isabelle: Isabelle) : String

    /** Returns ML code for an (anonymous) function of type `a -> exn` that converts a value
     * into its encoding as an exception.
     *
     * It is recommended that this function produces informative match failures in case of invalid inputs.
     * [[de.unruh.isabelle.mlvalue.MLValue.matchFailExn MLValue.matchFailExn]] is a helper function that facilitates this.
     *
     * This function should always return the same value, at least for the same `isabelle`. */
    def valueToExn(implicit isabelle: Isabelle) : String
  }

  /** Creates an MLValue containing the value `value`.
   * This transfers `value` to the Isabelle process and stores it in the object store there.
   * @return an [[MLValue]] that references the location in the object store */
  @inline def apply[A](value: A)(implicit conv: Converter[A], isabelle: Isabelle) : MLValue[A] =
    conv.store(value)

  /** Converts a future containing an [[MLValue]] into an [[MLValue]].
   *
   * The resulting [[MLValue]] then holds both the computation of the future, as well as the computation held by
   * the MLValue contained in that future.
   **/
  def removeFuture[A](future: Future[MLValue[A]])(implicit isabelle: Isabelle) : MLValue[A] =
    MLValue.unsafeFromId[A](future.flatMap(_.id))

  /** Compiles `ml` code and inserts it into the object store (without any conversion).
   *
   * `ml` must compile to an ML value of type `exn` that is the encoding of some Scala value of type `A`.
   * (As specified by the [[Converter]] for `A`, even though that converter is not actually used by this function.)
   *
   * The function does not check whether the encoding is correct for the type `A`! (And if that is not the case,
   * the returned [[MLValue]] breaks type-safety.)
   *
   * In most situations, it is preferrable to use higher level compilation functions such as [[compileValue]]
   * or [[compileFunction[D,R]* compileFunction]] or [[MLStoreFunction]] or [[MLRetrieveFunction]] that take care of the encoding as exceptions
   * automatically.
   **/
  def compileValueRaw[A](ml: String)(implicit isabelle: Isabelle): MLValue[A] =
    new MLValue[A](isabelle.storeValue(ml)).logError(s"""Error while compiling value "$ml":""")

  /** Compiles ML code `ml` and inserts it into the object store.
   *
   * `ml` must compile to an ML value of type `a` where `a` is the ML type corresponding to `A` (as specified
   * by the implicit [[Converter]]). Then the result is converted to an exception and stored in the object store.
   * An [[MLValue]] referencing that object is returned.
   *
   * If `ml` is an ML function, [[compileFunction[D,R]* compileFunction]] below might be more convenient.
   **/
  def compileValue[A](ml: String)(implicit isabelle: Isabelle, converter: Converter[A]): MLValue[A] =
    compileValueRaw[A](s"(${converter.valueToExn}) (($ml) : (${converter.mlType}))")

/*  @deprecated("will be removed, use compileFunction or compileValueRaw", "0.1.1-SNAPSHOT")
  def compileFunctionRaw[D, R](ml: String)(implicit isabelle: Isabelle): MLFunction[D, R] =
    MLFunction.unsafeFromId[D,R](isabelle.storeValue(s"E_Function (fn DObject x => ($ml) x |> DObject)")).logError(s"""Error while compiling function "$ml":""")*/

  /** Compiles an ML function and inserts it into the object store.
   *
   * `ml` must compile to an ML value of type `d -> r` where `d,r` are the ML type corresponding to `D,R` (as specified
   * by the implicit [[Converter]]s). Then the result is converted to an exception and stored in the object store.
   * An [[MLFunction]] referencing that object is returned. (An [[MLFunction]] is an [[MLValue]] with some extra methods
   * for evaluating functions.)
   *
   * For functions with more than one argument, see also [[compileFunction[D1,D2,R]* compileFunction[D1,D2,R]]],
   * [[compileFunction[D1,D2,D3,R]* compileFunction[D1,D2,D3,R]]], etc.
   **/
  //noinspection ScalaDeprecation
  def compileFunction[D, R](ml: String)(implicit isabelle: Isabelle, converterD: Converter[D], converterR: Converter[R]): MLFunction[D, R] =
    MLFunction.unsafeFromId[D,R](isabelle.storeValue(
      s"E_Function (DObject o (${converterR.valueToExn}) o (($ml) : ((${converterD.mlType}) -> (${converterR.mlType}))) o (${converterD.exnToValue}) o (fn DObject d => d))"
    )).logError(s"""Error while compiling function "$ml":""")

  /** Like [[compileFunction[D,R]* compileFunction[D,R]]], except that the ML code `ml` must be a function of type `unit -> r`
   * where `r` is the ML type corresponding to `R`. The resulting [[MLFunction2]] `f` can then be invoked
   * also as `f()` and not only as `f(())` (as would be the case if we had used
   * [[compileFunction[D,R]* compileFunction[D,R]]]`[Unit,R](ml)` to compile the function).
   **/
  def compileFunction0[R](ml: String)(implicit isabelle: Isabelle, converter: Converter[R]): MLFunction0[R] =
    compileFunction[Unit, R](ml).function0

  /** Like [[compileFunction[D,R]* compileFunction[D,R]]], except that the ML code `ml` must be a function of type `d1 * d2 -> r`
   * where `d1,d2,r` are the ML types corresponding to `D1,D2,R`. The resulting [[MLFunction2]] `f` can then be invoked
   * also as `f(x1,x2)` and not only as `f((x1,x2))` (as would be the case if we had used
   * [[compileFunction[D,R]* compileFunction[D,R]]]`[(D1,D2),R](ml)` to compile the function).
   **/
  def compileFunction[D1, D2, R](ml: String)
                                (implicit isabelle: Isabelle,
                                 converter1: Converter[D1], converter2: Converter[D2], converterR: Converter[R]): MLFunction2[D1, D2, R] =
    compileFunction[(D1,D2), R](ml).function2

  /** Analogous to [[compileFunction[D1,D2,R]* compileFunction[D1,D2,R]]], except for 3-tuples instead of 2-tuples. */
  def compileFunction[D1, D2, D3, R](ml: String)
                                    (implicit isabelle: Isabelle,
                                     converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3],
                                     converterR: Converter[R]): MLFunction3[D1, D2, D3, R] =
    compileFunction[(D1,D2,D3), R](ml).function3

  /** Analogous to [[compileFunction[D1,D2,R]* compileFunction[D1,D2,R]]], except for 4-tuples instead of 2-tuples. */
  def compileFunction[D1, D2, D3, D4, R](ml: String)
                                    (implicit isabelle: Isabelle,
                                     converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3],
                                     converter4: Converter[D4], converterR: Converter[R]): MLFunction4[D1, D2, D3, D4, R] =
    compileFunction[(D1,D2,D3,D4), R](ml).function4

  /** Analogous to [[compileFunction[D1,D2,R]* compileFunction[D1,D2,R]]], except for 5-tuples instead of 2-tuples. */
  def compileFunction[D1, D2, D3, D4, D5, R](ml: String)
                                        (implicit isabelle: Isabelle,
                                         converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3],
                                         converter4: Converter[D4], converter5: Converter[D5], converterR: Converter[R]): MLFunction5[D1, D2, D3, D4, D5, R] =
    compileFunction[(D1,D2,D3,D4,D5), R](ml).function5

  /** Analogous to [[compileFunction[D1,D2,R]* compileFunction[D1,D2,R]]], except for 6-tuples instead of 2-tuples. */
  def compileFunction[D1, D2, D3, D4, D5, D6, R](ml: String)
                                                (implicit isabelle: Isabelle,
                                                 converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3],
                                                 converter4: Converter[D4], converter5: Converter[D5], converter6: Converter[D6], converterR: Converter[R]): MLFunction6[D1, D2, D3, D4, D5, D6, R] =
    compileFunction[(D1,D2,D3,D4,D5,D6), R](ml).function6

  /** Analogous to [[compileFunction[D1,D2,R]* compileFunction[D1,D2,R]]], except for 7-tuples instead of 2-tuples. */
  def compileFunction[D1, D2, D3, D4, D5, D6, D7, R](ml: String)
                                                    (implicit isabelle: Isabelle,
                                                     converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3],
                                                     converter4: Converter[D4], converter5: Converter[D5], converter6: Converter[D6],
                                                     converter7: Converter[D7], converterR: Converter[R]): MLFunction7[D1, D2, D3, D4, D5, D6, D7, R] =
    compileFunction[(D1,D2,D3,D4,D5,D6,D7), R](ml).function7
}
