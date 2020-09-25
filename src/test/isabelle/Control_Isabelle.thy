(*
This theory is for opening \<open>control_isabelle.ml\<close> for editing. Load it in Isabelle and 
control-click on "\<dots>/control_isabelle.ml" below
*)

theory Control_Isabelle
  imports HOL.Set Main
begin

ML \<open>
val inputPipeName = "/dev/null"
val outputPipeName = "/dev/null"
\<close>

ML_file "../../main/resources/de/unruh/isabelle/control/control_isabelle.ml"

end
