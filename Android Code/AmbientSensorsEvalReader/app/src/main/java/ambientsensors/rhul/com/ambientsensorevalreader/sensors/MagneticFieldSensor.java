package ambientsensors.rhul.com.ambientsensorevalreader.sensors;

import android.content.Context;
import android.hardware.Sensor;

public class MagneticFieldSensor extends AbstractSensor {

    public MagneticFieldSensor(Context context) {
//        super(context, Sensor.TYPE_MAGNETIC_FIELD, SensorEnum.MagneticField.name());
        super(context, Sensor.TYPE_MAGNETIC_FIELD, "MagneticField");
    }
}
