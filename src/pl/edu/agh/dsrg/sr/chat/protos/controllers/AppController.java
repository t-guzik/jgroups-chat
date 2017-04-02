package pl.edu.agh.dsrg.sr.chat.protos.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import org.jgroups.JChannel;
import pl.edu.agh.dsrg.sr.chat.protos.ChannelsSynchronizer;
import pl.edu.agh.dsrg.sr.chat.protos.SynchronizationMessageReceiver;
import pl.edu.agh.dsrg.sr.chat.protos.ActiveChannelsStateUpdater;

import java.net.URL;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.ActionType.LEAVE;
import static pl.edu.agh.dsrg.sr.chat.protos.Main.getPrimaryStage;
import static pl.edu.agh.dsrg.sr.chat.protos.Main.log;

public class AppController {
    public static String login;
    public static boolean channelExists;
    private static ChannelsSynchronizer channelsSynchronizer;
    private URL tabURL = getClass().getResource("../fxml/Tab.fxml");

    @FXML
    private TabPane channelsTabPane;
    @FXML
    private ComboBox<Integer> channelComboBox;
    @FXML
    private TreeView<String> channelsTreeView;

    @FXML
    public void initialize() throws Exception {
        channelsTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        channelComboBox.setItems(FXCollections.observableArrayList(IntStream
                .rangeClosed(1, 100).boxed().collect(Collectors.toList())));
        channelComboBox.setVisibleRowCount(5);
        channelComboBox.getSelectionModel().selectFirst();
        channelsTreeView.getRoot().getChildren().addListener(childrenChanged);

        channelsSynchronizer = new ChannelsSynchronizer();
        channelsSynchronizer.getSynchronizationChannel()
                .setReceiver(new SynchronizationMessageReceiver(new ActiveChannelsStateUpdater(channelsTreeView), channelsSynchronizer.getSynchronizationChannel()));
        channelsSynchronizer.getSynchronizationChannel().getState(null, 10000);

        getPrimaryStage().setOnCloseRequest(e -> {
            try {
                channelsSynchronizer.leaveSynchronizationChannel();
            } catch (Exception e1) {
                log("Exception in app closing: " + e1);
            }
            System.exit(0);
        });
    }

    /**
     * Join to new channel
     */
    public void joinBtnFun(ActionEvent actionEvent) throws Exception {
        final JChannel channel = channelsSynchronizer.joinChannel(channelComboBox.getValue().toString());

        if (channelExists) {
            /** Create new tab */
            FXMLLoader fxmlLoader = new FXMLLoader(tabURL);
            Parent root = fxmlLoader.load();
            TabController tabController = fxmlLoader.getController();

            /** Set channel responsible for synchronization and specific tab channel */
            tabController.setChannelsSynchronizer(channelsSynchronizer);
            tabController.setChannel(channel);

            Tab tab = new Tab("Channel " + channelComboBox.getValue());
            channelsTabPane.getTabs().add(tab);
            channelsTabPane.getTabs().get(channelsTabPane.getTabs().size() - 1).setContent(root);

            tab.setOnClosed(new EventHandler<Event>() {
                @Override
                public void handle(Event event) {
                    channelsSynchronizer.leaveChannel(channel);
                    try {
                        channelsSynchronizer.sendSynchronizationMessage(LEAVE, channel.getName());
                    } catch (Exception e) {
                        log("Exception in tab closing: " + e);
                    }
                }
            });
        }
        channelComboBox.getSelectionModel().selectFirst();
    }

    /** TreeView listener*/
    private final ListChangeListener<TreeItem<String>> childrenChanged
            = new ListChangeListener<TreeItem<String>>() {

        @Override
        public void onChanged(
                javafx.collections.ListChangeListener.Change<? extends TreeItem<String>> change) {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (TreeItem<String> item : change.getAddedSubList()) {
                        item.getChildren().addListener(childrenChanged);
                    }
                } else if (change.wasRemoved()) {
                    for (TreeItem<String> item : change.getRemoved()) {
                        item.getChildren().removeListener(childrenChanged);
                    }
                }
            }
        }
    };
}
