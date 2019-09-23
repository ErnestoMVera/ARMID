package mx.uabc.ahrs.data;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

public class TSSBTSensor {

    public boolean isStreaming;

    private BluetoothSocket btSocket;
    private OutputStream BTOutStream;
    private InputStream BTInStream;
    private ReentrantLock reentrantLock;

    private float[] lastPacket = new float[]{0, 0, 0, 1};

    private Vector<Byte> unparsedStreamData = new Vector<>();

    public TSSBTSensor(String macAddress) {

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        UUID MY_UUID =
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {
            //Get a reference to the remote device
            BluetoothDevice remote_device = mBluetoothAdapter.getRemoteDevice(macAddress);
            //Create a socket
            btSocket = remote_device.createRfcommSocketToServiceRecord(MY_UUID);
            //Stop discovery if it is enabled
            mBluetoothAdapter.cancelDiscovery();
            //Try to connect to the remote device.
            btSocket.connect();
            //Now lets create the in/out streams
            BTOutStream = btSocket.getOutputStream();
            BTInStream = btSocket.getInputStream();
            reentrantLock = new ReentrantLock();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte createChecksum(byte[] data) {
        byte checksum = 0;

        for (byte datum : data) {
            checksum += datum % 256;
        }
        return checksum;
    }

    private void write(byte[] data) {
        byte[] msgBuffer = new byte[data.length + 2];
        System.arraycopy(data, 0, msgBuffer, 1, data.length);
        msgBuffer[0] = (byte) 0xf7;
        msgBuffer[data.length + 1] = createChecksum(data);
        try {
            BTOutStream.write(msgBuffer);
            BTOutStream.flush();
        } catch (IOException ignored) {
        }
    }

    private void writeReturnHeader(byte[] data) {
        byte[] msgBuffer = new byte[data.length + 2];
        System.arraycopy(data, 0, msgBuffer, 1, data.length);
        msgBuffer[0] = (byte) 0xf9;
        msgBuffer[data.length + 1] = createChecksum(data);
        try {
            BTOutStream.write(msgBuffer);
            BTOutStream.flush();
        } catch (IOException ignored) {
        }
    }

    private byte[] read(int amt) {
        byte[] response = new byte[amt];
        int amt_read = 0;
        while (amt_read < amt) {
            try {
                amt_read += BTInStream.read(response, amt_read, amt - amt_read);
            } catch (IOException e) {

                Log.d("Sensor", "Exception in read: " + e.toString());
            }
        }
        return response;
    }

    private float[] binToFloat(byte[] b) {
        if (b.length % 4 != 0) {
            return new float[0];
        }
        float[] return_array = new float[b.length / 4];
        for (int i = 0; i < b.length; i += 4) {
            //We account for endieness here
            int asInt = (b[i + 3] & 0xFF)
                    | ((b[i + 2] & 0xFF) << 8)
                    | ((b[i + 1] & 0xFF) << 16)
                    | ((b[i] & 0xFF) << 24);

            return_array[i / 4] = Float.intBitsToFloat(asInt);
        }
        return return_array;
    }

    private boolean quaternionCheck(float[] orient) {
        if (orient.length != 4)
            return false;
        double length = Math.sqrt(orient[0] * orient[0] + orient[1] * orient[1] + orient[2] * orient[2] + orient[3] * orient[3]);

        return Math.abs(1 - length) < 1f;

    }

    public void setTareCurrentOrient() {
        if (isStreaming) {
            stopStreaming();

            reentrantLock.lock();
            byte[] send_data = new byte[]{(byte) 0x60};
            write(send_data);
            reentrantLock.unlock();

            startStreaming();
        } else {
            reentrantLock.lock();
            byte[] send_data = new byte[]{(byte) 0x60};
            write(send_data);
            reentrantLock.unlock();
        }
    }

    public void startStreaming() {
        reentrantLock.lock();

        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(0x48);
        byte[] header = b.array();
        byte[] send_data = new byte[]{(byte) 0xdd, header[0], header[1], header[2], header[3]};
        write(send_data);

        send_data = new byte[]{(byte) 0x50, (byte) 0, (byte) 255, (byte) 255, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255, (byte) 255};
        write(send_data);

        b.putInt(0, 1000);
        byte[] interval = b.array();
        b.putInt(0, 0xffffffff);
        byte[] duration = b.array();
        b.putInt(0, 0);
        byte[] delay = b.array();
        send_data = new byte[]{(byte) 0x52, interval[0], interval[1], interval[2], interval[3],
                duration[0], duration[1], duration[2], duration[3],
                delay[0], delay[1], delay[2], delay[3]};
        write(send_data);

        send_data = new byte[]{(byte) 0x55};
        writeReturnHeader(send_data);
        //read(2);
        isStreaming = true;
        reentrantLock.unlock();
    }

    public void stopStreaming() {
        reentrantLock.lock();
        byte[] send_data = new byte[]{(byte) 0x56};
        write(send_data);
        try {
            while (BTInStream.available() != 0) {
                BTInStream.skip(BTInStream.available());
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            return;
        }
        isStreaming = false;
        reentrantLock.unlock();
    }

    public void close() {
        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public float[] getFilteredTaredOrientationQuaternion() {

        reentrantLock.lock();

        if (isStreaming) {
            try {
                if (unparsedStreamData.size() + BTInStream.available() < 18) {
                    return lastPacket;
                }

                byte[] response;
                response = read(BTInStream.available());
                reentrantLock.unlock();

                for (byte b : response) {
                    unparsedStreamData.add(b);
                }

                int location = unparsedStreamData.size() - 18;
                while (location > 0) {
                    byte checksum = (byte) (unparsedStreamData.toArray())[location];
                    byte data_length = (byte) (unparsedStreamData.toArray())[location + 1];

                    if ((data_length & 255) == 16) {
                        byte result = 0;
                        byte[] quat = new byte[16];
                        for (int i = 0; i < 16; i++) {
                            quat[i] = (byte) (byte) (unparsedStreamData.toArray())[location + 2 + i];
                            result = (byte) (result + quat[i]);
                        }

                        if ((result & 255) == (checksum & 255)) {
                            float[] res = binToFloat(quat);
                            if (quaternionCheck(res)) {
                                unparsedStreamData.subList(0, location + 18).clear();
                                lastPacket = res;
                                return lastPacket;
                            }
                        }
                    }

                    location -= 1;
                }
                return lastPacket;
            } catch (Exception e) {
                return lastPacket;
            }
        }

        byte[] send_data = new byte[]{(byte) 0x06};
        write(send_data);
        byte[] response = read(16);
        reentrantLock.unlock();
        return binToFloat(response);
    }
}
