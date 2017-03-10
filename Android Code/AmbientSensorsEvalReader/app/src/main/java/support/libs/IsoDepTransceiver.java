package support.libs;

import android.nfc.tech.IsoDep;

import java.io.IOException;

public class IsoDepTransceiver implements Runnable {

    private static final byte[] CLA_INS_P1_P2 = {0x00, (byte) 0xA4, 0x04, 0x00};
    private static final byte[] AID_ANDROID = {(byte) 0xF0, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
    private IsoDep isoDep;
    private OnMessageReceived onMessageReceived;
    private String message;

    public IsoDepTransceiver(IsoDep isoDep, OnMessageReceived onMessageReceived, String message) {
        this.isoDep = isoDep;
        this.onMessageReceived = onMessageReceived;
        this.message = message;
    }

    private byte[] createSelectAidApdu(byte[] aid) {
        byte[] result = new byte[6 + aid.length];
        System.arraycopy(CLA_INS_P1_P2, 0, result, 0, CLA_INS_P1_P2.length);
        result[4] = (byte) aid.length;
        System.arraycopy(aid, 0, result, 5, aid.length);
        result[result.length - 1] = 0;
        return result;
    }

    @Override
    public void run() {
        try {
            isoDep.setTimeout(3000);
            isoDep.connect();
            byte[] response = isoDep.transceive(createSelectAidApdu(AID_ANDROID));
            if (isoDep.isConnected() && !Thread.interrupted()) {
                response = isoDep.transceive(message.getBytes());
                onMessageReceived.onMessage(response);
            }
            isoDep.close();
        } catch (IOException e) {
            onMessageReceived.onError(e);
        }
    }

    public interface OnMessageReceived {

        void onMessage(byte[] message);

        void onError(Exception exception);
    }
}