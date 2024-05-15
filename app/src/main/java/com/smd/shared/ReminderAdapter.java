package com.smd.shared;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private List<Reminder> reminderList;
    private final OnReminderClickListener onReminderClickListener;

    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder);
        void onReminderLongClick(Reminder reminder);
    }

    public ReminderAdapter(List<Reminder> reminderList, OnReminderClickListener listener) {
        this.reminderList = reminderList;
        this.onReminderClickListener = listener;
    }

    // ViewHolder Class
    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView reminderTitle;
        TextView reminderDateTime;
        TextView reminderDescription;

        ReminderViewHolder(View itemView) {
            super(itemView);
            reminderTitle = itemView.findViewById(R.id.reminder_title);
            reminderDateTime = itemView.findViewById(R.id.reminder_datetime);
            reminderDescription = itemView.findViewById(R.id.reminder_description);
        }
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.reminder_item, parent, false);
        return new ReminderViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);

        holder.reminderTitle.setText(reminder.getTitle());
        holder.reminderDateTime.setText(reminder.getFormattedDateTime());
        holder.reminderDescription.setText(reminder.getDescription());

        // Click Listeners
        holder.itemView.setOnClickListener(v -> {
            if (onReminderClickListener != null) {
                onReminderClickListener.onReminderClick(reminder);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onReminderClickListener != null) {
                onReminderClickListener.onReminderLongClick(reminder);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }
}
