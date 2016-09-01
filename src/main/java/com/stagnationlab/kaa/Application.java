package com.stagnationlab.kaa;


import java.util.List;

import org.kaaproject.kaa.client.DesktopKaaPlatformContext;
import org.kaaproject.kaa.client.Kaa;
import org.kaaproject.kaa.client.KaaClient;
import org.kaaproject.kaa.client.notification.NotificationListener;
import org.kaaproject.kaa.client.notification.NotificationTopicListListener;
import org.kaaproject.kaa.common.endpoint.gen.Topic;
import com.stagnationlab.kaa.schema.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        new Application().launch();
    }

    private void launch() {
        // Create client for Kaa SDK
        KaaClient kaaClient = Kaa.newClient(new DesktopKaaPlatformContext());
        // Starts Kaa SDK client
        kaaClient.start();
        LOG.info("Kaa SDK client started!");
        // Registering listener for topic updates
        kaaClient.addTopicListListener(new NotificationTopicListListener() {
            @Override
            public void onListUpdated(List<Topic> topicList) {
                LOG.info("Topic list updated!");
                for (Topic topic : topicList) {
                    LOG.info("Received topic with id {} and name {}",
                            topic.getId(), topic.getName());
                }
            }
        });
        // Registering listener for notifications
        kaaClient.addNotificationListener(new NotificationListener() {
            @Override
            public void onNotification(long topicId, Notification notification) {
                LOG.info("Received notification {} for topic with id {}",
                        notification, topicId);
            }
        });
    }

}
