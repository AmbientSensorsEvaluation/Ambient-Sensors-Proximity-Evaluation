package ambientsensors.rhul.com.ambientsensorevalreader;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import ambientsensors.rhul.com.ambientsensorevalreader.database.DBmanagement;
import ambientsensors.rhul.com.ambientsensorevalreader.enums.LocationEnum;
import ambientsensors.rhul.com.ambientsensorevalreader.enums.SensorEnum;
import ambientsensors.rhul.com.ambientsensorevalreader.export.ExportData;
import ambientsensors.rhul.com.ambientsensorevalreader.sensors.AbstractSensor;
import ambientsensors.rhul.com.ambientsensorevalreader.sensors.GravitySensor;
import ambientsensors.rhul.com.ambientsensorevalreader.sensors.LightSensor;
import ambientsensors.rhul.com.ambientsensorevalreader.sensors.LinearAccelerationSensor;
import ambientsensors.rhul.com.ambientsensorevalreader.sensors.ProximitySensor;
import ambientsensors.rhul.com.ambientsensorevalreader.sensors.SoundSensor;
import support.libs.IsoDepTransceiver;

public class MainActivity extends AppCompatActivity implements IsoDepTransceiver.OnMessageReceived, NfcAdapter.ReaderCallback {

    private static MainActivity sInstance;

    private final String ASSOCIATED_CARD = "AssociatedCardID";

    private final int TOTAL_MEASUREMENTS = 2000;
    private final String DEVICE_ID = "DeviceID";
    private Spinner sItems;
    private Spinner lItems;
    private TextView steadyInfo;
    private TextView sensorInfo;
    private TextView locationInfo;
    private TextView cardInfo;
    private TextView counter;
    private Switch steadySwitch;
    private NfcAdapter nfcAdapter;
    private SharedPreferences settings;
    private int currentID;
    private String deviceID;
    private AbstractSensor sensor;

    private String rnd_id;

    private boolean writing = false;
    private boolean onMessage = false;

