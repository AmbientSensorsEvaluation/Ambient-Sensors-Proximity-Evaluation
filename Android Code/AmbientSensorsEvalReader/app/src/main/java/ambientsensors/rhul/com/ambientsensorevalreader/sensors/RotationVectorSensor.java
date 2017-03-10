package ambientsensors.rhul.com.ambientsensorevalreader.sensors;

import android.content.Context;
import android.hardware.Sensor;

/**
 * values[0]: x*sin(θ/2)
 * values[1]: y*sin(θ/2)
 * values[2]: z*sin(θ/2)
 * values[3]: cos(θ/2)
 * values[4]: estimated heading Accuracy (in radians) (-1 if unavailable)
 */
public class RotationVectorSensor extends AbstractSensor {

    public RotationVectorSensor(Context context) {
//        super(context, Sensor.TYPE_ROTATION_VECTOR, SensorEnum.RotationVector.name());
        super(context, Sensor.TYPE_ROTATION_VECTOR, "RotationVector");
    }
}
