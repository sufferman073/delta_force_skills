package com.rzy.dealt_force_skills.department;

/** Server-side provenance travels with the same instance and hidden chain as the effect. */
public interface DepartmentEffectSource {
    DepartmentManba.Silence dfs$silence();
    void dfs$silence(DepartmentManba.Silence silence);
    DepartmentConsumables.Source dfs$consumableSource();
    void dfs$consumableSource(DepartmentConsumables.Source source);
}
