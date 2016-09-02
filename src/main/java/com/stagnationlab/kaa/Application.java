package com.stagnationlab.kaa;

import com.stagnationlab.kaa.configuration.DeviceConfiguration;
import com.stagnationlab.kaa.log.TemperatureMeasurement;
import com.stagnationlab.kaa.profile.DeviceProfile;
import org.kaaproject.kaa.client.DesktopKaaPlatformContext;
import org.kaaproject.kaa.client.Kaa;
import org.kaaproject.kaa.client.KaaClient;
import org.kaaproject.kaa.client.SimpleKaaClientStateListener;
import org.kaaproject.kaa.client.configuration.base.ConfigurationListener;
import org.kaaproject.kaa.client.logging.BucketInfo;
import org.kaaproject.kaa.client.logging.LogDeliveryListener;
import org.kaaproject.kaa.client.logging.strategies.RecordCountLogUploadStrategy;
import org.kaaproject.kaa.common.endpoint.gen.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private KaaClient client;

    static class ReportTemperatureMeasurementTask extends TimerTask {

        private static final Logger log = LoggerFactory.getLogger(ReportTemperatureMeasurementTask.class);

        private KaaClient client;

        public ReportTemperatureMeasurementTask(KaaClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            double temperature = Math.random() * 10.0 + 10.0;
            String unit = "C";

            TemperatureMeasurement temperatureMeasurement = new TemperatureMeasurement();

            temperatureMeasurement.setTemperature(temperature);
            temperatureMeasurement.setUnit(unit);

            log.info("sending temperature measurement: " + temperature + unit);

            client.addLogRecord(temperatureMeasurement);
        }
    }

    public static void main(String[] args) {
        new Application().launch();
    }

    private void launch() {
        // use desktop platform context
        DesktopKaaPlatformContext platformContext = new DesktopKaaPlatformContext();

        // setup state listener
        SimpleKaaClientStateListener stateListener = new SimpleKaaClientStateListener() {
            @Override
            public void onStarted() {
                super.onStarted();

                String keyHash = client.getEndpointKeyHash();
                String accessToken  = client.getEndpointAccessToken();

                log.info("kaa client started, key hash: {}, access token: {}", keyHash, accessToken);

                showConfiguration();
                setupPeriodicTasks();
            }

            @Override
            public void onStopped() {
                super.onStopped();

                log.info("kaa client stopped");
            }
        };

        // create client
        client = Kaa.newClient(
                platformContext,
                stateListener
        );

        // create profile info
        DeviceProfile profile = new DeviceProfile();

        profile.setModel("HCM1152D");
        profile.setSerial("237345645723456");
        profile.setVersion("1.0.0");

        // set the profile to use
        client.setProfileContainer(() -> profile);

        // listen for log delivery status
        client.setLogDeliveryListener(new LogDeliveryListener() {
            @Override
            public void onLogDeliverySuccess(BucketInfo bucketInfo) {
                log.info("log delivered successfully");
            }

            @Override
            public void onLogDeliveryFailure(BucketInfo bucketInfo) {
                log.warn("log delivery failed");
            }

            @Override
            public void onLogDeliveryTimeout(BucketInfo bucketInfo) {
                log.warn("log delivery timed out");
            }
        });

        // listen for configuration changes
        client.addConfigurationListener(configuration -> {
            log.info("configuration updated");

            showConfiguration();
        });

        // set log upload strategy to use
        client.setLogUploadStrategy(new RecordCountLogUploadStrategy(1));

        // start client
        client.start();

        // listen for topic changes
        client.addTopicListListener(topicList -> {
            log.info("topic list updated");

            for (Topic topic : topicList) {
                log.info("received topic with id {} and name {}", topic.getId(), topic.getName());
            }
        });

        // listen for notifications
        client.addNotificationListener((topicId, notification) -> {
            log.info("Received notification {} for topic with id {}", notification, topicId);
        });
    }

    private void showConfiguration() {
        DeviceConfiguration configuration = client.getConfiguration();

        String ledPin = configuration.getLedPin();

        log.info("configured led pin: " + ledPin);
    }

    private void setupPeriodicTasks() {
        log.info("setting up periodic tasks");

        Timer timer = new Timer();
        ReportTemperatureMeasurementTask task = new ReportTemperatureMeasurementTask(client);
        timer.scheduleAtFixedRate(task, 0, 5000);
    }

}
