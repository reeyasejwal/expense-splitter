# Expense Splitter — Debt Minimization Engine

A JavaFX desktop application that splits group expenses and minimizes 
the number of transactions needed to settle all debts.

## Core Algorithm
Greedy debt minimization using two max-heaps.  
Time complexity: O(N log N) | Space: O(N)  
Each iteration settles at least one person completely.

## Features
- Custom split between selected members (not just equal split)
- Confidence-based ML anomaly detection using Z-score statistics
- Bar chart: net balances per person
- Pie chart: who paid how much
- Full expense history with table view
- ACID-compliant SQLite storage (Java transactions)

## Tech Stack
Java 21 · JavaFX 21 · SQLite · Maven

## Design Patterns Used
- Factory Pattern: SplitStrategy (equal vs custom)
- Single Responsibility: Person, Txn, DB, Minimizer, Anomaly — each one job
- SOLID principles throughout

## How to Run
```bash
mvn clean javafx:run
```
Requires Java 21 and Maven installed.

## Algorithm Explained
1. Compute net balance per person from all expenses
2. Separate into creditors (+ve) and debtors (-ve)
3. Use two max-heaps — always match largest creditor with largest debtor
4. Each step settles at least one person completely
5. Repeat until all balances = 0
