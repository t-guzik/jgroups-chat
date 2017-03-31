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
import java.util.stream.Collectors;

import static pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.*;
import static pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.*;
import static pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.ActionType.*;
import static pl.edu.agh.dsrg.sr.chat.protos.Main.debug;
import static pl.edu.agh.dsrg.sr.chat.protos.Main.log;

public class SynchronizationMessageReceiver extends ReceiverAdapter {
    private Map<String, List<String>> ChannelToUserMap;
    private ActiveChannelsStateUpdater activeChannelsStateUpdater;
    private JChannel channelsSynchronizer;

    public SynchronizationMessageReceiver(ActiveChannelsStateUpdater activeChannelsStateUpdater, JChannel channelsSynchronizer) {
        ChannelToUserMap = new HashMap<>();
        this.activeChannelsStateUpdater = activeChannelsStateUpdater;
        this.channelsSynchronizer = channelsSynchronizer;
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
    /** The getState() method's first argument is the target instance,
     * and null means get the state from the first instance (the coordinator).
     * The second argument is the timeout */
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
    /** The setState() method is called on the state requester, ie. the instance which called JChannel.getState().
     * Its task is to read the state from the input stream and set i */
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
    /** Called whenever a new instance joins the cluster, or an existing instance leaves (crashes included).
     * Its toString() method prints out the view ID (an increasing ID) and a list of the current instances in the cluster
     */
    public void viewAccepted(View view) {
        synchronized (ChannelToUserMap) {
            List<String> currentUsers = view.getMembers().stream().map(address -> channelsSynchronizer.getName(address)).collect(Collectors.toCollection(LinkedList::new));

            if (debug) log("ViewAccepted: Current users " + currentUsers.toString());

            for (String channel : ChannelToUserMap.keySet()) {
                List<String> disconnectedUsers = new LinkedList<>(ChannelToUserMap.get(channel));
                if (debug) log("ViewAccepted: Disconnected users from channel " + channel + ": " + disconnectedUsers.toString());

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
