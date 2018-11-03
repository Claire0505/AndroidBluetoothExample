package com.claire.androidbluetoothexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

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

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private RecyclerViewMessageAdapter mAdapter;

    private List<ChatMessage> chatMessageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRecyclerView();
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
