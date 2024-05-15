package com.smd.shared;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ReminderDetailActivity extends AppCompatActivity {

    private EditText reminderTitleEdit, reminderDescriptionEdit;
    private static final int REQUEST_CODE_WRITE_CALENDAR = 1;
    private TextView selectedDateTimeText;
    private Button setDateButton, setTimeButton, saveReminderButton;
    private Calendar selectedCalendar = Calendar.getInstance();
    private DatabaseReference remindersRef;
    private String reminderId;
    private CoordinatorLayout coordinatorLayout;
    private ChipGroup usersChipGroup;
    private List<User> userList = new ArrayList<>();
    private DatabaseReference usersRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_detail);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);

        // Initialize views
        reminderTitleEdit = findViewById(R.id.reminder_title_edit);
        reminderDescriptionEdit = findViewById(R.id.reminder_description_edit);
        selectedDateTimeText = findViewById(R.id.selected_datetime_text);
        setDateButton = findViewById(R.id.set_date_button);
        setTimeButton = findViewById(R.id.set_time_button);
        saveReminderButton = findViewById(R.id.save_reminder_button);

        usersChipGroup = findViewById(R.id.usersChipGroup);
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Fetch users when activity starts
        fetchUsers();

        // Firebase initialization
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        remindersRef = FirebaseDatabase.getInstance().getReference("reminders").child(userId);

        // Get reminder ID if editing existing reminder
        reminderId = getIntent().getStringExtra("reminderId");



        // Set up button click listeners
        setDateButton.setOnClickListener(v -> showDatePicker());
        setTimeButton.setOnClickListener(v -> showTimePicker());
        saveReminderButton.setOnClickListener(v -> saveReminder());

        // Load reminder details if editing
        if (reminderId != null) {
            loadReminderDetails();
        }

        updateDateTimeText();
    }

    private void fetchUsers() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear(); // Clear the list before adding new users

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null && !user.getId().equals(currentUserId)) { // Exclude current user
                        userList.add(user);
                        addChipForUser(user);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error fetching users:", databaseError.toException());
                // Use a Toast to display errors to the user, as Snackbars require a CoordinatorLayout.
                Toast.makeText(ReminderDetailActivity.this, "Failed to fetch users", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void addChipForUser(User user) {
        Chip chip = new Chip(this);
        chip.setText(user.getEmail()); // Assuming you want to display the email in the chip
        chip.setCheckable(true);
        usersChipGroup.addView(chip);
    }

    private void loadReminderDetails() {
        remindersRef.child(reminderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Reminder reminder = dataSnapshot.getValue(Reminder.class);
                if (reminder != null) {
                    reminderTitleEdit.setText(reminder.getTitle());
                    reminderDescriptionEdit.setText(reminder.getDescription());
                    selectedCalendar.setTimeInMillis(reminder.getTimestamp());
                    updateDateTimeText();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error loading reminder details", databaseError.toException());
                Snackbar.make(coordinatorLayout, "Failed to load reminder details.", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeText();
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedCalendar.set(Calendar.MINUTE, minute);
                    updateDateTimeText();
                },
                selectedCalendar.get(Calendar.HOUR_OF_DAY),
                selectedCalendar.get(Calendar.MINUTE),
                false); // Set to true for 24-hour format
        timePickerDialog.show();
    }

    private void updateDateTimeText() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        selectedDateTimeText.setText(formatter.format(selectedCalendar.getTime()));
    }

    private void loadReminderDetails(String reminderId) {
        remindersRef.child(reminderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Reminder reminder = dataSnapshot.getValue(Reminder.class);
                if (reminder != null) {
                    reminderTitleEdit.setText(reminder.getTitle());
                    reminderDescriptionEdit.setText(reminder.getDescription());
                    selectedCalendar.setTimeInMillis(reminder.getTimestamp());
                    updateDateTimeText();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error loading reminder details", databaseError.toException());
                Toast.makeText(ReminderDetailActivity.this, "Failed to load reminder details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveReminder() {
        String title = reminderTitleEdit.getText().toString().trim();
        String description = reminderDescriptionEdit.getText().toString().trim();
        long timestamp = selectedCalendar.getTimeInMillis();

        if (TextUtils.isEmpty(title)) {
            reminderTitleEdit.setError("Please enter a title");
            return;
        }

        Toast.makeText(this, "Saving Reminder...", Toast.LENGTH_SHORT).show();

        Reminder reminder;

        if (reminderId == null) {
            reminderId = remindersRef.push().getKey();
            reminder = new Reminder(reminderId, title, description, timestamp);
        } else {
            reminder = new Reminder(reminderId, title, description, timestamp);
        }

        // Get selected user emails from the ChipGroup
        List<String> selectedEmails = new ArrayList<>();
        for (int i = 0; i < usersChipGroup.getChildCount(); i++) {
            View chipView = usersChipGroup.getChildAt(i);
            if (chipView instanceof Chip && ((Chip) chipView).isChecked()) {
                selectedEmails.add(((Chip) chipView).getText().toString());
            }
        }

        // Convert emails to user IDs and save reminders
        convertEmailsToUserIds(selectedEmails, userIds -> {
            // Save reminder for the current user
            remindersRef.child(reminderId).setValue(reminder)
                    .addOnSuccessListener(aVoid -> {
                        // Save reminder for selected users
                        for (String userId : userIds) {
                            if(userId == FirebaseAuth.getInstance().getCurrentUser().getUid()) continue;
                            remindersRef.getRoot().child("reminders").child(userId).child(reminderId).setValue(reminder);
                        }

                        Toast.makeText(ReminderDetailActivity.this, "Reminder saved!", Toast.LENGTH_SHORT).show();
                        addToCalendar(reminder);

                        Intent intent = new Intent(ReminderDetailActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Error saving reminder:", e);
                        Toast.makeText(ReminderDetailActivity.this, "Failed to save reminder", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void convertEmailsToUserIds(List<String> emails, OnUserIdsFetchedListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        List<String> userIds = new ArrayList<>();

        for (String email : emails) {
            usersRef.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String userId = snapshot.child("id").getValue(String.class);
                                if (userId != null) {
                                    userIds.add(userId);
                                }
                            }

                            // If all emails have been processed, call the listener
                            if (userIds.size() == emails.size()) {
                                listener.onUserIdsFetched(userIds);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Error converting emails to user IDs:", error.toException());
                            // Handle the error here
                        }
                    });
        }
    }


    interface OnUserIdsFetchedListener {
        void onUserIdsFetched(List<String> userIds);
    }


    private void addToCalendar(Reminder reminder) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_CALENDAR}, REQUEST_CODE_WRITE_CALENDAR);
            return; // Wait for permission result
        }

        ContentValues eventValues = new ContentValues();
        eventValues.put(CalendarContract.Events.CALENDAR_ID, 1); // Default calendar
        eventValues.put(CalendarContract.Events.TITLE, reminder.getTitle());
        eventValues.put(CalendarContract.Events.DESCRIPTION, reminder.getDescription());
        eventValues.put(CalendarContract.Events.DTSTART, reminder.getTimestamp());
        eventValues.put(CalendarContract.Events.DTEND, reminder.getTimestamp() + 60 * 60 * 1000); // 1 hour event
        eventValues.put(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().getTimeZone().getID());

        Uri uri = getContentResolver().insert(CalendarContract.Events.CONTENT_URI, eventValues);

        if (uri != null) {
            Toast.makeText(this, "Reminder added to calendar", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to add reminder to calendar", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_CALENDAR) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try adding to calendar again
                saveReminder();
            } else {
                Toast.makeText(this, "Calendar permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
