package com.es.db;

import com.es.model.Person;

import java.sql.*;
import java.util.*;

public class DB {

    private static final String URL = "jdbc:sqlite:expenses.db";

    private static Connection getConn() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void init() {
        try (Connection c = getConn(); Statement s = c.createStatement()) {
            s.execute(
                "CREATE TABLE IF NOT EXISTS groups (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)"
            );
            s.execute(
                "CREATE TABLE IF NOT EXISTS members (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "group_id INTEGER NOT NULL, name TEXT NOT NULL)"
            );
            s.execute(
                "CREATE TABLE IF NOT EXISTS expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "group_id INTEGER NOT NULL, description TEXT NOT NULL, " +
                "amount_paise INTEGER NOT NULL, paid_by INTEGER NOT NULL, " +
                "ts DATETIME DEFAULT CURRENT_TIMESTAMP)"
            );
            s.execute(
                "CREATE TABLE IF NOT EXISTS splits (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "expense_id INTEGER NOT NULL, member_id INTEGER NOT NULL, " +
                "share_paise INTEGER NOT NULL)"
            );
            System.out.println("[DB] Ready.");
        } catch (SQLException e) {
            System.err.println("[DB] INIT FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ FIXED
    public static int createGroup(String name) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO groups (name) VALUES (?)")) {

            ps.setString(1, name);
            ps.executeUpdate();

            ResultSet rs = c.createStatement()
                    .executeQuery("SELECT last_insert_rowid()");
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("[DB] Created group '" + name + "' id=" + id);
                return id;
            }

        } catch (SQLException e) {
            System.err.println("[DB] createGroup FAILED: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    // ✅ FIXED
    public static int addMember(int groupId, String name) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO members (group_id, name) VALUES (?, ?)")) {

            ps.setInt(1, groupId);
            ps.setString(2, name);
            ps.executeUpdate();

            ResultSet rs = c.createStatement()
                    .executeQuery("SELECT last_insert_rowid()");
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("[DB] Added member '" + name + "' id=" + id);
                return id;
            }

        } catch (SQLException e) {
            System.err.println("[DB] addMember FAILED: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public static List<Person> getMembers(int groupId) {
        List<Person> list = new ArrayList<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id, name FROM members WHERE group_id = ? ORDER BY id")) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new Person(rs.getInt("id"), rs.getString("name")));
        } catch (SQLException e) {
            System.err.println("[DB] getMembers FAILED: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // ✅ FIXED
    public static boolean addExpense(int groupId, String desc,
                                     double amountRs, int paidById,
                                     Map<Integer, Double> splits) {
        Connection c = null;
        try {
            c = getConn();
            c.setAutoCommit(false);

            int expenseId;

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO expenses (group_id, description, amount_paise, paid_by) VALUES (?,?,?,?)")) {

                ps.setInt(1, groupId);
                ps.setString(2, desc);
                ps.setLong(3, Math.round(amountRs * 100));
                ps.setInt(4, paidById);
                ps.executeUpdate();

                ResultSet rs = c.createStatement()
                        .executeQuery("SELECT last_insert_rowid()");
                if (!rs.next()) throw new SQLException("No expense ID returned");
                expenseId = rs.getInt(1);
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO splits (expense_id, member_id, share_paise) VALUES (?,?,?)")) {
                for (Map.Entry<Integer, Double> entry : splits.entrySet()) {
                    ps.setInt(1, expenseId);
                    ps.setInt(2, entry.getKey());
                    ps.setLong(3, Math.round(entry.getValue() * 100));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            c.commit();
            System.out.println("[DB] Saved expense '" + desc + "' Rs." + amountRs);
            return true;

        } catch (Exception e) {
            System.err.println("[DB] addExpense FAILED: " + e.getMessage());
            e.printStackTrace();
            try { if (c != null) c.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            try { if (c != null) { c.setAutoCommit(true); c.close(); } }
            catch (SQLException ignored) {}
        }
    }

    public static Map<String, Double> getBalances(int groupId) {
        Map<Integer, String> idToName = new LinkedHashMap<>();
        Map<Integer, Double> bal = new LinkedHashMap<>();

        for (Person p : getMembers(groupId)) {
            idToName.put(p.getId(), p.getName());
            bal.put(p.getId(), 0.0);
        }

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT e.paid_by, s.member_id, e.amount_paise, s.share_paise " +
                 "FROM expenses e JOIN splits s ON e.id = s.expense_id " +
                 "WHERE e.group_id = ?")) {

            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int paidBy = rs.getInt(1);
                int membId = rs.getInt(2);
                long amtP = rs.getLong(3);
                long shareP = rs.getLong(4);

                if (paidBy == membId)
                    bal.merge(paidBy, (amtP - shareP) / 100.0, Double::sum);
                else
                    bal.merge(membId, -shareP / 100.0, Double::sum);
            }

        } catch (SQLException e) {
            System.err.println("[DB] getBalances FAILED: " + e.getMessage());
        }

        Map<String, Double> result = new LinkedHashMap<>();
        bal.forEach((id, v) -> result.put(idToName.get(id), v));
        return result;
    }

    public static Map<String, Double> getPaidByPerson(int groupId) {
        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT m.name, SUM(e.amount_paise)/100.0 FROM expenses e " +
                 "JOIN members m ON e.paid_by=m.id WHERE e.group_id=? GROUP BY e.paid_by")) {

            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                result.put(rs.getString(1), rs.getDouble(2));

        } catch (SQLException e) {
            System.err.println("[DB] getPaidByPerson FAILED: " + e.getMessage());
        }
        return result;
    }

    public static List<String[]> getHistory(int groupId) {
        List<String[]> rows = new ArrayList<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT m.name, e.description, e.amount_paise/100.0, e.ts " +
                 "FROM expenses e JOIN members m ON e.paid_by=m.id " +
                 "WHERE e.group_id=? ORDER BY e.ts DESC")) {

            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String date = rs.getString(4);
                if (date != null && date.length() >= 10)
                    date = date.substring(0, 10);

                rows.add(new String[]{
                        rs.getString(1),
                        rs.getString(2),
                        String.format("%.2f", rs.getDouble(3)),
                        date
                });
            }

        } catch (SQLException e) {
            System.err.println("[DB] getHistory FAILED: " + e.getMessage());
        }
        return rows;
    }
    public static List<Double> getAllAmounts(int groupId) {
    List<Double> list = new ArrayList<>();
    try (Connection c = getConn();
         PreparedStatement ps = c.prepareStatement(
             "SELECT amount_paise/100.0 FROM expenses WHERE group_id=?")) {

        ps.setInt(1, groupId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            list.add(rs.getDouble(1));
        }

    } catch (SQLException e) {
        System.err.println("[DB] getAllAmounts FAILED: " + e.getMessage());
    }
    return list;
}
}