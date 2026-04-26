package com.es.algo;

import com.es.model.Txn;

import java.util.*;

public class Minimizer {

    /**
     * Greedy debt minimization using two max-heaps.
     * Time:  O(N log N)
     * Space: O(N)
     *
     * Each step settles at least one person completely.
     * Produces the minimum number of transactions needed.
     */
    public static List<Txn> minimize(Map<String, Double> balances) {
        List<Txn> result = new ArrayList<>();

        PriorityQueue<Object[]> credQ = new PriorityQueue<>(
            (a, b) -> Double.compare((double) b[1], (double) a[1])
        );
        PriorityQueue<Object[]> debtQ = new PriorityQueue<>(
            (a, b) -> Double.compare((double) b[1], (double) a[1])
        );

        for (Map.Entry<String, Double> e : balances.entrySet()) {
            double v = e.getValue();
            if      (v >  0.01) credQ.offer(new Object[]{e.getKey(),  v});
            else if (v < -0.01) debtQ.offer(new Object[]{e.getKey(), -v});
        }

        while (!credQ.isEmpty() && !debtQ.isEmpty()) {
            Object[] cred = credQ.poll();
            Object[] debt = debtQ.poll();

            String credName = (String) cred[0]; double credBal = (double) cred[1];
            String debtName = (String) debt[0]; double debtBal = (double) debt[1];

            double settled = Math.min(credBal, debtBal);
            double rounded = Math.round(settled * 100) / 100.0;

            result.add(new Txn(debtName, credName, rounded));

            if (credBal - settled > 0.01) credQ.offer(new Object[]{credName, credBal - settled});
            if (debtBal - settled > 0.01) debtQ.offer(new Object[]{debtName, debtBal - settled});
        }

        return result;
    }
}
