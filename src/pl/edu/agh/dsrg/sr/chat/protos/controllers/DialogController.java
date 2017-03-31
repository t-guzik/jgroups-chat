package pl.edu.agh.dsrg.sr.chat.protos.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import pl.edu.agh.dsrg.sr.chat.protos.Main;

import java.io.IOException;
import java.net.URL;

import static pl.edu.agh.dsrg.sr.chat.protos.controllers.AppController.login;

public class DialogController {
    @FXML
    private TextField loginTextField;
    private URL rootURL = getClass().getResource("../fxml/App.fxml");

    public void saveBtnFun(ActionEvent actionEvent) throws IOException {
        login = loginTextField.getText();
        if (Main.debug) Main.log("User " + loginTextField.getText() + " logged in");

        FXMLLoader fxmlLoader = new FXMLLoader(rootURL);
        /** Initialize root app */
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 640, 400);
        Main.getPrimaryStage().setScene(scene);
        Main.getPrimaryStage().centerOnScreen();
    }
}
