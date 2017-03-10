package ambientsensors.rhul.com.ambientsensorevalreader.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.List;
import java.util.Objects;

public class WiFiSensor extends AbstractSensor {

    private Context context;
    private WifiManager mWifiManager;
    private List<ScanResult> mScanResults;
    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (Objects.equals(intent.getAction(), WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mScanResults = mWifiManager.getScanResults();
            }
        }
    };

    public WiFiSensor(Context context) {
//        super(context, -1, SensorEnum.WiFi.name());
        super(context, -1, "WiFi");
        this.context = context;
    }

    @Override
    protected void stopSensor() {
        context.unregisterReceiver(mWifiScanReceiver);
    }

    @Override
    public void generateData(String rnd_id) {
        if (!mWifiManager.isWifiEnabled())
            mainAct.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainAct.noWiFi();
                }
            });
        else {
            String data = "<measurements>";

            if (mScanResults != null)
                for (ScanResult i : mScanResults) {
                    data += "<measurement>";
                    data += "<bssid>" + i.BSSID + "</bssid>";
                    data += "<ssid>" + i.SSID + "</ssid>";
                    data += "<strength>" + i.level + "</strength>";
                    data += "<capabilities>" + i.capabilities + "</capabilities>";
                    data += "<frequency>" + i.frequency + "</frequency>";
                    data += "<timestamp>" + i.timestamp + "</timestamp>";
                    data += "</measurement>";
                }

            data += "</measurements>";

            db.writeToDB(mainAct.getLocation().name(), startTime, rnd_id, data);
        }
    }

    @Override
    protected void initSensor() {
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        context.registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager.startScan();
    }
}
