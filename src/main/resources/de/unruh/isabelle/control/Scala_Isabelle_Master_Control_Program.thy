theory Scala_Isabelle_Master_Control_Program
  imports Pure
begin

ML_file "control_isabelle.ml"

ML \<open>
let
val path = File.platform_path (Path.explode "WORKING_DIRECTORY")
in
  OS.FileSys.chDir path
  handle OS.SysErr (msg,_) => error ("Could set Isabelle working directory as " ^ path ^ ": " ^ msg)
end
\<close>

ML "Control_Isabelle.update_ml_compilation_context (fn _ => Context.Theory \<^theory>)"

ML "Control_Isabelle.handleLines()"

end
