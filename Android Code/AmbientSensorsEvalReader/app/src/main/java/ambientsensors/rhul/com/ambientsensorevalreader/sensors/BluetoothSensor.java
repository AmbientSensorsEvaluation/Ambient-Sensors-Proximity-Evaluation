package ambientsensors.rhul.com.ambientsensorevalreader.sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;

public class BluetoothSensor extends AbstractSensor {

    private Context context;
    private List<BluetoothDevice> deviceList;
    private final BroadcastReceiver mBTScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceList.add(device);
            }
        }
    };
    private BluetoothAdapter mBTAdapter;

    public BluetoothSensor(Context context) {
//        super(context, -1, SensorEnum.Bluetooth.name());
        super(context, -1, "Bluetooth");
        this.context = context;
        deviceList = new ArrayList<>();
    }

    @Override
    protected void stopSensor() {
        mBTAdapter.cancelDiscovery();
        context.unregisterReceiver(mBTScanReceiver);
        mainAct.setWriting(false);
    }

    @Override
    public void generateData(String rnd_id) {
        if (!mBTAdapter.isEnabled())
            mainAct.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainAct.noBT();
                }
            });
        else {
            String data = "<measurements>";

            if (deviceList != null)
                for (BluetoothDevice i : deviceList) {
                    data += "<measurement>";
                    data += "<name>" + i.getName() + "</name>";
                    data += "<address>" + i.getAddress() + "</address>";
                    data += "<class>" + i.getBluetoothClass() + "</class>";
                    data += "<bondState>" + i.getBondState() + "</bondState>";
                    data += "<type>" + i.getType() + "</type>";
                    if (i.getUuids() != null)
                        for (ParcelUuid j : i.getUuids())
                            data += "<uuid" + j + ">" + j.toString() + "</uuid" + j + ">";
                    data += "</measurement>";
                }

            data += "</measurements>";

            db.writeToDB(mainAct.getLocation().name(), startTime, rnd_id, data);
        }
    }

    @Override
    protected void initSensor() {
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(mBTScanReceiver, filter);
        mBTAdapter.startDiscovery();
    }
}
