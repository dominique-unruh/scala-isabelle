theory Scala_Isabelle_Master_Control_Program
  imports Pure
begin

(* run_one_of_these_ml will try to compile and execute all the given ML fragments
    until one of them successfully compiles and executes. *)
ML \<open>
fun run_one_of_these_ml mls = let
  val errors = Unsynchronized.ref []
  fun run_single ml = 
    Context.>> (Local_Theory.touch_ml_env #> ML_Context.exec (fn () =>
                ML_Context.eval_source (ML_Compiler.verbose true ML_Compiler.flags) ml) #>
                Local_Theory.propagate_ml_env)
  fun run [] = raise ERROR ("Could not run any of the ML codes. Errors were:\n" ^
                            String.concatWith "\n" (!errors))
    | run (ml::mls) = run_single ml
                      handle ERROR e => (errors := e :: !errors; run mls)
in
  run mls
end
\<close>

(* Define compatibility aliases for Mutex.mutex and friends.
   For use in control_isabelle.ml only *)
ML \<open>
run_one_of_these_ml [
  \<open>type mcp_mutex = Mutex.mutex val mcp_mutex = Mutex.mutex val mcp_mutex_lock = Mutex.lock val mcp_mutex_unlock = Mutex.unlock\<close>,
  \<open>type mcp_mutex = Thread.Mutex.mutex val mcp_mutex = Thread.Mutex.mutex val mcp_mutex_lock = Thread.Mutex.lock val mcp_mutex_unlock = Thread.Mutex.unlock\<close>]
\<close>

(* Do the equivalent of `declare [[ML_catch_all = true]]`, but without error if it fails *)
ML \<open>
run_one_of_these_ml [\<open>Context.>> (Context.map_theory (Config.put_global ML_Compiler.ML_catch_all true))\<close>,
                     \<open>\<close>]
\<close>


ML_file "control_isabelle.ml"

ML \<open>
let
val path = File.platform_path (Path.explode "WORKING_DIRECTORY")
in
  OS.FileSys.chDir path
  handle OS.SysErr (msg,_) => error ("Could not set Isabelle working directory as " ^ path ^ ": " ^ msg)
end
\<close>

ML "Control_Isabelle.update_ml_compilation_context (fn _ => Context.Theory \<^theory>)"

ML "Control_Isabelle.handleLines()"

end
