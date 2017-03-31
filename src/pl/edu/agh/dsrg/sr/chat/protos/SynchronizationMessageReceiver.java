package pl.edu.agh.dsrg.sr.chat.protos;

import javafx.application.Platform;
import org.jgroups.*;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatState.Builder;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.*;
import static pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.*;
import static pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.ActionType.*;
import static pl.edu.agh.dsrg.sr.chat.protos.Main.debug;
import static pl.edu.agh.dsrg.sr.chat.protos.Main.log;

public class SynchronizationMessageReceiver extends ReceiverAdapter {
    private Map<String, List<String>> ChannelToUserMap;
    private ActiveChannelsStateUpdater activeChannelsStateUpdater;
    private JChannel chatManagement;

    public SynchronizationMessageReceiver(ActiveChannelsStateUpdater activeChannelsStateUpdater, JChannel chatManagement) {
        ChannelToUserMap = new HashMap<>();
        this.activeChannelsStateUpdater = activeChannelsStateUpdater;
        this.chatManagement = chatManagement;
    }

    @Override
    public void receive(Message message) {
        synchronized (ChannelToUserMap) {
            try {
                ChatAction action = parseFrom(message.getBuffer());
                ActionType actionType = action.getAction();
                String channel = action.getChannel();
                String user = action.getNickname();

                if (actionType == JOIN) {
                    /** Channel have been already created -> Add user to channel */
                    if (ChannelToUserMap.containsKey(channel))
                        ChannelToUserMap.get(channel).add(user);
                    /** Channel have not been already created -> Add also channel, then user */
                    else {
                        ChannelToUserMap.put(channel, new LinkedList<>());
                        ChannelToUserMap.get(channel).add(user);
                    }
                }
                else if (actionType == LEAVE) {
                    if (ChannelToUserMap.containsKey(channel)) {
                        ChannelToUserMap.get(channel).remove(user);
                        if (ChannelToUserMap.get(channel).isEmpty())
                            ChannelToUserMap.remove(channel);
                    }
                }
            } catch (Exception e) {
                log("Exception in msg receiver menagement: " + e);
            }
        }

        Platform.runLater(() -> activeChannelsStateUpdater.updateChannelsTree(ChannelToUserMap));
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (ChannelToUserMap) {
            Builder builder = ChatState.newBuilder();

            for (String channel : ChannelToUserMap.keySet()) {
                for (String user : ChannelToUserMap.get(channel)) {
                    builder.addStateBuilder().
                            setAction(JOIN).
                            setChannel(channel).
                            setNickname(user);
                }
            }

            ChatState chatState = builder.build();
            chatState.writeTo(output);
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        synchronized (ChannelToUserMap) {
            ChatState state = ChatState.parseFrom(input);
            ChannelToUserMap.clear();

            for (ChatAction action : state.getStateList()) {
                if (!ChannelToUserMap.containsKey(action.getChannel())) {
                    ChannelToUserMap.put(action.getChannel(), new LinkedList<>());
                }

                ChannelToUserMap.get(action.getChannel()).add(action.getNickname());
            }

            Platform.runLater(() -> activeChannelsStateUpdater.updateChannelsTree(ChannelToUserMap));
        }
    }

    @Override
    public void viewAccepted(View view) {
        synchronized (ChannelToUserMap) {
            List<String> currentUsers = new LinkedList<>();
            if (debug) log("ViewAccepted: " + view.getMembers().toString());
            if (debug) log("ViewAccepted: " + view.getMembers().toString());

            for (Address address : view.getMembers()) {
                currentUsers.add(chatManagement.getName(address));
            }

            for (String channel : ChannelToUserMap.keySet()) {
                List<String> disconnectedUsers = new LinkedList<>(ChannelToUserMap.get(channel));
                disconnectedUsers.removeAll(currentUsers);

                for (String disc : disconnectedUsers) {
                    ChannelToUserMap.get(channel).remove(disc);
                }

                if (ChannelToUserMap.get(channel).isEmpty()) {
                    ChannelToUserMap.remove(channel);
                }
            }
            Platform.runLater(() -> activeChannelsStateUpdater.updateChannelsTree(ChannelToUserMap));
        }
    }
}
