package ambientsensors.rhul.com.ambientsensorevalcard.sensors;

import android.content.Context;
import android.hardware.Sensor;

public class PressureSensor extends AbstractSensor {

    public PressureSensor(Context context) {
//        super(context, Sensor.TYPE_PRESSURE, SensorEnum.Pressure.name());
        super(context, Sensor.TYPE_PRESSURE, "Pressure");
    }
}
