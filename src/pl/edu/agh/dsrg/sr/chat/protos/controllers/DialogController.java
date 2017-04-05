package pl.edu.agh.dsrg.sr.chat.protos.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import pl.edu.agh.dsrg.sr.chat.protos.Main;

import java.io.IOException;
import java.net.URL;

import static pl.edu.agh.dsrg.sr.chat.protos.controllers.AppController.login;

public class DialogController {
    @FXML
    private Label infoLbl;
    @FXML
    private TextField loginTextField;
    private URL rootURL = getClass().getResource("../fxml/App.fxml");

    public void saveBtnFun(ActionEvent actionEvent) throws IOException {
        loginTextField.setText(loginTextField.getText().replaceAll("\\s+", ""));
        if(loginTextField.getText().equals("")){
            infoLbl.setText("Set your login first!");
            infoLbl.setStyle("-fx-text-fill : red");
        }
        else {
            login = loginTextField.getText().replaceAll("\\s+", "");

            if (Main.debug) Main.log("User " + loginTextField.getText() + " logged in");

            FXMLLoader fxmlLoader = new FXMLLoader(rootURL);
            /** Initialize root app */
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 640, 400);
            Main.getPrimaryStage().setScene(scene);
            Main.getPrimaryStage().centerOnScreen();
        }
    }
}
