package ambientsensors.rhul.com.ambientsensorevalcard.sensors;

import android.content.Context;
import android.hardware.Sensor;

public class GravitySensor extends AbstractSensor {

    public GravitySensor(Context context) {
//        super(context, Sensor.TYPE_GRAVITY, SensorEnum.Gravity.name());
        super(context, Sensor.TYPE_GRAVITY, "Gravity");
    }
}
