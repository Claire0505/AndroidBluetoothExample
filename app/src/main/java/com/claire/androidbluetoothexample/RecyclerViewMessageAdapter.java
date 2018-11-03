package com.claire.androidbluetoothexample;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class RecyclerViewMessageAdapter extends RecyclerView.Adapter<RecyclerViewMessageAdapter.ViewHolder> {

    private List<Message> messageList;

    public static final int SENDER = 0;
    public static final int RECIPENT = 1;

    public RecyclerViewMessageAdapter(Context context, List<Message> messages){
        messageList = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == RECIPENT){
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item_purple, parent, false);
            return new ViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item_green, parent, false);
            return new ViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        viewHolder.mTextMessage.setText(messageList.get(position).getMessage());
        viewHolder.mTextMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                remove(position);
            }
        });

    }

    private void remove(int pos) {
        int position = pos;
        messageList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, messageList.size());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderName().equals("Me")){
            return SENDER;
        }else {
            return RECIPENT;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView mTextMessage;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextMessage = itemView.findViewById(R.id.textMessage);
        }
    }
}
