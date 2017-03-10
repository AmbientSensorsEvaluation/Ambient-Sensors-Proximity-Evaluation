package ambientsensors.rhul.com.ambientsensorevalcard.datageneration;

public class SensorEventData {

    private float[] values;
    private long timestamp;

    public SensorEventData(float[] values, long timestamp) {
        this.values = new float[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
        this.timestamp = timestamp;
    }

    public float[] getValues() {
        return values;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
