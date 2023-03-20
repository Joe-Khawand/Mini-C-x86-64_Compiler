package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class Liveness {
    static class LiveInfo {
        ERTL instr;
        Label[] succ;   // successeurs
        Set<Label> pred = new HashSet<>();   // prédécesseurs
        Set<Register> defs;   // définitions
        Set<Register> uses;   // utilisations
        Set<Register> ins = new HashSet<>();    // variables vivantes en entrée
        Set<Register> outs = new HashSet<>();   // variables vivantes en sortie

        public LiveInfo(ERTL instr, Label[] succ, Set<Register> defs, Set<Register> uses) {
            this.instr = instr;
            this.succ = succ;
            this.defs = defs;
            this.uses = uses;
        }

        @Override
        public String toString() {
            return instr +
                    " pred=" + pred +
                    " defs=" + defs +
                    " uses=" + uses +
                    " ins=" + ins +
                    " outs=" + outs;
        }
    }

    Map<Label, LiveInfo> info = new HashMap<>();

    Liveness(ERTLgraph g) {
        populateInfo(g);
        setPreds();
        setInsAndOuts();
    }

    /**
     * Remplit la table à partir du graphe de flot de contrôle, avec pour l'instant des ensembles vides pour les champs
     * pred, ins et outs.
     */
    private void populateInfo(ERTLgraph g) {
        g.graph.forEach((label, ertl) -> info.put(label, new LiveInfo(ertl, ertl.succ(), ertl.def(), ertl.use())));

        for (ERTL ertl : g.graph.values())
            for (Register r : ertl.use())
                r.next++;
    }

    /**
     * Parcourt la table pour remplir les champs pred (les prédécesseurs),
     * à partir de l'information contenue dans les champs succ (les successeurs).
     */
    private void setPreds() {
        info.forEach((label, liveInfo) -> {
            for (Label s : liveInfo.succ)
                info.get(s).pred.add(label);
        });
    }

    /**
     * Algorithme de Kildall pour calculer les champs ins et outs.
     */
    private void setInsAndOuts() {
        HashSet<Label> ws = new HashSet<>(info.keySet());
        Queue<Label> wq = new LinkedList<>(ws);

        while (!ws.isEmpty()) {
            Label label = wq.remove();
            ws.remove(label);

            LiveInfo li = info.get(label);
            li.outs = new HashSet<>();
            for (Label s : li.succ)
                li.outs.addAll(info.get(s).ins);

            Set<Register> outMinusDef = new HashSet<>(li.outs);
            outMinusDef.removeAll(li.defs);

            Set<Register> oldIns = li.ins;
            li.ins = new HashSet<>(li.uses);
            li.ins.addAll(outMinusDef);

            if (!oldIns.equals(li.ins))
                for (Label p : li.pred)
                    if (ws.add(p))
                        wq.add(p);
        }
    }

    private void print(Set<Label> visited, Label l) {
        if (visited.contains(l)) return;
        visited.add(l);
        LiveInfo li = this.info.get(l);
        System.out.println("  " + String.format("%3s", l) + ": " + li);
        for (Label s: li.succ) print(visited, s);
      }
    
    void print(Label entry) {
        print(new HashSet<Label>(), entry);
    }
}

