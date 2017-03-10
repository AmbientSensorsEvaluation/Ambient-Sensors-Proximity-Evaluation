package ambientsensors.rhul.com.ambientsensorevalcard;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

import java.util.Arrays;
import java.util.List;

import ambientsensors.rhul.com.ambientsensorevalcard.enums.SensorEnum;
import ambientsensors.rhul.com.ambientsensorevalcard.sensors.AbstractSensor;
import ambientsensors.rhul.com.ambientsensorevalcard.sensors.GravitySensor;
import ambientsensors.rhul.com.ambientsensorevalcard.sensors.LightSensor;
import ambientsensors.rhul.com.ambientsensorevalcard.sensors.LinearAccelerationSensor;
import ambientsensors.rhul.com.ambientsensorevalcard.sensors.ProximitySensor;
import ambientsensors.rhul.com.ambientsensorevalcard.sensors.SoundSensor;

public class MyHostApduService extends HostApduService {

    private MainActivity mainAct;
    private AbstractSensor sensor;
    private boolean writeData;
    private String readerID;
    private boolean readerError = false;
    private String rnd_id;
    private boolean writing = false;
    private boolean failed = false;
    private boolean complete = false;
    private SensorEnum currentSensor;

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        if (selectAidApdu(apdu)) {
            return getWelcomeMessage();
        } else {
            if (!writing) {
                complete = false;
                writing = true;
                failed = false;
                String setting = new String(apdu);
                long start = takeAction(setting);
                writeRecord(start);
                return transactionCompleteMessage();
            } else {
                return ("Still in previous transaction!").getBytes();
            }
        }
    }

    private byte[] transactionCompleteMessage() {
        if (sensor != null && writeData && mainAct.isAssociatedReader(readerID) && !readerError) {
            return ("Transaction complete! [" + mainAct.dataForReader(currentSensor.name())
                    + ";" + rnd_id + "]").getBytes();
        }
        failed = true;
        mainAct.notifySuccess(false);
        return ("Transaction failed!").getBytes();
    }

    private void writeRecord(final long start) {
        new Thread() {
            @Override
            public void run() {
                while (start + 500 > System.currentTimeMillis()) {
                }
                sensor.stopRecording();
                if (!failed) {
                    if (sensor.isSupported()) {
                        sensor.generateData(rnd_id);
                        mainAct.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mainAct.setSuccessfulMeasurements(sensor.getActiveMeasurements());
                                mainAct.notifySuccess(true);
                            }
                        });
                    }
                    mainAct.sensorRoller();
                } else
                    failed = false;
                writing = false;
            }
        }.start();
        complete = true;
    }

    private long takeAction(String setting) {
        mainAct = MainActivity.getInstance();

        switch (currentSensor = mainAct.getSensor()) {
            case Light:
                sensor = new LightSensor(mainAct);
                break;
            case Gravity:
                sensor = new GravitySensor(mainAct);
                break;
            case Proximity:
                sensor = new ProximitySensor(mainAct);
                break;
//            case RotationVector:
//                sensor = new RotationVectorSensor(mainAct);
//                break;
//            case WiFi:
//                sensor = new WiFiSensor(mainAct);
//                break;
//            case Bluetooth:
//                sensor = new BluetoothSensor(mainAct);
//                break;
//            case Accelerometer:
//                sensor = new AccelerometerSensor(mainAct);
//                break;
//            case Gyroscope:
//                sensor = new GyroscopeSensor(mainAct);
//                break;
            case LinearAcceleration:
                sensor = new LinearAccelerationSensor(mainAct);
                break;
//            case Pressure:
//                sensor = new PressureSensor(mainAct);
//                break;
//            case MagneticField:
//                sensor = new MagneticFieldSensor(mainAct);
//                break;
//            case Humidity:
//                sensor = new HumiditySensor(mainAct);
//                break;
//            case Temperature:
//                sensor = new TemperatureSensor(mainAct);
//                break;
//            case GPS:
//                sensor = new LocationSensor(mainAct, LocationSensor.TYPE_GPS);
//                break;
            case Sound:
                sensor = new SoundSensor(mainAct);
                break;
//            case NetworkLocation:
//                sensor = new LocationSensor(mainAct, LocationSensor.TYPE_NETWORK);
//                break;
//            case GeomagneticRotationVector:
//                sensor = new GeomagneticRotationVectorSensor(mainAct);
//                break;
            default:
                break;
        }

        long start = sensor.startRecording();

        List<String> items = Arrays.asList(setting.split("\\s*;\\s*"));

        if (items.size() != 6) {
            readerError = true;
            mainAct.showReaderError(items.get(0));
            failed = true;
        } else {
            readerError = false;
            writeData = mainAct.setSettings(items.get(0).replaceAll("\\s", ""), items.get(1).replaceAll("\\s", ""), items.get(2));
            readerID = items.get(4);
            rnd_id = items.get(5);
        }

        return start;
    }

    private byte[] getWelcomeMessage() {
        return "Hello Reader!".getBytes();
    }

    private boolean selectAidApdu(byte[] apdu) {
        return apdu.length >= 2 && apdu[0] == (byte) 0 && apdu[1] == (byte) 0xa4;
    }

    @Override
    public void onDeactivated(int reason) {
        if (!complete) {
            failed = true;
            mainAct = MainActivity.getInstance();
            mainAct.warnConnectionLost();
        }
    }
}