package ambientsensors.rhul.com.ambientsensorevalcard;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import ambientsensors.rhul.com.ambientsensorevalcard.database.DBmanagement;
import ambientsensors.rhul.com.ambientsensorevalcard.enums.LocationEnum;
import ambientsensors.rhul.com.ambientsensorevalcard.enums.SensorEnum;
import ambientsensors.rhul.com.ambientsensorevalcard.export.ExportData;

public class MainActivity extends AppCompatActivity {

    private static MainActivity sInstance;

    private final int TOTAL_MEASUREMENTS = 2000;
    private final String DEVICE_ID = "DeviceID";
    private final String ASSOCIATED_READER = "AssociatedReaderID";
    private SharedPreferences settings;
    private String deviceID;

    private Spinner sItems;
    private Spinner lItems;
    private TextView steadyInfo;
    private TextView sensorInfo;
    private TextView locationInfo;
    private TextView readerInfo;
    private TextView counter;
    private Switch steadySwitch;

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
                Snackbar.make(view, R.string.exporting, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                new ExportData(deviceID);
            }
        });

        PackageManager pm = this.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            Toast.makeText(this, R.string.no_nfc,
                    Toast.LENGTH_LONG).show();
        }

        genID();
        buildInterface();
    }

    /**
     * Dirty way to ask for permissions, but convenient since the devices will be controlled by us.
     * <p/>
     * NOTE: only permissions that are ranked as dangerous are requested. For more info (because
     * this may change in the future) check:
     * <p/>
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

    private void generateDeviceID() {
        SecureRandom random = new SecureRandom();
        this.deviceID = new BigInteger(130, random).toString(32);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(DEVICE_ID, deviceID);
        editor.apply();
    }

    public boolean isAssociatedReader(String id) {
        String associatedReader = settings.getString(ASSOCIATED_READER, null);

        if (associatedReader == null) {
            storeAssociatedReader(id);
            return true;
        }

        if (associatedReader.equals(id))
            return true;

        Snackbar.make(this.findViewById(android.R.id.content), R.string.wrong_reader, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        return false;
    }

    public int getTotalMeasurements() {
        return TOTAL_MEASUREMENTS;
    }

    private void storeAssociatedReader(String id) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ASSOCIATED_READER, id);
        editor.apply();
    }

    public SensorEnum getSensor() {
        return SensorEnum.valueOf(sItems.getSelectedItem().toString().replaceAll("\\s", ""));
    }

    public boolean setSettings(String se, String lo, String move) {
        String previousSensor = sItems.getSelectedItem().toString();
        String previousLocation = lItems.getSelectedItem().toString();
        boolean previousMoving = steadySwitch.isChecked();
        boolean moveChanged = false;

        if (move.equals("true")) {
            if (!previousMoving)
                moveChanged = true;
        } else {
            if (previousMoving)
                moveChanged = true;
        }

        if (!previousSensor.equals(se) || !previousLocation.equals(lo) || moveChanged) {
            applyNewSettings(se, lo, moveChanged);
            return false;
        }
        return true;
    }

    public LocationEnum getLocation() {
        return LocationEnum.valueOf(lItems.getSelectedItem().toString().replaceAll("\\s", ""));
    }

    protected void sensorRoller() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sItems.setSelection(getSensor().next().ordinal());
            }
        });
    }

    public void notifySuccess(boolean success) {
        if (success) {
            Snackbar.make(this.findViewById(android.R.id.content), R.string.success_save, Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
        } else
            Snackbar.make(this.findViewById(android.R.id.content), R.string.fail_save, Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
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

    public void setSuccessfulMeasurements(int no) {
        counter.setText(String.format(getString(R.string.completion), no, TOTAL_MEASUREMENTS));
    }

    public void setReaderID(String readerID) {
        readerInfo.setText(String.format(getString(R.string.reader_id), readerID.toUpperCase()));
    }

    private void applyNewSettings(final String se, final String lo, final boolean moveChanged) {
        sItems.setSelection(SensorEnum.valueOf(se).ordinal());
        lItems.setSelection(LocationEnum.valueOf(lo).ordinal());
        if (moveChanged)
            steadySwitch.setChecked(!steadySwitch.isChecked());
    }

    private void buildInterface() {
        steadySwitch = (Switch) findViewById(R.id.switch1);

        steadyInfo = (TextView) findViewById(R.id.steady_info);
        sensorInfo = (TextView) findViewById(R.id.sensor_info);
        locationInfo = (TextView) findViewById(R.id.location_info);
        readerInfo = (TextView) findViewById(R.id.reader_info);
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

    /**
     * Deletes the last recorded entry
     *
     * @param db the database
     */
    public void deleteLastEntry(DBmanagement db) {
        db.deleteEntry(db.getLastEntryNumber());
        this.setSuccessfulMeasurements(db.getTotalActiveEntries());
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
                    Toast.makeText(getApplicationContext(), R.string.delete_cancelled, Toast.LENGTH_LONG)
                            .show();
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

    public void warnConnectionLost() {
        Snackbar.make(this.findViewById(android.R.id.content), R.string.connection_lost, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    public String dataForReader(String sensor) {
        return deviceID + ";" + sensor + ";"
                + lItems.getSelectedItem().toString() + ";" + steadySwitch.isChecked();
    }

    public void showReaderError(String error) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(R.string.error_body + error)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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
