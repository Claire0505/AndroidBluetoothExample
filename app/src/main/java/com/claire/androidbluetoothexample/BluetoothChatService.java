package com.claire.androidbluetoothexample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


/**
   *BluetoothChatService負責設置和管理藍牙的所有工作
   *與其他設備的連接。 它有一個偵聽的線程
   *傳入連接，用於連接設備的線程，以及
   *連接時執行數據傳輸的線程。
  */

public class BluetoothChatService {

    //Name for the SDP record when creating server socket (創建服務器套接字時SDP記錄的名稱)
    private static final String NAME = "BluetoothChat";

    //Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state(當前連接狀態)
    public static final int STATE_NONE = 0; //還未做連線
    public static final int STATE_LISTEN = 1; //開啟準備偵聽傳入
    public static final int STATE_CONNECTING = 2; //現在啟動傳輸連接
    public static final int STATE_CONNECTED = 3; //現在連線到遠端設備

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
       mAdapter = BluetoothAdapter.getDefaultAdapter();
       mState = STATE_NONE;
       mHandler = handler;
    }

    /**
     * Set the current state of the chat connection 設置聊天連接的當前狀態
     *
     * @param state An integer defining the current connection state 定義當前連接狀態的整數
     */
    private synchronized void setState(int state){
        mState = state;
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
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
    public synchronized void start(){
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to listen on BluetoothServerSocket
        if (mAcceptThread == null){
           mAcceptThread = new AcceptThread();
           mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice connect
     */
    public synchronized  void connect(BluetoothDevice device){
        // Cancel any thread attempting to make a connection (取消任何嘗試建立連線的線程)
        if (mState == STATE_CONNECTING){
            if (mConnectThread != null){
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        // Cancel any thread currently running a connection (取消當前正在運行的線程)
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);

    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * (啟動 ConnectedThread 以開始管理藍牙連接
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel any currently running a connection (取消當前在正運行連接的任何線程)
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null){
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChatActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_CONNECTED);

    }
    /**
     * Stop all threads
     **/
    public synchronized void stop(){

        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null){
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);

    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner (以不同步的方式寫入)
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out){
        //Create temporary(臨時) object
        ConnectedThread r;
        // Synchronize(同步) a copy of the ConnectedThread
        synchronized (this){
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized (執行寫入不同步)
        r.write(out);
    }

    /**
     *  Indicate that the connection was Lost and notify the UI Activity
     *  (指示連接嘗試失敗並通知UI活動)
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChatActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     * (指示連接已丟失並通知UI活動)
     */
    private void connectionLost(){
        setState(STATE_LISTEN);
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChatActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled). 它會一直運行，直到接受連接或直到取消
     */
    private class AcceptThread extends Thread{
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }

        public void run(){
            setName("AcceptThread");
            BluetoothSocket socket = null;
            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED){
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted (同意連線)
                if (socket != null){
                    synchronized (BluetoothChatService.this){
                        switch (mState){
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation(情況) normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready(準備) or already(已經) connected. Terminate(終止) new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                }
            }
        }
        public void cancel(){
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread (BluetoothDevice device){
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run(){
            setName("ConnectThread");
            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();
            // Make a connection to the BluetoothSocket
            try {
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                e.printStackTrace();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    e.printStackTrace();
                }
                // Start the service over to restart listening mode
                // 啟動服務以重新啟動偵聽模式
                BluetoothChatService.this.start();
                return;
            }
            //Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this){
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice);

        }
        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. (此線程在與遠程設備連接期間運行)
     * It handles all incoming and outgoing transmissions. (處理所有傳入和傳出傳輸)
     */
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread (BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connected
            while (true){
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_READ, bytes, -1,buffer);
                } catch (IOException e) {
                    connectionLost();
                    break;
                }
            }
        }
        /**
         * Write to the connected OutStream.
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_WRITE,-1,-1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
