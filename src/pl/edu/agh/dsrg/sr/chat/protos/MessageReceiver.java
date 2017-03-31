package pl.edu.agh.dsrg.sr.chat.protos;

import com.google.protobuf.InvalidProtocolBufferException;
import javafx.application.Platform;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import pl.edu.agh.dsrg.sr.chat.protos.controllers.TabController;

import static pl.edu.agh.dsrg.sr.chat.protos.Main.log;

public class MessageReceiver extends ReceiverAdapter {
    private TabController tabController;

    public MessageReceiver(TabController tabController) {
        this.tabController = tabController;
    }

    @Override
    public void receive(Message msg) {
        try {
            ChatOperationProtos.ChatMessage message = ChatOperationProtos.ChatMessage.parseFrom(msg.getBuffer());
            Platform.runLater(() -> tabController.getChatTextArea().appendText(message.getMessage() + "\n"));
        } catch (InvalidProtocolBufferException e) {
            log("Exception in receiving msg: " + e);
        }
    }
}
