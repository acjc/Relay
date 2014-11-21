package uk.co.acjc.relay.common;

public class MessageContract {

    public static final String REQUEST_PHONE_STATUS_PATH = "/request/phone_status";
    public static final String STOP_PHONE_STATUS_PATH = "/stop/phone_status";

    public static final String TIMESTAMP_KEY = "uk.uk.co.acjc.phone_status.TIMESTAMP";

    public static class BatteryStatus {

        private BatteryStatus() {}

        public static final String CHARGED_KEY = "uk.uk.co.acjc.phone_status.CHARGED";
        public static final String CHARGING_KEY = "uk.uk.co.acjc.phone_status.CHARGING";
        public static final String DISCHARGING_GOOD_HEALTH_KEY = "uk.uk.co.acjc.phone_status.DISCHARGING_GOOD_HEALTH";
        public static final String DISCHARGING_AVERAGE_HEALTH_KEY = "uk.uk.co.acjc.phone_status.DISCHARGING_AVERAGE_HEALTH";
        public static final String BATTERY_LOW_KEY = "uk.uk.co.acjc.phone_status.BATTERY_LOW";
    }

    public static class BatteryInfo {

        private BatteryInfo() {}

        public static final String BATTERY_LEVEL_PATH = "/phone_status/battery_level";

        public static final String BATTERY_CHARGING_KEY = "uk.uk.co.acjc.phone_status.BATTERY_CHARGING";
        public static final String BATTERY_LEVEL_KEY = "uk.uk.co.acjc.phone_status.BATTERY_LEVEL";
    }

    public static class Connectivity {

        private Connectivity() {}

        public static final String CONNECTIVITY_PATH = "/phone_status/connectivity";

        public static final String CONNECTIVITY_KEY = "uk.uk.co.acjc.phone_status.CONNECTIVITY";
        public static final String SSID_KEY = "uk.uk.co.acjc.phone_status.SSID";
        public static final String WIFI_STRENGTH_KEY = "uk.uk.co.acjc.phone_status.WIFI_STRENGTH";
        public static final String MOBILE_DATA_TYPE_KEY = "uk.uk.co.acjc.phone_status.MOBILE_DATA_TYPE";
        public static final String MOBILE_DATA_STRENGTH_KEY = "uk.uk.co.acjc.phone_status.MOBILE_DATA_STRENGTH";
    }
}
