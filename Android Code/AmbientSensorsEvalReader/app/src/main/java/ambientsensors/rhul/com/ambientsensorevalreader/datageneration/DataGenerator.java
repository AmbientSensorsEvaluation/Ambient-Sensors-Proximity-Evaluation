package ambientsensors.rhul.com.ambientsensorevalreader.datageneration;

import java.util.ArrayList;

public class DataGenerator {

    private ArrayList<SensorEventData> valueList;

    public DataGenerator(ArrayList<SensorEventData> valueList) {
        this.valueList = valueList;
    }

    public String getGeneratedData() {
        int cnt = 0;
        long initTime = 0;
        String data = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><measurements>";

        if (valueList.size() > 0)
            initTime = valueList.get(0).getTimestamp();

        for (int i = 0; i < valueList.size(); i++) {
            float[] vals = valueList.get(i).getValues();

            data += "<measurement>";
            data += "<id>" + cnt++ + "</id>";
            data += "<timestamp>" + ((valueList.get(i).getTimestamp() - initTime) / 1_000_000) + "</timestamp>";
            for (int j = 0; j < vals.length; j++)
                data += "<data" + j + ">" + vals[j] + "</data" + j + ">";
            data += "</measurement>";
        }

        data += "</measurements>";

        return data;
    }
}
