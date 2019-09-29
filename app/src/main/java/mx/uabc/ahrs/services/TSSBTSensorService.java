package mx.uabc.ahrs.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import mx.uabc.ahrs.SensorActivity;

public class TSSBTSensorService {

    // Debugging
    private static final String TAG = "BluetoothSensorService";
    private static final boolean D = true;

    UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private TSSBTSensorService.ConnectThread mConnectThread;
    private TSSBTSensorService.ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    /**
     * Constructor. Prepares a new SensorActivity session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public TSSBTSensorService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {

        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(SensorActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {

        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {

        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new TSSBTSensorService.ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }


        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new TSSBTSensorService.ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(SensorActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(SensorActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {

        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }


    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {

        setState(STATE_NONE);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(SensorActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(SensorActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {

        setState(STATE_NONE);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(SensorActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(SensorActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                TSSBTSensorService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (TSSBTSensorService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public boolean isStreaming;
        private ReentrantLock reentrantLock;
        private float[] lastPacket = new float[]{0, 0, 0, 1};
        private Vector<Byte> unparsedStreamData = new Vector<>();

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            reentrantLock = new ReentrantLock();
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
        }

        /**
         * Write to the connected OutStream.
         *
         * @param data The bytes to write
         */
        private void write(byte[] data) {
            byte[] msgBuffer = new byte[data.length + 2];
            System.arraycopy(data, 0, msgBuffer, 1, data.length);
            msgBuffer[0] = (byte) 0xf7;
            msgBuffer[data.length + 1] = createChecksum(data);
            try {
                mmOutStream.write(msgBuffer);
                mmOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        private byte createChecksum(byte[] data) {
            byte checksum = 0;

            for (byte datum : data) {
                checksum += datum % 256;
            }
            return checksum;
        }

        private void writeReturnHeader(byte[] data) {
            byte[] msgBuffer = new byte[data.length + 2];
            System.arraycopy(data, 0, msgBuffer, 1, data.length);
            msgBuffer[0] = (byte) 0xf9;
            msgBuffer[data.length + 1] = createChecksum(data);
            try {
                mmOutStream.write(msgBuffer);
                mmOutStream.flush();
            } catch (IOException ignored) {
            }
        }

        private byte[] read(int amt) {

            byte[] response = new byte[amt];
            int amt_read = 0;
            while (amt_read < amt) {
                try {
                    amt_read += mmInStream.read(response, amt_read, amt - amt_read);
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
            double length = Math.sqrt(orient[0] * orient[0] + orient[1] * orient[1]
                    + orient[2] * orient[2] + orient[3] * orient[3]);

            return Math.abs(1 - length) < 1f;
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
                while (mmInStream.available() != 0) {
                    mmInStream.skip(mmInStream.available());
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                return;
            }

            isStreaming = false;

            reentrantLock.unlock();
        }

        public float[] getFilteredTaredOrientationQuaternion() {

            reentrantLock.lock();

            if (isStreaming) {
                try {
                    if (unparsedStreamData.size() + mmInStream.available() < 18) {
                        return lastPacket;
                    }

                    byte[] response;
                    response = read(mmInStream.available());
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

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

    }

}
