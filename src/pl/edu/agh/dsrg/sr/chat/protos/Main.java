package pl.edu.agh.dsrg.sr.chat.protos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {
    public static boolean debug = false;
    private URL dialogURL = getClass().getResource("fxml/Dialog.fxml");
    private static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        setPrimaryStage(primaryStage);

        /** Initialize logging dialog */
        Scene scene = new Scene(new FXMLLoader(dialogURL).load(), 280, 140);
        primaryStage.setScene(scene);
        primaryStage.setTitle("C.H.A.T.");
        primaryStage.show();
    }

    private void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void log(String s) {
        System.out.println("[DEBUG]: " + s);
    }
}
