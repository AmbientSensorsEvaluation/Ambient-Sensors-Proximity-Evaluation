package ambientsensors.rhul.com.ambientsensorevalcard.sensors;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ambientsensors.rhul.com.ambientsensorevalcard.enums.SensorEnum;

public class SoundSensor extends AbstractSensor {

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_DEFAULT;
    private int cnt;
    private String mFileName = null;
    private AudioRecord recorder;
    private Thread th;
    private int BufferElements2Rec = 1024;
    private int BytesPerElement = 2;
    private boolean isRecording = false;

    public SoundSensor(Context context) {
        super(context, -1, SensorEnum.Sound.name());

        cnt = db.getLastEntryNumber();

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/Card" + cnt + ".pcm";
    }

    private void onRecord(boolean start) {
        if (start) {
            rec();
        } else {
            stopRec();
        }
    }

    private void rec() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        isRecording = true;

        th = new Thread(new Runnable() {
            @Override
            public void run() {
                recorder.startRecording();

                writeAudioDataToFile();
            }
        });

        th.start();
    }

    private void writeAudioDataToFile() {
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(mFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            recorder.read(sData, 0, BufferElements2Rec);
            try {
                byte bData[] = short2byte(sData);

                assert os != null;
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            assert os != null;
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private void stopRec() {
        if (null != recorder) {
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            th = null;
        }
    }

    @Override
    public void stopSensor() {
        onRecord(false);
    }

    @Override
    public void generateData(String rnd_id) {
        String data = "Card" + cnt + ".pcm";

        db.writeToDB(mainAct.getLocation().name(), startTime, rnd_id, data);
    }

    @Override
    public long startRecording() {
        onRecord(true);

        return 0;
    }
}
