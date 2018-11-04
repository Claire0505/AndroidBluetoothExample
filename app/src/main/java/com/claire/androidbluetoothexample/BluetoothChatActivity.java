package com.claire.androidbluetoothexample;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class BluetoothChatActivity extends AppCompatActivity {
    // ChatMessage types sent from the BluetoothChatService
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // key names receive from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private EditText mOutEditText;
    private Button mSendButton;
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing message
    private StringBuilder mOutStringBuffer;
    
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    
    public int counter = 0;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private RecyclerViewMessageAdapter mAdapter;

    private List<ChatMessage> chatMessageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRecyclerView();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // If the adapter is null, the Bluetooth is not supported
        if (mBluetoothAdapter == null){
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null)
                setupChat();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mChatService != null){
            if (mChatService.getState() == BluetoothChatService.STATE_NONE){
                mChatService.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null)
            mChatService.stop();
    }

    private void ensureDiscoverable(){
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void setupChat() {
        mOutEditText = findViewById(R.id.editText_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);
        mSendButton = findViewById(R.id.btn_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView textView = findViewById(R.id.editText_out);
                String message = textView.getText().toString();
                sendMessage(message);
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing message
        mOutStringBuffer = new StringBuilder("");
    }

    private void sendMessage(String message){
        // Check that we're actually connected before trying anything
        // 在嘗試任何事情之前檢查我們是否已實際連接
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED){
            Toast.makeText(this, "You are not connected to a device", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send (檢查是否確實要發送內容)
        if (message.length() > 0){
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                    // If the cation is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP ){
                        String message = textView.getText().toString();
                        sendMessage(message);
                    }
                    return true;
                }
            };

    // The Handler that gets information back from the BluetoothChatService
    // 處理程序從BluetoothChatService獲取信息
    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[])msg.obj;
                    // construct a string from the buffer (從緩衝區構造一個字符串)
                    String writeMessage = new String(writeBuf);
                    mAdapter.notifyDataSetChanged();;
                    chatMessageList.add(new ChatMessage(counter++, writeMessage, "Me"));
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer (從緩衝區中的有效字節構造一個字符串)
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mAdapter.notifyDataSetChanged();
                    chatMessageList.add(new ChatMessage(counter++, readMessage, mConnectedDeviceName));
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to " +
                                    mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case REQUEST_CONNECT_DEVICE:
                // when DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK){
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BluetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (requestCode == Activity.RESULT_OK){
                    // Bluetooth is now enable, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, "Bluetooth was not enabled. Leaving Bluetooth Chat",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    public void connect(View v){
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    public void discoverable(View v){
        ensureDiscoverable();
    }

    private void setRecyclerView() {
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new RecyclerViewMessageAdapter(getBaseContext(), chatMessageList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        testMessage();
    }

    private void testMessage() {
        chatMessageList.add(new ChatMessage(1,"Hello","Me"));
        chatMessageList.add(new ChatMessage(2,"Hello","Jacky"));
        chatMessageList.add(new ChatMessage(3,"This is an example about RecyclerView","Me"));
        chatMessageList.add(new ChatMessage(4,"Great news","Jacky"));

    }
}
