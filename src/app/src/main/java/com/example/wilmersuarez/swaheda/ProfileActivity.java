package com.example.wilmersuarez.swaheda;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private Button sendRequestBtn, declineRequestBtn;
    private TextView userName;
    private ImageView userImage;

    private DatabaseReference friendRequestRef;
    private DatabaseReference friendsRef;
    String sender_user_id;
    String receiver_user_id;

    private String CURRENT_STATE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Database reference setup
        DatabaseReference userDataRef = FirebaseDatabase.getInstance().getReference().child("Users");
        friendRequestRef = FirebaseDatabase.getInstance().getReference().child("FriendRequests");
        friendRequestRef.keepSynced(true);
        friendsRef = FirebaseDatabase.getInstance().getReference().child("Friends");
        friendsRef.keepSynced(true);

        // Get current user ID
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        sender_user_id = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        // View setup
        sendRequestBtn = findViewById(R.id.sendRequestButton);
        declineRequestBtn = findViewById(R.id.declineRequestButton);
        userName = findViewById(R.id.profile_visit_user_name);
        userImage = findViewById(R.id.profile_visit_user_image);

        // Users current 'Friend' value
        CURRENT_STATE = "not_friends";

        // Get the users ID sent to the activity
        receiver_user_id = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("visit_user_id")).toString();

        // Get data from database and update the Layout with the data retrieved
        userDataRef.child(receiver_user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = Objects.requireNonNull(dataSnapshot.child("user_name").getValue()).toString();
                String image = Objects.requireNonNull(dataSnapshot.child("user_image").getValue()).toString();

                userName.setText(name);
                Picasso.get().load(image).placeholder(R.drawable.default_profile_picture).into(userImage);

                // Retrieve the friend request data (to avoid multiple requests)
                friendRequestRef.child(sender_user_id)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.hasChild(receiver_user_id)) {
                                        // Get the type of request
                                        String requestType = Objects.requireNonNull(dataSnapshot.child(receiver_user_id).child("request_type").getValue()).toString();
                                        if (requestType.equals("friend_request_sent")) {
                                            CURRENT_STATE = "request_sent";
                                            sendRequestBtn.setText(R.string.cancel_request_button_change);
                                            sendRequestBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.remove, 0, 0, 0);

                                            declineRequestBtn.setVisibility(View.INVISIBLE);
                                            declineRequestBtn.setEnabled(false);
                                        } else if (requestType.equals("friend_request_received")) {
                                            CURRENT_STATE = "request_received";
                                            sendRequestBtn.setText(R.string.accept_button_request_change);
                                            sendRequestBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.add, 0, 0, 0);

                                            // Decline friend request button is only visible to the user that received the friend request
                                            declineRequestBtn.setVisibility(View.VISIBLE);
                                            declineRequestBtn.setEnabled(true);

                                            // when the receiving user presses the decline friend request button
                                            declineRequestBtn.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    DeclineFriendRequest();
                                                }
                                            });
                                        }
                                    } else {
                                        friendsRef.child(sender_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if(dataSnapshot.hasChild(receiver_user_id)) {
                                                    CURRENT_STATE = "friends";
                                                    sendRequestBtn.setText(R.string.unfriend_button_change);
                                                    sendRequestBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.button, 0, 0, 0);

                                                    declineRequestBtn.setVisibility(View.INVISIBLE);
                                                    declineRequestBtn.setEnabled(false);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Decline button is initially invisible and disable
        declineRequestBtn.setVisibility(View.INVISIBLE);
        declineRequestBtn.setEnabled(false);

        // Check if the receiver user and send user are not equal
        if(!sender_user_id.equals(receiver_user_id)) {
            sendRequestBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Initially Disable button
                    sendRequestBtn.setEnabled(false);
                    // If the two users are not friends, allow send friend request
                    switch (CURRENT_STATE) {
                        case "not_friends":
                            SendFriendReqeust();

                            // If the request was already sent, allow cancellation of request
                            break;
                        case "request_sent":
                            CancelFriendRequst();

                            // If the was received, Allow user to accept request
                            break;
                        case "request_received":
                            AcceptFriendRequest();

                            // If two users are friend, Allow un-friend
                            break;
                        case "friends":
                            Unfriend();
                            break;
                    }
                }
            });
        } else {
            // Make buttons invisible
            sendRequestBtn.setVisibility(View.INVISIBLE);
            declineRequestBtn.setVisibility(View.INVISIBLE);
        }
    }

    // Decline the friend request
    private void DeclineFriendRequest() {
        // Remove request of sender
        friendRequestRef.child(sender_user_id).child(receiver_user_id).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            // Remove request received by receiver
                            friendRequestRef.child(receiver_user_id).child(sender_user_id).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()) {
                                                sendRequestBtn.setEnabled(true);
                                                CURRENT_STATE = "not_friends";
                                                sendRequestBtn.setText(R.string.send_request_button_change);
                                                sendRequestBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.add, 0, 0, 0);

                                                // Invisible after declining
                                                declineRequestBtn.setVisibility(View.INVISIBLE);
                                                declineRequestBtn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    // Un-friend a friend
    private void Unfriend() {
        // Remove friend nodes connecting the two users as friends
        friendsRef.child(sender_user_id).child(receiver_user_id).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            friendsRef.child(receiver_user_id).child(sender_user_id).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()) {
                                                sendRequestBtn.setEnabled(true);
                                                CURRENT_STATE = "not_friends";
                                                sendRequestBtn.setText(R.string.send_request_button_change);
                                                sendRequestBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.add, 0, 0, 0);

                                                declineRequestBtn.setVisibility(View.INVISIBLE);
                                                declineRequestBtn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    // Accept Friend Request
    private void AcceptFriendRequest() {
        // Save the date the two users became friends
        Calendar date = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentDate = new SimpleDateFormat("MM dd, yyyy");
        final String formattedDate = currentDate.format(date.getTime());

        friendsRef.child(sender_user_id).child(receiver_user_id).child("date").setValue(formattedDate)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        friendsRef.child(receiver_user_id).child(sender_user_id).child("date").setValue(formattedDate)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Remove request of sender once they become friends
                                        friendRequestRef.child(sender_user_id).child(receiver_user_id).removeValue()
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if(task.isSuccessful()) {
                                                            // Remove request received by receiver once they become friends
                                                            friendRequestRef.child(receiver_user_id).child(sender_user_id).removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if(task.isSuccessful()) {
                                                                                // Re-enable button
                                                                                sendRequestBtn.setEnabled(true);
                                                                                CURRENT_STATE = "friends";  // Became frineds
                                                                                sendRequestBtn.setText(R.string.unfriend_btn_change);
                                                                                sendRequestBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.button, 0, 0, 0);

                                                                                declineRequestBtn.setVisibility(View.INVISIBLE);
                                                                                declineRequestBtn.setEnabled(false);
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    // Cancel the friend request
    private void CancelFriendRequst() {
        // Remove request of sender
        friendRequestRef.child(sender_user_id).child(receiver_user_id).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            // Remove request received by receiver
                            friendRequestRef.child(receiver_user_id).child(sender_user_id).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()) {
                                                // Re-enable button
                                                sendRequestBtn.setEnabled(true);
                                                CURRENT_STATE = "not_friends";
                                                sendRequestBtn.setText(R.string.send_request_btn_change);
                                                sendRequestBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.add, 0, 0, 0);

                                                declineRequestBtn.setVisibility(View.INVISIBLE);
                                                declineRequestBtn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    // Send friend request to the user selected
    private void SendFriendReqeust() {
        friendRequestRef.child(sender_user_id)
                .child(receiver_user_id)
                .child("request_type")
                .setValue("friend_request_sent")
        .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    // Tell the receiver that they received a friend request
                    friendRequestRef.child(receiver_user_id)
                            .child(sender_user_id)
                            .child("request_type")
                            .setValue("friend_request_received")
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {
                                // Re-enable Friend request button (request was sent)
                                sendRequestBtn.setEnabled(true);
                                CURRENT_STATE = "request_sent";
                                sendRequestBtn.setText(R.string.cancel_request_btn_change);
                                sendRequestBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.remove, 0, 0, 0);

                                declineRequestBtn.setVisibility(View.INVISIBLE);
                                declineRequestBtn.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });
    }
}
