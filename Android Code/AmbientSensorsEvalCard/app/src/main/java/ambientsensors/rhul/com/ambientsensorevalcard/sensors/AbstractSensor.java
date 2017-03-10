package ambientsensors.rhul.com.ambientsensorevalcard.sensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ambientsensors.rhul.com.ambientsensorevalcard.MainActivity;
import ambientsensors.rhul.com.ambientsensorevalcard.database.DBmanagement;
import ambientsensors.rhul.com.ambientsensorevalcard.datageneration.DataGenerator;
import ambientsensors.rhul.com.ambientsensorevalcard.datageneration.SensorEventData;

public class AbstractSensor {

    protected DBmanagement db;
    protected String startTime;
    protected MainActivity mainAct;
    private ArrayList<SensorEventData> valueList;
    private final SensorEventListener sensorListener
            = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            valueList.add(new SensorEventData(event.values, event.timestamp));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    private boolean supported = true;
    private SensorManager mySensorManager;
    private Sensor sensor;

    public AbstractSensor(Context context, int type, String tableName) {
        mainAct = MainActivity.getInstance();

        if (type != -1) {
            isSensorSupported(type);
            valueList = new ArrayList<>();
            mySensorManager = (SensorManager) context.getSystemService(Activity.SENSOR_SERVICE);
            sensor = mySensorManager.getDefaultSensor(type);
        }

        db = new DBmanagement(context, tableName);
    }

    public void stopRecording() {
        stopSensor();
    }

    private boolean isSensorSupported(int type) {
        SensorManager mSensorManager = (SensorManager) mainAct.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s : deviceSensors) {
            if (s.getType() == type) {
                return true;
            }
        }
        supported = false;
        return false;
    }

    public boolean isSupported() {
        return supported;
    }

    public long startRecording() {
        if (isSupported()) {
            startTime = getDateTime();

            initSensor();
        } else {
            mainAct.sensorNotSupported();
        }

        return System.currentTimeMillis();
    }

    public void generateData(String rnd_id) {
        db.writeToDB(mainAct.getLocation().name(), startTime, rnd_id,
                new DataGenerator(valueList).getGeneratedData());
    }

    protected void stopSensor() {
        mySensorManager.unregisterListener(sensorListener);
    }

    protected void initSensor() {
        if (sensor != null) {

            boolean listener = mySensorManager.registerListener(
                    sensorListener,
                    sensor,
                    10000);
//                    SensorManager.SENSOR_DELAY_FASTEST);

            if (!listener) {
                mainAct.sensorNotSupported();
            }
        }
    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public int getActiveMeasurements() {
        return db.getTotalActiveEntriesInLocation(mainAct.getLocation().toString());
    }

    public boolean notExceedTotalMeasurements() {
        return getActiveMeasurements() + 1 <= mainAct.getTotalMeasurements();
    }
}
