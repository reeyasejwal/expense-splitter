package com.es.ui;

import com.es.algo.Anomaly;
import com.es.algo.Minimizer;
import com.es.db.DB;
import com.es.model.Person;
import com.es.model.Txn;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

public class App {

    private final Stage stage;
    private int          groupId   = -1;
    private String       groupName = "";
    private List<Person> members   = new ArrayList<>();
    private BorderPane   root;

    private static final String PURPLE    = "#6C63FF";
    private static final String DARK_BG   = "#1e1e3f";
    private static final String PAGE_BG   = "#f4f4f8";
    private static final String GREEN     = "#1D9E75";
    private static final String RED       = "#E24B4A";
    private static final String AMBER     = "#BA7517";
    private static final String BLUE      = "#378ADD";

    public App(Stage stage) {
        this.stage = stage;
        showSetup();
    }

    // ─────────────────────────────────────────
    //  SETUP SCREEN
    // ─────────────────────────────────────────
    private void showSetup() {
        groupId   = -1;
        groupName = "";
        members   = new ArrayList<>();

        List<String>   pendingNames = new ArrayList<>();
        VBox           memberRows   = new VBox(6);
        Label          statusLbl    = label("", 12, RED);

        TextField groupField  = styledField("e.g. Goa Trip 2025");
        TextField memberField = styledField("Type full name then click Add");
        HBox.setHgrow(memberField, Priority.ALWAYS);

        Button addBtn    = accentButton("+ Add Member");
        Button createBtn = accentButton("Create Group  →");
        createBtn.setMaxWidth(Double.MAX_VALUE);

        Runnable doAdd = () -> {
            String name = memberField.getText().trim();
            if (name.isEmpty())                 { statusLbl.setText("Name cannot be empty.");      return; }
            if (pendingNames.contains(name))    { statusLbl.setText("'" + name + "' already added."); return; }

            pendingNames.add(name);
            statusLbl.setText("");

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:white;-fx-padding:8 12;" +
                "-fx-background-radius:6;-fx-border-color:#e0e0e0;-fx-border-radius:6;");

            Label dot     = label("●", 10, PURPLE);
            Label nameLbl = label(name, 13, "#222");
            Region sp     = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

            Button rem = new Button("×");
            rem.setStyle("-fx-background-color:transparent;-fx-text-fill:" + RED +
                ";-fx-font-size:15px;-fx-cursor:hand;-fx-border-color:transparent;-fx-padding:0 4;");
            final String captured = name;
            rem.setOnAction(ev -> { pendingNames.remove(captured); memberRows.getChildren().remove(row); });

            row.getChildren().addAll(dot, nameLbl, sp, rem);
            memberRows.getChildren().add(row);
            memberField.clear();
            memberField.requestFocus();
        };