    public static MainActivity getInstance() {
        return sInstance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        sInstance = this;

        askPermissions();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.uploading, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                new ExportData(deviceID);
            }
        });

        PackageManager pm = this.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            Toast.makeText(this, R.string.no_nfc,
                    Toast.LENGTH_LONG).show();
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        genID();
        buildInterface();
    }

    /**
     * Dirty way to ask for permissions, but convenient since the devices will be controlled by us.
     * <p>
     * NOTE: only permissions that are ranked as dangerous are requested. For more info (because
     * this may change in the future) check:
     * <p>
     * http://developer.android.com/guide/topics/security/permissions.html#perm-groups
     */
    private void askPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    2);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    3);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    4);
        }
    }

    private void genID() {
        settings = getSharedPreferences("AppPreferences", 0);

        deviceID = settings.getString(DEVICE_ID, null);

        if (deviceID == null) {
            generateDeviceID();
        }
    }

    public void deleteLastEntry(DBmanagement db) {
        db.deleteEntry(db.getLastEntryNumber());
        this.setSuccessfulMeasurements(db.getTotalActiveEntries());
    }

    public int getTotalMeasurements() {
        return TOTAL_MEASUREMENTS;
    }

    public LocationEnum getLocation() {
        return LocationEnum.valueOf(lItems.getSelectedItem().toString().replaceAll("\\s", ""));
    }

    public void noBT() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.bt_off_head)
                .setMessage(R.string.bt_off_body)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void noGPS() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.no_gps_head)
                .setMessage(R.string.no_gps_body)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void noWiFi() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.wifi_off_head)
                .setMessage(R.string.wifi_off_body)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void storeAssociatedCard(String id) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ASSOCIATED_CARD, id);
        editor.apply();
    }

    public void notifySuccess(boolean success) {
        if (success)
            Snackbar.make(this.findViewById(android.R.id.content), R.string.success_save, Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
        else
            Snackbar.make(this.findViewById(android.R.id.content), R.string.fail_save, Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
    }

    public void setCardID(final String readerID) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cardInfo.setText(String.format(getString(R.string.card_id), readerID.toUpperCase()));
            }
        });
    }

    private void generateDeviceID() {
        SecureRandom random = new SecureRandom();
        this.deviceID = new BigInteger(130, random).toString(32);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(DEVICE_ID, deviceID);
        editor.apply();
    }

    public boolean isAssociatedCard(String id) {
        String associatedReader = settings.getString(ASSOCIATED_CARD, null);

        if (associatedReader == null) {
            storeAssociatedCard(id);
            return true;
        }

        if (associatedReader.equals(id))
            return true;

        Snackbar.make(this.findViewById(android.R.id.content), R.string.wrong_card, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        return false;
    }

    public SensorEnum getSensor() {
        return SensorEnum.valueOf(sItems.getSelectedItem().toString().replaceAll("\\s", ""));
    }

    public void setSuccessfulMeasurements(int no) {
        counter.setText(String.format(getString(R.string.completion), no, TOTAL_MEASUREMENTS));
    }

    private void buildInterface() {
        steadySwitch = (Switch) findViewById(R.id.switch1);

        steadyInfo = (TextView) findViewById(R.id.steady_info);
        sensorInfo = (TextView) findViewById(R.id.sensor_info);
        locationInfo = (TextView) findViewById(R.id.location_info);
        cardInfo = (TextView) findViewById(R.id.reader_info);
        counter = (TextView) findViewById(R.id.counter);
        TextView thisID = (TextView) findViewById(R.id.this_id);

        thisID.setText(String.format(getString(R.string.id), deviceID.toUpperCase()));

        buildSensorSpinner();
        buildLocationSpinner();

        updateInfo();

        steadySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateInfo();
            }
        });

        sItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                updateInfo();
                setSuccessfulMeasurements(new DBmanagement(getApplicationContext(), getSensor().name())
                        .getTotalActiveEntriesInLocation(lItems.getSelectedItem().toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }

        });

        lItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                updateInfo();
                setSuccessfulMeasurements(new DBmanagement(getApplicationContext(), getSensor().name())
                        .getTotalActiveEntriesInLocation(lItems.getSelectedItem().toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }

        });

        setSuccessfulMeasurements(new DBmanagement(getApplicationContext(), getSensor().name())
                .getTotalActiveEntriesInLocation(lItems.getSelectedItem().toString()));
    }

    private void updateInfo() {
        if (steadySwitch.isChecked())
            steadyInfo.setText(R.string.not_steady_info);
        else
            steadyInfo.setText(R.string.steady_info);

        sensorInfo.setText(String.format(getString(R.string.sensor_txt), sItems.getSelectedItem().toString()));
        locationInfo.setText(String.format(getString(R.string.location_txt), lItems.getSelectedItem().toString()));
    }

    private void buildSensorSpinner() {
        String[] stringArray = Arrays.toString(SensorEnum.values()).replaceAll("^.|.$", "").split(", ");

        sItems = (Spinner) findViewById(R.id.sensor_spinner);
        sItems.setAdapter(new MyCustomAdapter(this, R.layout.row, stringArray, true));
    }

    private void buildLocationSpinner() {
        String[] stringArray = Arrays.toString(LocationEnum.values()).replaceAll("^.|.$", "").split(", ");

        lItems = (Spinner) findViewById(R.id.location_spinner);
        lItems.setAdapter(new MyCustomAdapter(this, R.layout.row, stringArray, false));
    }


    @Override
    public void onResume() {
        super.onResume();
        nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null);
    }

    @Override
    public void onPause() {
        super.onPause();
        nfcAdapter.disableReaderMode(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.delete_previous) {
            final boolean[] undo_clicked = {false};

            final Snackbar snackbar_del = Snackbar.make(findViewById(android.R.id.content), R.string.delete_notif, Snackbar.LENGTH_LONG);
            snackbar_del.setAction("Undo", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    undo_clicked[0] = true;
                    snackbar_del.dismiss();
                    Toast.makeText(getApplicationContext(), R.string.delete_cancelled, Toast.LENGTH_LONG).show();
                }
            })
                    .setActionTextColor(Color.RED)
                    .getView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {

                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    if (!undo_clicked[0]) {
                        deleteLastEntry(new DBmanagement(sInstance, getSensor().name()));
                        Toast.makeText(getApplicationContext(), R.string.delete_success, Toast.LENGTH_LONG).show();
                    }
                }
            });
            snackbar_del.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        if (!writing && !onMessage) {
            IsoDep isoDep = IsoDep.get(tag);

            String msg;

            rnd_id = "";
            while (rnd_id.equals("")) {
                SecureRandom random = new SecureRandom();
                rnd_id = new BigInteger(32, random).toString(32);
            }

            msg = (sItems.getSelectedItem().toString().equals("") ? " " : sItems.getSelectedItem().toString())
                    + ";" + (lItems.getSelectedItem().toString().equals("") ? " " : lItems.getSelectedItem().toString())
                    + ";" + String.valueOf(steadySwitch.isChecked())
                    + ";" + currentID
                    + ";" + deviceID
                    + ";" + rnd_id;

            writing = true;

            IsoDepTransceiver transceiver = new IsoDepTransceiver(isoDep, this, msg);

            new Thread(transceiver).start();

            takeAction();
        }
    }

    private void errorDialogue() {
        Snackbar.make(this.findViewById(android.R.id.content), R.string.error_body, Snackbar.LENGTH_SHORT)
                .setAction("Action", null).show();
        new ExportData(deviceID);
    }

    public void setWriting(boolean writing) {
        this.writing = writing;
    }

    private void takeAction() {
        switch (this.getSensor()) {
            case Light:
                sensor = new LightSensor(this);
                break;
            case Gravity:
                sensor = new GravitySensor(this);
                break;
            case Proximity:
                sensor = new ProximitySensor(this);
                break;
//            case RotationVector:
//                sensor = new RotationVectorSensor(this);
//                break;
//            case WiFi:
//                sensor = new WiFiSensor(this);
//                break;
//            case Bluetooth:
//                sensor = new BluetoothSensor(this);
//                break;
//            case Accelerometer:
//                sensor = new AccelerometerSensor(this);
//                break;
//            case Gyroscope:
//                sensor = new GyroscopeSensor(this);
//                break;
            case LinearAcceleration:
                sensor = new LinearAccelerationSensor(this);
                break;
//            case Pressure:
//                sensor = new PressureSensor(this);
//                break;
//            case MagneticField:
//                sensor = new MagneticFieldSensor(this);
//                break;
//            case Humidity:
//                sensor = new HumiditySensor(this);
//                break;
//            case Temperature:
//                sensor = new TemperatureSensor(this);
//                break;
//            case GPS:
//                sensor = new LocationSensor(this, LocationSensor.TYPE_GPS);
//                break;
            case Sound:
                sensor = new SoundSensor(this);
                break;
//            case NetworkLocation:
//                sensor = new LocationSensor(this, LocationSensor.TYPE_NETWORK);
//                break;
//            case GeomagneticRotationVector:
//                sensor = new GeomagneticRotationVectorSensor(this);
//                break;
            default:
                break;
        }

        sensor.startRecording();
    }

    public void showCardError(String message) {
        Snackbar.make(this.findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
                .setAction("Action", null).show();
        new ExportData(deviceID);
    }

    @Override
    public void onMessage(final byte[] message) {
        onMessage = true;

        String msg = new String(message);

        if (msg.equals("Tag was lost.")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    errorDialogue();
                }
            });
        } else if (msg.startsWith("Transaction complete!")) {
            String settings = msg.substring(msg.indexOf("[") + 1, msg.indexOf("]"));

            final List<String> items = Arrays.asList(settings.split("\\s*;\\s*"));

            if (items.size() == 5 && isAssociatedCard(items.get(0))
                    && items.get(1).equals(getSensor().name())
                    && items.get(2).equals(getLocation().name())
                    && items.get(3).equals(String.valueOf(steadySwitch.isChecked()))
                    && items.get(4).equals(rnd_id)) {

                while (!sensor.isFinished()) {

                }

                if (sensor.isSupported()) {
                    sensor.generateData(rnd_id);
                    notifySuccess(true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setSuccessfulMeasurements(new DBmanagement(getApplicationContext(), getSensor().name())
                                    .getTotalActiveEntriesInLocation(getLocation().name()));
                        }
                    });
                }
                setCardID(items.get(0));
                playSuccessSound();
                sensorRoller();
            } else {
                showCardError("Received wrong arguments from card!");
            }
        } else if (msg.equals("Transaction failed!")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showCardError(getString(R.string.card_transaction_failed));
                }
            });
        } else if (msg.equals("Still in previous transaction!")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showCardError(getString(R.string.card_in_prev_transaction));
                }
            });
        } else {
            final String mes = getString(R.string.card_problem_general) + msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showCardError(mes);
                }
            });
        }

        onMessage = false;
    }

    private void sensorRoller() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sItems.setSelection(getSensor().next().ordinal());
            }
        });
    }

    private void playSuccessSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(final Exception exception) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (exception != null)
                    onMessage(exception.getMessage().getBytes());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0)
            for (int i : grantResults) {
                if (i != PackageManager.PERMISSION_GRANTED) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.error)
                            .setMessage(R.string.no_permissions)
                            .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(false)
                            .show();
                }
            }
    }

    public void sensorNotSupported() {
        Snackbar.make(this.findViewById(android.R.id.content), R.string.not_supported, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    public class MyCustomAdapter extends ArrayAdapter<String> {

        private boolean image;
        private String[] names;

        public MyCustomAdapter(Context context, int textViewResourceId,
                               String[] objects, boolean image) {
            super(context, textViewResourceId, objects);
            this.image = image;
            this.names = objects;
        }

        @Override
        public View getDropDownView(int position, View convertView,
                                    ViewGroup parent) {
            return getCustomView(position, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, parent);
        }

        public View getCustomView(int position, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate(R.layout.row, parent, false);
            TextView label = (TextView) row.findViewById(R.id.item);
            label.setText(names[position]);

            ImageView icon = (ImageView) row.findViewById(R.id.icon);
            if (image)
                icon.setImageResource(android.R.drawable.presence_online);
            return row;
        }
    }
}
