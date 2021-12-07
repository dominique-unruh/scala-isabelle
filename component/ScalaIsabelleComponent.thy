theory ScalaIsabelleComponent
  imports Main
begin

(* Start the de.unruh.isabelle.control.Isabelle instance (accessible in Scala as de.unruh.isabelle.control.ComponentFunctions.isabelle),
   and load the ML-file containing the structure Control_Isabelle *)
ML \<open>ML_Context.eval_file ML_Compiler.flags (Path.explode (\<^scala>\<open>initializeScalaIsabelle\<close> "I know what I am doing!"))\<close>

ML \<open>
local
val control_isabelle_struct =
  #allStruct ML_Env.name_space () |> find_first (fn (n,_) => n = "Control_Isabelle") |> Option.valOf |> snd
val params : Isabelle_Thread.params = {name="scala-isabelle protocol", stack_limit=NONE, interrupts=false}
in
val _ = Isabelle_Thread.fork params (fn () =>
  (#enterStruct ML_Env.name_space ("Control_Isabelle", control_isabelle_struct);
   Control_Isabelle.handleLines ()))    
end
\<close>

(* Example *)
ML \<open>
\<^scala>\<open>test\<close> "1+2"
\<close>

(* Example *)
ML \<open>
val scala = \<open>
import de.unruh.isabelle.control.IsabelleComponent.isabelle
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.mlvalue.Implicits._
import scala.concurrent.ExecutionContext.Implicits.global

println(MLValue.compileValue[Int]("1+2").retrieveNow)
\<close>
\<close>

(* Example *)
ML \<open>
scala
|> Input.string_of
|> Scala_Compiler.toplevel true
\<close>

end