        addBtn.setOnAction(e -> doAdd.run());
        memberField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) doAdd.run(); });

        createBtn.setOnAction(e -> {
            String gName = groupField.getText().trim();
            if (gName.isEmpty())             { statusLbl.setText("Enter a group name."); return; }
            if (pendingNames.size() < 2)     { statusLbl.setText("Add at least 2 members."); return; }

            int gId = DB.createGroup(gName);
            if (gId == -1) { statusLbl.setText("Database error. Check terminal for details."); return; }

            groupId   = gId;
            groupName = gName;

            for (String nm : pendingNames) DB.addMember(groupId, nm);
            members = DB.getMembers(groupId);

            if (members.isEmpty()) {
                statusLbl.setText("Members failed to save. Check terminal.");
                return;
            }
            showDashboard();
        });

        // Layout
        Label title = label("Expense Splitter", 26, PURPLE);
        title.setStyle(title.getStyle() + "-fx-font-weight:500;");
        Label sub = label("Debt minimizer · Smart split · ML anomaly detection", 12, "#999");

        VBox card = new VBox(14);
        card.setStyle("-fx-background-color:white;-fx-border-color:#e0e0e0;" +
            "-fx-border-radius:14;-fx-background-radius:14;-fx-padding:36;");
        card.setMaxWidth(460);

        HBox memberInputRow = new HBox(8, memberField, addBtn);
        memberInputRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(
            title, sub, new Separator(),
            label("Group Name", 12, "#555"), groupField,
            label("Members  (min. 2)", 12, "#555"), memberInputRow,
            memberRows, statusLbl, createBtn
        );

        StackPane outer = new StackPane(card);
        outer.setStyle("-fx-background-color:" + PAGE_BG + ";");
        outer.setPadding(new Insets(60));

        stage.setScene(new Scene(outer, 1050, 700));
        stage.setTitle("Expense Splitter");
        stage.show();
    }

    // ─────────────────────────────────────────
    //  DASHBOARD
    // ─────────────────────────────────────────
    private void showDashboard() {
        root = new BorderPane();
        root.setLeft(buildSidebar());
        root.setCenter(buildOverview());
        stage.setScene(new Scene(root, 1050, 700));
    }

    private VBox buildSidebar() {
        VBox sb = new VBox(4);
        sb.setStyle("-fx-background-color:" + DARK_BG + ";-fx-padding:28 14;");
        sb.setPrefWidth(210);

        Label appLabel = label("Expense\nSplitter", 20, "white");
        appLabel.setStyle(appLabel.getStyle() + "-fx-font-weight:500;-fx-padding:0 0 4 8;");
        Label grpLabel = label(groupName + "  ·  " + members.size() + " members", 11, "#8888bb");
        grpLabel.setStyle(grpLabel.getStyle() + "-fx-padding:0 0 16 8;");
        grpLabel.setWrapText(true);

        Button[] nav = {
            navButton("  Overview"),
            navButton("  Add Expense"),
            navButton("  Settle Up"),
            navButton("  History")
        };

        nav[0].setOnAction(e -> { setActive(nav, 0); root.setCenter(buildOverview()); });
        nav[1].setOnAction(e -> { setActive(nav, 1); root.setCenter(buildAddExpense(nav)); });
        nav[2].setOnAction(e -> { setActive(nav, 2); root.setCenter(buildSettleUp(nav)); });
        nav[3].setOnAction(e -> { setActive(nav, 3); root.setCenter(buildHistory(nav)); });
        setActive(nav, 0);

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);

        Button newGroup = new Button("  ← New Group");
        newGroup.setStyle("-fx-background-color:transparent;-fx-text-fill:#8888bb;" +
            "-fx-font-size:12px;-fx-cursor:hand;-fx-border-color:transparent;-fx-padding:8 12;");
        newGroup.setMaxWidth(Double.MAX_VALUE);
        newGroup.setOnAction(e -> showSetup());

        sb.getChildren().addAll(appLabel, grpLabel, new Separator());
        sb.getChildren().addAll(nav);
        sb.getChildren().addAll(spacer, newGroup);
        return sb;
    }

    // ─────────────────────────────────────────
    //  PANEL 1 — OVERVIEW
    // ─────────────────────────────────────────
    private Node buildOverview() {
        List<Double> amounts  = DB.getAllAmounts(groupId);
        double total = amounts.stream().mapToDouble(d -> d).sum();
        double avg   = amounts.isEmpty() ? 0 : total / amounts.size();
        int    count = amounts.size();

        HBox stats = new HBox(12,
            statCard("Total Spent",  String.format("Rs.%.2f", total), PURPLE),
            statCard("Members",      String.valueOf(members.size()),   GREEN),
            statCard("Expenses",     String.valueOf(count),            AMBER),
            statCard("Avg Expense",  String.format("Rs.%.2f", avg),   BLUE)
        );

        Map<String, Double> bals = DB.getBalances(groupId);
        VBox balList = new VBox(8);
        if (bals.isEmpty()) {
            balList.getChildren().add(infoBox("No expenses yet. Go to 'Add Expense' to get started."));
        } else {
            for (Map.Entry<String, Double> e : bals.entrySet()) {
                double v = e.getValue();
                String color = v >  0.01 ? GREEN : v < -0.01 ? RED : "#888";
                String txt   = v >  0.01 ? "gets back Rs." + String.format("%.2f", v)
                             : v < -0.01 ? "owes Rs."     + String.format("%.2f", -v)
                             : "settled up";
                HBox row = new HBox();
                row.setStyle("-fx-background-color:white;-fx-padding:12 16;" +
                    "-fx-background-radius:8;-fx-border-color:#eee;-fx-border-radius:8;");
                row.setAlignment(Pos.CENTER_LEFT);
                Label nameLbl = label(e.getKey(), 13, "#222");
                Label balLbl  = label(txt, 13, color);
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                row.getChildren().addAll(nameLbl, sp, balLbl);
                balList.getChildren().add(row);
            }
        }

        VBox page = new VBox(20,
            pageTitle("Overview"),
            stats,
            sectionTitle("Current Balances"),
            balList
        );
        return scrollPage(page);
    }

    // ─────────────────────────────────────────
    //  PANEL 2 — ADD EXPENSE
    // ─────────────────────────────────────────
    private Node buildAddExpense(Button[] nav) {
        TextField descField = styledField("e.g. Dinner at hotel");
        TextField amtField  = styledField("Amount in Rs.");
        ComboBox<Person> paidBox = new ComboBox<>(FXCollections.observableArrayList(members));
        paidBox.setPromptText("Who paid?");
        paidBox.setMaxWidth(Double.MAX_VALUE);
        paidBox.setStyle("-fx-font-size:13px;");

        // Checkboxes — who to split between
        List<CheckBox>        checks       = new ArrayList<>();
        Map<Integer, TextField> shareFields = new LinkedHashMap<>();
        VBox checkList  = new VBox(8);
        VBox customBox  = new VBox(8); customBox.setVisible(false); customBox.setManaged(false);

        for (Person p : members) {
            CheckBox cb = new CheckBox(p.getName());
            cb.setSelected(true);
            cb.setStyle("-fx-font-size:13px;");
            checks.add(cb);
            checkList.getChildren().add(cb);

            TextField tf = styledField(p.getName() + "'s share");
            shareFields.put(p.getId(), tf);
            HBox row = new HBox(10, label(p.getName() + ":", 13, "#555"), tf);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(tf, Priority.ALWAYS);
            customBox.getChildren().add(row);
        }

        Button selAll = ghostButton("Select all");
        selAll.setOnAction(e -> checks.forEach(c -> c.setSelected(true)));

        ToggleGroup tg  = new ToggleGroup();
        RadioButton eqR = new RadioButton("Split equally");
        RadioButton cuR = new RadioButton("Custom amounts");
        eqR.setToggleGroup(tg); eqR.setSelected(true);
        cuR.setToggleGroup(tg);
        eqR.setStyle("-fx-font-size:13px;"); cuR.setStyle("-fx-font-size:13px;");

        eqR.setOnAction(e -> { customBox.setVisible(false); customBox.setManaged(false); fillEqual(amtField, checks, shareFields); });
        cuR.setOnAction(e -> { customBox.setVisible(true);  customBox.setManaged(true);  fillEqual(amtField, checks, shareFields); });

        amtField.textProperty().addListener((o, ov, nv) -> {
            if (eqR.isSelected()) fillEqual(amtField, checks, shareFields);
        });
        checks.forEach(cb -> cb.selectedProperty().addListener((o, ov, nv) -> {
            if (eqR.isSelected()) fillEqual(amtField, checks, shareFields);
        }));

        Label anomalyLbl = label("", 12, AMBER);
        anomalyLbl.setWrapText(true);
        anomalyLbl.setStyle(anomalyLbl.getStyle() +
            "-fx-background-color:#FFF8E1;-fx-padding:8 12;-fx-background-radius:6;");
        anomalyLbl.setVisible(false); anomalyLbl.setManaged(false);

        Label resultLbl = label("", 13, GREEN);

        Button addBtn  = accentButton("Add Expense");
        Button backBtn = ghostButton("← Back to Overview");
        backBtn.setVisible(false); backBtn.setManaged(false);
        backBtn.setOnAction(e -> { setActive(nav, 0); root.setCenter(buildOverview()); });

        addBtn.setOnAction(e -> {
            resultLbl.setText("");
            anomalyLbl.setVisible(false); anomalyLbl.setManaged(false);

            String desc   = descField.getText().trim();
            String amtTxt = amtField.getText().trim();
            Person paidBy = paidBox.getValue();

            if (desc.isEmpty())   { resultLbl.setStyle(resultLbl.getStyle().replace(GREEN, RED)); resultLbl.setText("Enter a description."); return; }
            if (amtTxt.isEmpty()) { resultLbl.setStyle(resultLbl.getStyle().replace(GREEN, RED)); resultLbl.setText("Enter an amount.");     return; }
            if (paidBy == null)   { resultLbl.setStyle(resultLbl.getStyle().replace(GREEN, RED)); resultLbl.setText("Select who paid.");      return; }

            double amt;
            try {
                amt = Double.parseDouble(amtTxt);
                if (amt <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                resultLbl.setStyle("-fx-text-fill:" + RED + ";-fx-font-size:13px;");
                resultLbl.setText("Enter a valid positive number, e.g. 500");
                return;
            }

            List<Person> selected = new ArrayList<>();
            for (int i = 0; i < checks.size(); i++)
                if (checks.get(i).isSelected()) selected.add(members.get(i));

            if (selected.isEmpty()) {
                resultLbl.setStyle("-fx-text-fill:" + RED + ";-fx-font-size:13px;");
                resultLbl.setText("Select at least 1 person to split between.");
                return;
            }

            // ML anomaly check
            String warn = Anomaly.check(amt, DB.getAllAmounts(groupId));
            if (warn != null) {
                anomalyLbl.setText("ML Alert: " + warn);
                anomalyLbl.setVisible(true); anomalyLbl.setManaged(true);
            }

            // Build splits
            Map<Integer, Double> splits = new LinkedHashMap<>();
            if (eqR.isSelected()) {
                double share = amt / selected.size();
                selected.forEach(p -> splits.put(p.getId(), share));
            } else {
                double total = 0;
                for (Person p : selected) {
                    String val = shareFields.get(p.getId()).getText().trim();
                    if (val.isEmpty()) {
                        resultLbl.setStyle("-fx-text-fill:" + RED + ";-fx-font-size:13px;");
                        resultLbl.setText("Enter share for " + p.getName());
                        return;
                    }
                    try {
                        double s = Double.parseDouble(val);
                        splits.put(p.getId(), s);
                        total += s;
                    } catch (NumberFormatException ex) {
                        resultLbl.setStyle("-fx-text-fill:" + RED + ";-fx-font-size:13px;");
                        resultLbl.setText("Invalid share for " + p.getName());
                        return;
                    }
                }
                if (Math.abs(total - amt) > 0.5) {
                    resultLbl.setStyle("-fx-text-fill:" + RED + ";-fx-font-size:13px;");
                    resultLbl.setText(String.format(
                        "Shares sum to Rs.%.2f but expense is Rs.%.2f", total, amt));
                    return;
                }
            }

            boolean ok = DB.addExpense(groupId, desc, amt, paidBy.getId(), splits);
            if (!ok) {
                resultLbl.setStyle("-fx-text-fill:" + RED + ";-fx-font-size:13px;");
                resultLbl.setText("Database error — expense not saved. Check terminal.");
                return;
            }

            resultLbl.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:13px;");
            resultLbl.setText("Expense added! Go to Overview or Settle Up to see it.");
            backBtn.setVisible(true); backBtn.setManaged(true);

            descField.clear();
            amtField.clear();
            paidBox.setValue(null);
            checks.forEach(c -> c.setSelected(true));
        });

        VBox form = new VBox(14);
        form.setStyle("-fx-background-color:white;-fx-padding:28;" +
            "-fx-background-radius:12;-fx-border-color:#e0e0e0;-fx-border-radius:12;");
        form.setMaxWidth(580);
        form.getChildren().addAll(
            label("Description", 12, "#555"), descField,
            label("Amount (Rs.)", 12, "#555"), amtField,
            label("Paid by", 12, "#555"), paidBox,
            label("Split between", 12, "#555"), selAll, checkList,
            label("Split type", 12, "#555"),
            new HBox(20, eqR, cuR),
            customBox,
            anomalyLbl,
            addBtn, resultLbl, backBtn
        );

        VBox page = new VBox(20, pageTitle("Add Expense"), form);
        return scrollPage(page);
    }

    // ─────────────────────────────────────────
    //  PANEL 3 — SETTLE UP
    // ─────────────────────────────────────────
    private Node buildSettleUp(Button[] nav) {
        Map<String, Double> balances = DB.getBalances(groupId);
        List<Txn>           txns     = Minimizer.minimize(balances);

        // Bar chart — net balances
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        yAxis.setLabel("Rs.");
        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setTitle("Net Balances per Person");
        bar.setPrefHeight(270);
        bar.setAnimated(false);
        bar.setStyle("-fx-background-color:white;-fx-background-radius:10;");

        XYChart.Series<String, Number> credS = new XYChart.Series<>(); credS.setName("Receives (Rs.)");
        XYChart.Series<String, Number> debtS = new XYChart.Series<>(); debtS.setName("Owes (Rs.)");
        for (Map.Entry<String, Double> e : balances.entrySet()) {
            if      (e.getValue() >  0.01) credS.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
            else if (e.getValue() < -0.01) debtS.getData().add(new XYChart.Data<>(e.getKey(), -e.getValue()));
        }
        bar.getData().addAll(credS, debtS);
        HBox.setHgrow(bar, Priority.ALWAYS);

        // Pie chart — who paid how much
        PieChart pie = new PieChart();
        pie.setTitle("Who Paid How Much");
        pie.setPrefHeight(270);
        pie.setAnimated(false);
        pie.setStyle("-fx-background-color:white;-fx-background-radius:10;");
        Map<String, Double> paid = DB.getPaidByPerson(groupId);
        if (paid.isEmpty()) {
            pie.getData().add(new PieChart.Data("No data yet", 1));
        } else {
            paid.forEach((name, amt) ->
                pie.getData().add(new PieChart.Data(
                    name + " Rs." + String.format("%.0f", amt), amt)));
        }
        HBox.setHgrow(pie, Priority.ALWAYS);

        HBox charts = new HBox(16, bar, pie);
        charts.setFillHeight(true);

        // Transaction cards
        VBox txnList = new VBox(8);
        if (txns.isEmpty()) {
            txnList.getChildren().add(infoBox("Everyone is settled up!"));
        } else {
            for (int i = 0; i < txns.size(); i++) {
                Txn t = txns.get(i);
                HBox card = new HBox(12);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color:white;-fx-padding:14 18;" +
                    "-fx-background-radius:8;-fx-border-color:#eee;-fx-border-radius:8;");

                Label num   = new Label(String.valueOf(i + 1));
                num.setStyle("-fx-background-color:#EDE9FB;-fx-text-fill:#5B4EBB;" +
                    "-fx-font-size:11px;-fx-font-weight:500;-fx-padding:3 8;-fx-background-radius:10;");
                Label from  = label(t.from, 13, "#222");
                from.setStyle(from.getStyle() + "-fx-font-weight:500;");
                Label arrow = label("  pays  Rs." + String.format("%.2f", t.amount) + "  →  ", 13, RED);
                Label to    = label(t.to, 13, GREEN);
                to.setStyle(to.getStyle() + "-fx-font-weight:500;");

                card.getChildren().addAll(num, from, arrow, to);
                txnList.getChildren().add(card);
            }
        }

        Label countLbl = label("Minimum transactions needed: " + txns.size(), 12, "#888");

        Button backBtn = ghostButton("← Back to Overview");
        backBtn.setOnAction(e -> { setActive(nav, 0); root.setCenter(buildOverview()); });

        VBox page = new VBox(20,
            pageTitle("Settle Up"),
            charts,
            sectionTitle("Settlement Plan"),
            countLbl,
            txnList,
            backBtn
        );
        return scrollPage(page);
    }

    // ─────────────────────────────────────────
    //  PANEL 4 — HISTORY
    // ─────────────────────────────────────────
    private Node buildHistory(Button[] nav) {
        List<String[]> history = DB.getHistory(groupId);

        TableView<String[]> table = new TableView<>();
       table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setStyle("-fx-background-color:white;-fx-background-radius:10;");
        table.setPrefHeight(400);

        String[] cols = {"Paid By", "Description", "Amount (Rs.)", "Date"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> col = new TableColumn<>(cols[i]);
            col.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[idx]));
            col.setStyle("-fx-font-size:13px;");
            table.getColumns().add(col);
        }
        table.setItems(FXCollections.observableArrayList(history));

        if (history.isEmpty()) {
            table.setPlaceholder(new Label("No expenses yet. Add some first."));
        }

        Button backBtn = ghostButton("← Back to Overview");
        backBtn.setOnAction(e -> { setActive(nav, 0); root.setCenter(buildOverview()); });

        VBox page = new VBox(20,
            pageTitle("Expense History"),
            label(history.size() + " expenses recorded", 12, "#888"),
            table,
            backBtn
        );
        return scrollPage(page);
    }

    // ─────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────
    private void fillEqual(TextField amtField, List<CheckBox> checks,
                            Map<Integer, TextField> shareFields) {
        try {
            double amt  = Double.parseDouble(amtField.getText().trim());
            long   cnt  = checks.stream().filter(CheckBox::isSelected).count();
            if (cnt < 1) return;
            double share = amt / cnt;
            for (int i = 0; i < checks.size(); i++) {
                if (checks.get(i).isSelected())
                    shareFields.get(members.get(i).getId()).setText(String.format("%.2f", share));
                else
                    shareFields.get(members.get(i).getId()).clear();
            }
        } catch (NumberFormatException ignored) {}
    }

    private Button navButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        String base = "-fx-background-color:transparent;-fx-text-fill:#a0a0cc;" +
            "-fx-font-size:13px;-fx-padding:10 14;-fx-background-radius:6;" +
            "-fx-cursor:hand;-fx-border-color:transparent;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> { if (!b.getStyle().contains(PURPLE)) b.setStyle(base.replace("transparent", "#2a2a5a")); });
        b.setOnMouseExited(e ->  { if (!b.getStyle().contains(PURPLE)) b.setStyle(base); });
        return b;
    }

    private void setActive(Button[] btns, int i) {
        String base = "-fx-background-color:transparent;-fx-text-fill:#a0a0cc;" +
            "-fx-font-size:13px;-fx-padding:10 14;-fx-background-radius:6;" +
            "-fx-cursor:hand;-fx-border-color:transparent;";
        String active = "-fx-background-color:" + PURPLE + ";-fx-text-fill:white;" +
            "-fx-font-size:13px;-fx-padding:10 14;-fx-background-radius:6;" +
            "-fx-cursor:hand;-fx-border-color:transparent;";
        for (Button b : btns) b.setStyle(base);
        btns[i].setStyle(active);
    }

    private VBox statCard(String lbl, String val, String color) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color:white;-fx-padding:18 20;" +
            "-fx-background-radius:10;-fx-border-color:#eee;-fx-border-radius:10;");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label l = label(lbl, 11, "#999");
        Label v = label(val, 22, color);
        v.setStyle(v.getStyle() + "-fx-font-weight:500;");
        card.getChildren().addAll(l, v);
        return card;
    }

    private Node infoBox(String msg) {
        Label l = label(msg, 13, "#555");
        l.setStyle(l.getStyle() + "-fx-background-color:white;-fx-padding:16 20;" +
            "-fx-background-radius:8;-fx-border-color:#eee;-fx-border-radius:8;");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private Label pageTitle(String t) {
        Label l = label(t, 22, "#222");
        l.setStyle(l.getStyle() + "-fx-font-weight:500;");
        return l;
    }

    private Label sectionTitle(String t) {
        return label(t, 13, "#777");
    }

    private ScrollPane scrollPage(VBox content) {
        content.setStyle("-fx-padding:30 36;-fx-background-color:" + PAGE_BG + ";");
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:" + PAGE_BG + ";");
        return sp;
    }

    private Label label(String text, double size, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:" + size + "px;-fx-text-fill:" + color + ";");
        return l;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-font-size:13px;-fx-padding:9 12;" +
            "-fx-border-color:#e0e0e0;-fx-border-radius:6;-fx-background-radius:6;");
        return tf;
    }

    private Button accentButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + PURPLE + ";-fx-text-fill:white;" +
            "-fx-font-size:13px;-fx-padding:10 24;-fx-background-radius:6;-fx-cursor:hand;");
        return b;
    }

    private Button ghostButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:transparent;-fx-text-fill:" + PURPLE + ";" +
            "-fx-font-size:12px;-fx-cursor:hand;-fx-border-color:transparent;-fx-padding:6 0;");
        return b;
    }
}
