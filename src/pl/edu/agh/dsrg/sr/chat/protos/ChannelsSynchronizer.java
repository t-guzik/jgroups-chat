package pl.edu.agh.dsrg.sr.chat.protos;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.ActionType;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import static pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatMessage.newBuilder;
import static pl.edu.agh.dsrg.sr.chat.protos.Main.debug;
import static pl.edu.agh.dsrg.sr.chat.protos.Main.log;
import static pl.edu.agh.dsrg.sr.chat.protos.controllers.AppController.channelExists;
import static pl.edu.agh.dsrg.sr.chat.protos.controllers.AppController.login;

/**
 * Class responsible for synchronization using JChannel.
 */
public class ChannelsSynchronizer {
    private Map<String, JChannel> channels;
    private JChannel synchronizationChannel;

    public ChannelsSynchronizer() throws Exception {
        channels = new LinkedHashMap<>();

        /** Join synchronization channel */
        synchronizationChannel = new JChannel(false);
        configureChannel(synchronizationChannel, null);
        synchronizationChannel.connect("ChatManagement321321");
        if (debug){
            log("ChannelsSynchronizer created list of active channels!");
            log("Synchronization channel \"ChatManagement321321\" connected");
        }
    }

    public void leaveSynchronizationChannel() throws Exception {
        synchronizationChannel.close();
    }

    /** LEAVE or JOIN given channel */
    public void sendSynchronizationMessage(ActionType type, String channel) throws Exception {
        ChatAction action = ChatAction.newBuilder()
                .setAction(type)
                .setChannel(channel)
                .setNickname(login).build();

        Message msg = new Message(null, null, action.toByteArray());
        synchronizationChannel.send(msg);
    }

    public void send(JChannel channel, String text) throws Exception {
        channel.send(new Message(null, null, newBuilder().setMessage(text).build().toByteArray()));
    }

    /**
     * Join user to new channel.
     *
     * @param number channel number
     * @return new JChannel if doesnt exist or null if already created
     * @throws Exception
     */
    public JChannel joinChannel(String number) throws Exception {
        if (channels.containsKey(number)) {
            if(channels.get(number).isConnected()){
                log("User " + login + " have already joined to channel " + number);
                channelExists = false;
                return channels.get(number);
            }
            /** Channel exiists but not connected */
            else {
                channels.get(number).connect(number);
                log("User " + login + " joined channel " + number);
                return channels.get(number);
            }
        }

        JChannel channel = new JChannel(false);
        configureChannel(channel, number);
        channel.setName(number);
        channel.connect(number);
        channels.put(number, channel);
        channelExists = true;

        log("User " + login + " joined channel " + number);
        return channel;
    }

    public void leaveChannel(JChannel channel) {
        channel.setReceiver(null);
        channel.disconnect();
    }

    private void initStack(ProtocolStack stack, String address) throws Exception {
        Protocol UDP = new UDP();

        if (address != null) {
            UDP.setValue("mcast_group_addr", InetAddress.getByName("230.0.0." + address));
        }

        stack.addProtocol(UDP)
                .addProtocol(new PING())
                .addProtocol(new MERGE3())
                .addProtocol(new FD_SOCK())
                .addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
                .addProtocol(new VERIFY_SUSPECT())
                .addProtocol(new BARRIER())
                .addProtocol(new NAKACK2())
                .addProtocol(new UNICAST3())
                .addProtocol(new STABLE())
                .addProtocol(new GMS())
                .addProtocol(new UFC())
                .addProtocol(new MFC())
                .addProtocol(new FRAG2())
                .addProtocol(new STATE_TRANSFER())
                .addProtocol(new FLUSH());
        stack.init();
    }

    private void configureChannel(JChannel channel, String number) throws Exception {
        ProtocolStack stack = new ProtocolStack();
        channel.setProtocolStack(stack);
        initStack(stack, number);
    }

    public JChannel getSynchronizationChannel() {
        return synchronizationChannel;
    }
}