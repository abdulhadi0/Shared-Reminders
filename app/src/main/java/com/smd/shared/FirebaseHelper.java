package com.smd.shared;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static final String REMINDERS_PATH = "reminders";
    private static final String USERS_PATH = "users";
    private static final String USERNAME_PATH = "usernames";

    private static FirebaseDatabase database = FirebaseDatabase.getInstance();
    private static DatabaseReference remindersRef = database.getReference(REMINDERS_PATH);

    public static void getReminders(ValueEventListener listener) {
        remindersRef.addValueEventListener(listener);
    }

    public static void addReminder(Reminder reminder) {
        remindersRef.setValue(reminder);
    }

    public static void updateReminder(Reminder reminder) {
        remindersRef.child(reminder.getId()).setValue(reminder);
    }

    public static void deleteReminder(String reminderId) {
        remindersRef.child(reminderId).removeValue();
    }

    public static void isUsernameAvailable(String username, OnUsernameAvailabilityCheckListener listener) {
        DatabaseReference usernamesRef = database.getReference(USERNAME_PATH);
        usernamesRef.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isAvailable = !snapshot.exists();
                listener.onUsernameAvailabilityChecked(isAvailable);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking username availability: " + error.getMessage());
                listener.onUsernameAvailabilityChecked(false); // Assume not available in case of error
            }
        });
    }

    public static void registerUser(String username, String email, String password, OnRegistrationCompleteListener listener) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference usersRef = database.getReference(USERS_PATH).child(userId);

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("username", username);
                        userData.put("email", email);

                        usersRef.setValue(userData)
                                .addOnSuccessListener(aVoid -> {
                                    // Save username in the usernames node for uniqueness check
                                    database.getReference(USERNAME_PATH).child(username).setValue(true);
                                    listener.onRegistrationComplete(true);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error registering user: " + e.getMessage());
                                    listener.onRegistrationComplete(false);
                                });
                    } else {
                        Log.e(TAG, "Error registering user: " + task.getException().getMessage());
                        listener.onRegistrationComplete(false);
                    }
                });
    }

    public interface OnUsernameAvailabilityCheckListener {
        void onUsernameAvailabilityChecked(boolean isAvailable);
    }

    public interface OnRegistrationCompleteListener {
        void onRegistrationComplete(boolean success);
    }
}

