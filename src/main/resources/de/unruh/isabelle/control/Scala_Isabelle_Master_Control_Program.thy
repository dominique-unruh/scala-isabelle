theory Scala_Isabelle_Master_Control_Program
  imports Pure
begin

ML_file "control_isabelle.ml"

ML \<open>OS.FileSys.chDir "WORKING_DIRECTORY"\<close>

ML "Control_Isabelle.update_ml_compilation_context (fn _ => Context.Theory \<^theory>)"

ML "Control_Isabelle.handleLines()"

end
