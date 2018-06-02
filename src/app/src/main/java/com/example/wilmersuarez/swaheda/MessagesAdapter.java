package com.example.wilmersuarez.swaheda;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesAdapter extends RecyclerView.Adapter {

    private List<Messages> userMessagesList;
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private Context vG;
    private String userName;

    MessagesAdapter(List<Messages> userMessagesList) {
        this.userMessagesList = userMessagesList;
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Messages messages = userMessagesList.get(position);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // If the current user is the sender of the message
        if(messages.getFrom().equals(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            // If some other user sent the message
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View V;
        vG = parent.getContext();
        // Inflate the appropriate layout according to the ViewType
        if(viewType == VIEW_TYPE_MESSAGE_SENT) {
            V = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_sent, parent, false);
            return new SentMessageViewHolder(V);
        } else if(viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            V = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_received, parent, false);
            return new ReceivedMessagesViewHolder(V);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // Passes the message object to a ViewHolder so that the contents can be bound to UI.
        Messages messages = userMessagesList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageViewHolder) holder).bind(messages);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessagesViewHolder) holder).bind(messages);
        }
    }

    private class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        ImageView messageImage;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            messageImage = itemView.findViewById(R.id.message_image_id);
        }

        void bind(Messages messages) {
            // Check if user is sending a Text Message or an Image Message
            String messageType = messages.getType();

            // If message is a text
            if(messageType.equals("text")) {
                // Make image invisible
                messageImage.setVisibility(View.GONE);

                messageText.setVisibility(View.VISIBLE);
                messageText.setText(messages.getMessage());

                // Format the current time
                timeText.setVisibility(View.VISIBLE);
                Date date = new Date(messages.getTime());
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss");
                String formattedDate = formatter.format(date);
                timeText.setText(formattedDate);

                // If message is an Image
            } else if(messageType.equals("image")) {
                // Make the message invisible
                messageText.setVisibility(View.GONE);
                timeText.setVisibility(View.GONE);

                messageImage.setVisibility(View.VISIBLE);
                Glide.with(vG).load(messages.getMessage()).into(messageImage);
            }
        }
    }

    private class ReceivedMessagesViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, nameText;
        CircleImageView userProfileImage;
        ImageView messageImage;

        ReceivedMessagesViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body_receiver);
            timeText = itemView.findViewById(R.id.text_message_time_receiver);
            nameText = itemView.findViewById(R.id.text_message_name_receiver);
            userProfileImage = itemView.findViewById(R.id.image_message_profile_receiver);
            messageImage = itemView.findViewById(R.id.message_image_id2);
        }

        void bind(Messages messages) {
            // Check if user is sending a Text Message or an Image Message
            String messageType = messages.getType();
            String senderId = messages.getFrom();
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(senderId);
            usersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Get user Name
                    userName = Objects.requireNonNull(dataSnapshot.child("user_name").getValue()).toString();
                    nameText.setText(userName);
                    // Get user
                    String userThumbImage = Objects.requireNonNull(dataSnapshot.child("user_thumb_image").getValue()).toString();

                    Picasso.get().load(userThumbImage)
                            .placeholder(R.drawable.default_profile_picture).into(userProfileImage);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            // If message is a text
            if(messageType.equals("text")) {
                // Make image invisible
                messageImage.setVisibility(View.GONE);

                messageText.setVisibility(View.VISIBLE);
                messageText.setText(messages.getMessage());

                nameText.setVisibility(View.VISIBLE);
                nameText.setText(userName);

                timeText.setVisibility(View.VISIBLE);
                // Format the current time
                Date date = new Date(messages.getTime());
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss");
                String formattedDate = formatter.format(date);
                timeText.setText(formattedDate);
            // If message is an Image
            } else {
                // Make the message invisible
                messageText.setVisibility(View.GONE);
                nameText.setVisibility(View.GONE);
                timeText.setVisibility(View.GONE);

                messageImage.setVisibility(View.VISIBLE);
                Glide.with(vG).load(messages.getMessage()).into(messageImage);
            }
        }

    }
}
