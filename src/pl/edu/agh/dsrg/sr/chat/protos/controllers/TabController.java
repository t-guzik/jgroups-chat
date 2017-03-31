package pl.edu.agh.dsrg.sr.chat.protos.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.jgroups.JChannel;
import pl.edu.agh.dsrg.sr.chat.protos.ChannelsSynchronizer;
import pl.edu.agh.dsrg.sr.chat.protos.MessageReceiver;

import static pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.ActionType.JOIN;
import static pl.edu.agh.dsrg.sr.chat.protos.Main.debug;
import static pl.edu.agh.dsrg.sr.chat.protos.Main.log;
import static pl.edu.agh.dsrg.sr.chat.protos.controllers.AppController.login;

public class TabController {
    private ChannelsSynchronizer channelsSynchronizer;
    private JChannel channel;

    @FXML
    private TextField msgTextField;

    @FXML
    private TextArea chatTextArea;

    public void sendBtnFun(ActionEvent actionEvent) throws Exception {
        channelsSynchronizer.send(channel, login + ": " + msgTextField.getText());
        msgTextField.clear();
    }

    public void setChannelsSynchronizer(ChannelsSynchronizer channelsSynchronizer) {
        this.channelsSynchronizer = channelsSynchronizer;
        if (debug) log("Set tab chat managment " + channelsSynchronizer.getSynchronizationChannel().getAddressAsString());
    }

    /** Set newly-created channel msg receiver, send msg to synchronization channel and assign channel to Tab. */
    public void setChannel(JChannel channel) throws Exception{
        channel.setReceiver(new MessageReceiver(this));
        channelsSynchronizer.sendSynchronizationMessage(JOIN, channel.getName());
        this.channel = channel;
        if (debug) log("Assigned channel " + channel.getAddressAsString() + " to Tab");
    }

    public TextArea getChatTextArea() {
        return this.chatTextArea;
    }
}

