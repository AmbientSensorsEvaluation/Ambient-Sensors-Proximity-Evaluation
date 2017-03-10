package ambientsensors.rhul.com.ambientsensorevalcard.export;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class ExportData {

    public ExportData(String id) {
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                String DB_FILEPATH = "/data/ambientsensors.rhul.com.ambientsensorevalcard/databases/StoreData.db";
                File currentDB = new File(data, DB_FILEPATH);
                String BK_FILEPATH = "Card_" + id + ".db";
                File backupDB = new File(sd, BK_FILEPATH);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception ignored) {
        }
    }
}
