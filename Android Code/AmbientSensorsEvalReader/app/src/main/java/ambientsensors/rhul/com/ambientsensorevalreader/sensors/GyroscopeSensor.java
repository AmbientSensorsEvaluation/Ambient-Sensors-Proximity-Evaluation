package ambientsensors.rhul.com.ambientsensorevalreader.sensors;

import android.content.Context;
import android.hardware.Sensor;

/**
 * values[0]: Angular speed around the x-axis
 * values[1]: Angular speed around the y-axis
 * values[2]: Angular speed around the z-axis
 */
public class GyroscopeSensor extends AbstractSensor {

    public GyroscopeSensor(Context context) {
//        super(context, Sensor.TYPE_GYROSCOPE, SensorEnum.Gyroscope.name());
        super(context, Sensor.TYPE_GYROSCOPE, "Gyroscope");
    }
}
