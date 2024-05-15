package com.smd.shared;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.smd.shared.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ReminderAdapter.OnReminderClickListener {

    private ActivityMainBinding binding;
    private RecyclerView reminderList;
    private ReminderAdapter reminderAdapter;
    private List<Reminder> reminderDataList = new ArrayList<>();
    private DatabaseReference remindersRef;
    private TextView noReminderText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        noReminderText = findViewById(R.id.no_reminder_text);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, ReminderDetailActivity.class)));

        reminderList = binding.contentMain.reminderList;
        reminderList.setLayoutManager(new LinearLayoutManager(this));
        reminderAdapter = new ReminderAdapter(reminderDataList, this);
        reminderList.setAdapter(reminderAdapter);

        // Check Authentication (Optionally, can be put in onStart as well)
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() == null) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish(); // Close MainActivity
            } else {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                remindersRef = FirebaseDatabase.getInstance().getReference("reminders").child(userId);
                fetchReminders();
            }
        });
    }

    private void addToCalendar(Reminder reminder) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_CALENDAR}, 1);
            return;
        }

        ContentValues eventValues = new ContentValues();
        eventValues.put(CalendarContract.Events.CALENDAR_ID, 1); // Default calendar
        eventValues.put(CalendarContract.Events.TITLE, reminder.getTitle());
        eventValues.put(CalendarContract.Events.DESCRIPTION, reminder.getDescription());
        eventValues.put(CalendarContract.Events.DTSTART, reminder.getTimestamp());
        eventValues.put(CalendarContract.Events.DTEND, reminder.getTimestamp() + 60 * 60 * 1000); // 1 hour
        eventValues.put(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().getTimeZone().getID());

        try {
            Uri uri = getContentResolver().insert(CalendarContract.Events.CONTENT_URI, eventValues);
            if (uri != null) {
                Toast.makeText(this, "Reminder added to calendar", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("AddToCalendar", "Failed to insert event into calendar");
                Toast.makeText(this, "Failed to add reminder to calendar", Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalArgumentException | SecurityException e) {
            Log.e("AddToCalendar", "Error adding reminder to calendar:", e);

            // Check if the error is due to permission denial
            if (e instanceof SecurityException) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_CALENDAR}, 1);
            } else {
                // Other errors (e.g., IllegalArgumentException)
                Toast.makeText(this, "Failed to add reminder to calendar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchReminders() {
        remindersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                reminderDataList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Reminder reminder = snapshot.getValue(Reminder.class);
                    if (reminder != null) {
                        reminder.setId(snapshot.getKey());

                        // Check if the reminder is already in the calendar
                        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                            if (!isReminderInCalendar(reminder)) {
                                addToCalendar(reminder);
                            }
                        } else {
                            // Request permission to read calendar events if not granted.
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_CALENDAR}, 1);
                        }

                        reminderDataList.add(reminder);
                    }
                }
                reminderAdapter.notifyDataSetChanged();
                updateEmptyListMessage();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Database error:", databaseError.toException());
            }
        });
    }

    private boolean isReminderInCalendar(Reminder reminder) {
        String[] projection = new String[]{CalendarContract.Events._ID, CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART};
        String selection = CalendarContract.Events.TITLE + " = ?";
        String[] selectionArgs = new String[]{reminder.getTitle()};

        try (Cursor cursor = getContentResolver().query(CalendarContract.Events.CONTENT_URI, projection, selection, selectionArgs, null)) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

// (addToCalendar method remains the same from the previous responses)

    // Add the method to handle permission requests.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch reminders again to add to the calendar
                fetchReminders();
            } else {
                // Permission denied, handle accordingly (e.g., show a message to the user)
            }
        }
    }

    private void updateEmptyListMessage() {
        noReminderText.setVisibility(reminderDataList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onReminderClick(Reminder reminder) {
        Intent intent = new Intent(this, ReminderDetailActivity.class);
        intent.putExtra("reminderId", reminder.getId());
        startActivity(intent);
    }

    @Override
    public void onReminderLongClick(Reminder reminder) {
        showDeleteConfirmationDialog(reminder);
    }

    private void showDeleteConfirmationDialog(Reminder reminder) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_reminder)
                .setMessage(R.string.delete_reminder_confirmation)  // Corrected line
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteReminder(reminder))
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void deleteReminder(Reminder reminder) {
        remindersRef.child(reminder.getId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    reminderDataList.remove(reminder);
                    reminderAdapter.notifyDataSetChanged();
                    updateEmptyListMessage();
                    Toast.makeText(this, R.string.reminder_deleted, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error deleting reminder:", e);
                    Toast.makeText(this, R.string.error_deleting_reminder, Toast.LENGTH_SHORT).show();
                });
    }

    // ... (in MainActivity.java)

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    // ... (in MainActivity.java)

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Handle the settings action (open settings activity, etc.)
            return true;
        } else if (id == R.id.action_sign_out) {
            signOut();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();

        // Optionally, clear any local app data or preferences here

        // Redirect to LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
        startActivity(intent);
        finish();
    }

}
