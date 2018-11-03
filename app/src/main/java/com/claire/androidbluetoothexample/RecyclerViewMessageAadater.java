package com.claire.androidbluetoothexample;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class RecyclerViewMessageAadater {

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView mTextMessage;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextMessage = itemView.findViewById(R.id.textMessage);
        }
    }
}
