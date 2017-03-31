package pl.edu.agh.dsrg.sr.chat.protos;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.List;
import java.util.Map;

import static pl.edu.agh.dsrg.sr.chat.protos.Main.debug;
import static pl.edu.agh.dsrg.sr.chat.protos.Main.log;

public class ActiveChannelsStateUpdater {
    private TreeView treeView;

    public ActiveChannelsStateUpdater(TreeView treeView) {
        this.treeView = treeView;
    }

    public void updateChannelsTree(Map<String, List<String>> map) {
        if (debug) log("Active channels and users: " + map.toString());

        TreeItem<String> allChannelsItem = new TreeItem<>("Active channels");
        for (String channel : map.keySet()) {
            TreeItem<String> channelItem = new TreeItem<>("Channel " + channel);
            channelItem.setExpanded(true);

            for (String user : map.get(channel)) {
                TreeItem<String> userItem = new TreeItem<>(user);
                userItem.setExpanded(true);
                channelItem.getChildren().add(userItem);
            }

            allChannelsItem.getChildren().add(channelItem);
        }
        treeView.setRoot(allChannelsItem);
        treeView.setShowRoot(false);
    }
}
