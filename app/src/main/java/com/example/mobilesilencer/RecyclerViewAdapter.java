package com.example.mobilesilencer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.myViewHolder>{
    MosqueInfo data[];

    public RecyclerViewAdapter(MosqueInfo[] data) {
        this.data = data;
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.singlerow, parent, false);
        return new myViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        holder.id.setText("Mosque ID: " + data[position].getId());
        holder.lat.setText("Latitude : " + data[position].getLatitude());
        holder.lng.setText("Longitude : " + data[position].getLongitude());
    }

    @Override
    public int getItemCount() {
        return data.length;
    }

    class myViewHolder extends RecyclerView.ViewHolder{

        TextView id, lat, lng;
        public myViewHolder(@NonNull View itemView) {
            super(itemView);

            id = itemView.findViewById(R.id.id);
            lat = itemView.findViewById(R.id.lat);
            lng = itemView.findViewById(R.id.lng);
        }
    }
}

