package ambientsensors.rhul.com.ambientsensorevalreader.sensors;

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

import ambientsensors.rhul.com.ambientsensorevalreader.MainActivity;
import ambientsensors.rhul.com.ambientsensorevalreader.database.DBmanagement;
import ambientsensors.rhul.com.ambientsensorevalreader.datageneration.DataGenerator;
import ambientsensors.rhul.com.ambientsensorevalreader.datageneration.SensorEventData;

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
    private long initTimer;
    private boolean finished = false;
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

//            Log.d("MIN DELAY", "" + sensor.getMinDelay());
//            Log.d("VENDOR", sensor.getVendor());
//            Log.d("VERSION", "" + sensor.getVersion());
        }

        db = new DBmanagement(context, tableName);
    }

    public void stopRecording() {
        stopSensor();
        mainAct.setWriting(false);
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

    public boolean isFinished() {
        return finished;
    }

    public void startRecording() {
        finished = false;

        if (isSupported()) {
            startTime = getDateTime();

            initSensor();
        } else {
            mainAct.sensorNotSupported();
        }

        long start = System.currentTimeMillis();

        while (start + 500 > System.currentTimeMillis()) {

        }
        stopRecording();

        finished = true;
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
            initTimer = 0;

            mySensorManager.registerListener(
                    sensorListener,
                    sensor,
                    10000);
            //SensorManager.SENSOR_DELAY_FASTEST);
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
