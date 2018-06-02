package com.example.wilmersuarez.swaheda;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String messageReceiverId;
    private String messageReceiverName;
    private TextView lastSeen;
    private CircleImageView chatProfileImage;
    private EditText inputMessage;
    private static final int GALLERY_REQUEST = 1;
    private DatabaseReference rootRef;
    private String senderId;
    private final List<Messages> messageList = new ArrayList<>();
    private MessagesAdapter messagesAdapter;
    private StorageReference imageStorageRef;
    private ProgressDialog loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Database reference
        rootRef = FirebaseDatabase.getInstance().getReference();

        // Get sender reference
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        senderId = mAuth.getCurrentUser().getUid(); // Get the id of the user sending the message

        // Get selected user's id and name passed into the Activity's intent
        messageReceiverId = getIntent().getExtras().get("visit_user_id").toString();
        messageReceiverName = getIntent().getExtras().get("user_name").toString();
        imageStorageRef = FirebaseStorage.getInstance().getReference().child("Message_Images");

        // Action bar setup
        Toolbar chatToolBar = findViewById(R.id.chat_upper_bar_id);
        loading = new ProgressDialog(this);

        setSupportActionBar(chatToolBar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        @SuppressLint("InflateParams") View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);

        // Link actionbar to custom view bar
        actionBar.setCustomView(actionBarView);

        // Action bar views setup
        TextView userName = findViewById(R.id.chat_bar_userName_id);
        lastSeen = findViewById(R.id.chat_bar_lastSeen_id);
        chatProfileImage = findViewById(R.id.chat_bar_userPicture_id);

        // Send Views setup
        ImageButton sendMessageBtn = findViewById(R.id.sendMessageBtn_id);
        ImageButton sendImageBtn = findViewById(R.id.sendImageBtn_id);
        inputMessage = findViewById(R.id.inputMessage_id);

        // Recycler view setup
        RecyclerView messagesListRV = findViewById(R.id.messagesList_RV_id);

        messagesAdapter = new MessagesAdapter(messageList);
        messagesListRV.setHasFixedSize(true);
        messagesListRV.setLayoutManager(new LinearLayoutManager(this));
        messagesListRV.setAdapter(messagesAdapter);

        FetchMessages();

        // Set bar 'title' to the username of the user receiving the message
        userName.setText(messageReceiverName);

        // Refer to the user that is receiving the message
        rootRef.child("Users").child(messageReceiverId).addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final String online = dataSnapshot.child("online").getValue().toString();
                final String profileThumbImage = dataSnapshot.child("user_thumb_image").getValue().toString();

                // Display last seen time
                // If online
                if(online.equals("true")) {
                    lastSeen.setText("Online");
                } else {
                    // Create instance of LastTimeSeen class to calculate time of users
                    new LastTimeSeen();
                    long last_seen = Long.parseLong(online);
                    String lastSeenDisplayTime = LastTimeSeen.getTimeAgo(last_seen);
                    lastSeen.setText(lastSeenDisplayTime);
                }

                // Set the profile image
                Picasso.get()
                        .load(profileThumbImage)
                        .networkPolicy(NetworkPolicy.OFFLINE)   // Allow offline picture loading
                        .placeholder(R.drawable.default_profile_picture)
                        .into(chatProfileImage, new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError(Exception e) {
                                // If images failed to load
                                Picasso.get()
                                        .load(profileThumbImage)
                                        .placeholder(R.drawable.default_profile_picture)
                                        .into(chatProfileImage);
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Listen for send message button
        sendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               SendMessage();
            }
        });

        sendImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= 23) {
                    if (ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        sendImage();
                    } else {
                        ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    }
                } else {
                    sendImage();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendImage();
            } else {
                Toast.makeText(this, "External write permission has not been granted.",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // Send another user an image
    private void sendImage() {
        Intent imageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imageIntent.setType("image/*");
        startActivityForResult(imageIntent, GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            loading.setTitle("Sending Image");
            loading.show();
            Uri mImageUri = data.getData();
            // Make references to the sender and receiver nodes in the Messages node (to send the image instead of a message)
            final String messageSenderRef = "Messages/" + senderId + "/" + messageReceiverId;
            final String messageReceiverRef = "Messages/" + messageReceiverId + "/" + senderId;

            DatabaseReference messageKey = rootRef.child("Messages").child(senderId)
                    .child(messageReceiverId).push();
            final String messagePushId = messageKey.getKey();

            // Store image to firebase storage folder "Images"
            StorageReference filePath = imageStorageRef.child(messagePushId + ".jpg");
            filePath.putFile(mImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful()) {
                        // Get Image Url from Firebase Storage
                        final String imageDownloadUrl= task.getResult().getDownloadUrl().toString();

                        // Image Message body
                        HashMap<String, Object> messageBodyText = new HashMap<>();
                        messageBodyText.put("message", imageDownloadUrl);   // Instead of text message, send image URL
                        messageBodyText.put("seen", false);
                        messageBodyText.put("type", "image");
                        messageBodyText.put("from", senderId);
                        messageBodyText.put("time", ServerValue.TIMESTAMP);
                        messageBodyText.put("ReceiverName", messageReceiverName);

                        // Image Message details for both receiver and sender
                        HashMap<String, Object> messageBodyDetails = new HashMap<>();
                        messageBodyDetails.put(messageSenderRef + "/" + messagePushId, messageBodyText);
                        messageBodyDetails.put(messageReceiverRef + "/" + messagePushId, messageBodyText);

                        rootRef.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null) {
                                    Log.d("Chat Log", databaseError.getMessage());
                                }

                                // Clear the edit text field after message was sent
                                inputMessage.setText(null);

                                loading.dismiss();
                            }
                        });

                        loading.dismiss();
                    } else {
                        Toast.makeText(ChatActivity.this, "Failed sending Image! Try again...", Toast.LENGTH_SHORT).show();
                        loading.dismiss();
                    }
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    loading.setMessage("Please wait... " + ((int) progress) + "%");
                }
            });
        }
    }

    // Get the messages
    private void FetchMessages() {
        rootRef.child("Messages").child(senderId).child(messageReceiverId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Messages messages = dataSnapshot.getValue(Messages.class);
                        messageList.add(messages);

                        messagesAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    // Send the message
    private void SendMessage() {
        // Get the message to send
        String message = inputMessage.getText().toString();

        // If the message is empty, do nothing
        if(TextUtils.isEmpty(message)) {
        } else {
            // Make references to the sender and receiver nodes in the Messages node.
            String messageSenderRef = "Messages/" + senderId + "/" + messageReceiverId;
            String messageReceiverRef = "Messages/" + messageReceiverId + "/" + senderId;

            // Make unique random key for each message
            DatabaseReference messageKey = rootRef.child("Messages").child(senderId)
                    .child(messageReceiverId).push();
            String messagePushId = messageKey.getKey();

            // Message body
            HashMap<String, Object> messageBodyText = new HashMap<>();
            messageBodyText.put("message", message);
            messageBodyText.put("seen", false);
            messageBodyText.put("type", "text");
            messageBodyText.put("from", senderId);
            messageBodyText.put("time", ServerValue.TIMESTAMP);
            messageBodyText.put("ReceiverName", messageReceiverName);

            // Message details for both receiver and sender
            HashMap<String, Object> messageBodyDetails = new HashMap<>();
            messageBodyDetails.put(messageSenderRef + "/" + messagePushId, messageBodyText);
            messageBodyDetails.put(messageReceiverRef + "/" + messagePushId, messageBodyText);

            rootRef.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    // If error occurred
                    if(databaseError != null) {
                        Log.d("Chat Log", databaseError.getMessage());
                    }

                    // Clear the edit text field after message was sent
                    inputMessage.setText(null);
                }
            });
        }
    }
}
