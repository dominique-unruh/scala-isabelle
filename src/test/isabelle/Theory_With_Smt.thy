theory Theory_With_Smt
  imports Main
begin

lemma True
  using [[smt_timeout=120]]
  by smt

end
