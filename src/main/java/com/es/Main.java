package com.es;

import com.es.db.DB;
import com.es.ui.App;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        DB.init();
        new App(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
