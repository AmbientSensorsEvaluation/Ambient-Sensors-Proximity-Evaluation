package ambientsensors.rhul.com.ambientsensorevalcard.sensors;

import android.content.Context;
import android.hardware.Sensor;

public class LinearAccelerationSensor extends AbstractSensor {

    public LinearAccelerationSensor(Context context) {
//        super(context, Sensor.TYPE_LINEAR_ACCELERATION, SensorEnum.LinearAcceleration.name());
        super(context, Sensor.TYPE_LINEAR_ACCELERATION, "LinearAcceleration");
    }
}
