theory Scratch
  imports ScalaIsabelleComponent.ScalaIsabelleComponent
Main
begin

ML \<open>open Control_Isabelle\<close>

ML \<open>
val id = addToObjects (E_Term \<^term>\<open>1+2+3\<close>)
\<close>


(* Example *)
ML \<open>
\<^scala>\<open>test\<close> "1+2+3"
\<close>

(* ML \<open>
let
val mutex = Mutex.mutex ()
val _ = Mutex.lock mutex
val result = (Thy_Info.use_thy "Main") handle e => (Mutex.unlock mutex; Exn.reraise e)
(* val result = 123 *)
val _ = Mutex.unlock mutex
in result end;
Thy_Info.get_theory "Main"
\<close> *)


(* Example *)
ML \<open>
val scala = \<open>
import de.unruh.isabelle.control.IsabelleComponent._
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.mlvalue.Implicits._
import scala.concurrent.ExecutionContext.Implicits.global
import de.unruh.isabelle.pure._
import de.unruh.isabelle.pure.Implicits._

val ctxt = Context("Main")
val mlvalue = unsafeMLValueFromNumericID[Term](33)
val term = mlvalue.retrieveNow.pretty(ctxt)
println(term)

/*
val ml = 
"""let
val mutex = Mutex.mutex ()
val _ = Mutex.lock mutex
(* val result = (Thy_Info.use_thy "Main") handle e => (Mutex.unlock mutex; Exn.reraise e)*)
val result = 123
val _ = Mutex.unlock mutex
in result end;
Thy_Info.get_theory "Main"  
"""

val thy = MLValue.compileValue[Theory](ml).retrieveNow.force
println(s"Thy: $thy")
*/

println(MLValue.compileValue[Int]("1+5").retrieveNow)
\<close>

|> Input.string_of
|> Scala_Compiler.toplevel true
\<close>

ML \<open>
fun explore_term ctxt t = let
  val ctxt_id = addToObjects (E_Context ctxt) |> string_of_int
  val term_id = addToObjects (E_Term t) |> string_of_int
in \<^scala>\<open>showTermExplorer\<close> [ctxt_id, term_id]; () end
\<close>


ML \<open>
explore_term \<^context> \<^term>\<open>\<Sum>x\<in>UNIV. 1 / x + 32348471263\<close>
\<close>


end