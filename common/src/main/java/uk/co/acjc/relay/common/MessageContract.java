package uk.co.acjc.relay.common;

public class MessageContract {

    public static final String REQUEST_BASIC_INFO_PATH = "/relay/request/basic_info";
    public static final String STOP_BASIC_INFO_PATH = "/relay/stop/basic_info";

    public static final String TIMESTAMP_KEY = "uk.co.acjc.relay.TIMESTAMP";

    public static class BatteryStatus {

        private BatteryStatus() {}

        public static final String CHARGED_KEY = "uk.co.acjc.relay.CHARGED";
        public static final String CHARGING_KEY = "uk.co.acjc.relay.CHARGING";
        public static final String DISCHARGING_GOOD_HEALTH_KEY = "uk.co.acjc.relay.DISCHARGING_GOOD_HEALTH";
        public static final String DISCHARGING_AVERAGE_HEALTH_KEY = "uk.co.acjc.relay.DISCHARGING_AVERAGE_HEALTH";
        public static final String BATTERY_LOW_KEY = "uk.co.acjc.relay.BATTERY_LOW";
    }

    public static class BatteryInfo {

        private BatteryInfo() {}

        public static final String BATTERY_LEVEL_PATH = "/relay/battery_level";

        public static final String BATTERY_CHARGING_KEY = "uk.co.acjc.relay.BATTERY_CHARGING";
        public static final String BATTERY_LEVEL_KEY = "uk.co.acjc.relay.BATTERY_LEVEL";
    }

    public static class Connectivity {

        private Connectivity() {}

        public static final String CONNECTIVITY_PATH = "/relay/connectivity";

        public static final String CONNECTIVITY_KEY = "uk.co.acjc.relay.CONNECTIVITY";
        public static final String SSID_KEY = "uk.co.acjc.relay.SSID";
        public static final String WIFI_STRENGTH_KEY = "uk.co.acjc.relay.WIFI_STRENGTH";
        public static final String MOBILE_DATA_TYPE_KEY = "uk.co.acjc.relay.MOBILE_DATA_TYPE";
        public static final String MOBILE_DATA_STRENGTH_KEY = "uk.co.acjc.relay.MOBILE_DATA_STRENGTH";
    }
}
