theory Demo
  imports Main
begin

ML \<open>
@{scala reverse} "!gninrom dooG"
\<close>

(* ML \<open>
@{scala log} "Hello log window!"
\<close> *)

ML \<open>
fun initialize_scala_isabelle () = let
  val code = @{scala initializeScalaIsabelle} ""
  val _ = tracing code
  val source = Input.string code
  val _ = Context.>> (ML_Context.exec (fn () => 
     (ML_Context.eval ML_Compiler.flags Position.none (ML_Lex.read_source_sml source))))
in () end
\<close>

ML \<open>
initialize_scala_isabelle ()
\<close>

ML \<open>
open Control_Isabelle
\<close>



ML \<open>
@{scala accessScalaIsabelle} "1+2" 
\<close>


end