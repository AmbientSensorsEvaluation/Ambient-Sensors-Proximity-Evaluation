package ambientsensors.rhul.com.ambientsensorevalcard.sensors;

import android.content.Context;
import android.hardware.Sensor;

/**
 * Sensor coordinates:
 * 0: x
 * 1: y
 * 2: z
 */
public class AccelerometerSensor extends AbstractSensor {

    public AccelerometerSensor(Context context) {
//        super(context, Sensor.TYPE_ACCELEROMETER, SensorEnum.Accelerometer.name());
        super(context, Sensor.TYPE_ACCELEROMETER, "Accelerometer");
    }
}
