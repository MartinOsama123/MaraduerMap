package com.example.martinosama.maraduermap;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import de.hdodenhof.circleimageview.CircleImageView;


public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendViewHolder> {

    private Context mContext;
    private int flag;
    private String currentStudentID;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private LinkedHashMap<String,String> currentStudentFriendsListNamesWithIDs;
    private LinkedHashMap<String,String> studentListNamesWithIDs;
    private LinkedHashMap<String,String> currentStudentFriendRequestsListNamesWithIDs;
    private LinkedHashMap<String,String> currentStudentPendingFriendsListNamesWithIDs;
    final private RecyclerViewAdapterOnClickHandler mRecyclerViewAdapterOnClickHandler;

    public interface RecyclerViewAdapterOnClickHandler{
        void onClickListener(String clickedItem, String name);
    }

    public FriendListAdapter(Context context, LinkedHashMap<String,String> list, int flag,String currentStudentID, RecyclerViewAdapterOnClickHandler recyclerViewAdapterOnClickHandler) {
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        this.mContext = context;
        if (mContext instanceof MainActivity) {
            this.currentStudentFriendsListNamesWithIDs = list;
        }
        else if (mContext instanceof FriendAddActivity) {
            this.studentListNamesWithIDs = list;
        }
        else if (mContext instanceof PendingActivity && flag == 2){
            this.flag = flag;
            this.currentStudentPendingFriendsListNamesWithIDs = list;
            this.currentStudentID = currentStudentID;
        }
        else if (mContext instanceof PendingActivity && flag == 1){
            this.flag = flag;
            this.currentStudentFriendRequestsListNamesWithIDs = list;
            this.currentStudentID = currentStudentID;
        }

        this.mRecyclerViewAdapterOnClickHandler = recyclerViewAdapterOnClickHandler;
    }

    @Override
    public FriendViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater;
        View view;
        if (mContext instanceof PendingActivity){
            inflater = LayoutInflater.from(mContext);
            view = inflater.inflate(R.layout.requests_list, parent, false);
        }else {
            inflater = LayoutInflater.from(mContext);
            view = inflater.inflate(R.layout.friend_list, parent, false);
        }
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final FriendViewHolder holder, final int position) {

        if(mContext instanceof MainActivity){

            if (currentStudentFriendsListNamesWithIDs.size()==0){
                holder.idTextView.setVisibility(View.GONE);
                holder.profilePic.setVisibility(View.GONE);
                holder.nameTextView.setText("No friends found.");

            }else {

                String friendName = (new ArrayList<>(currentStudentFriendsListNamesWithIDs.values())).get(position);
                String friendID = (new ArrayList<>(currentStudentFriendsListNamesWithIDs.keySet())).get(position);
                holder.idTextView.setVisibility(View.VISIBLE);
                holder.profilePic.setVisibility(View.VISIBLE);
                holder.nameTextView.setText(friendName);
                holder.idTextView.setText(friendID);
                holder.itemView.setTag(friendID);
                StorageReference ref = storageReference.child("images/"+ friendID);
                    GlideApp.with(mContext)
                            .load(ref)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .placeholder(R.drawable.d)
                            .dontAnimate()
                            .into(holder.profilePic);



            }
        } else if (mContext instanceof FriendAddActivity){

            if (studentListNamesWithIDs.size()==0){
                holder.profilePic.setVisibility(View.GONE);
                holder.nameTextView.setVisibility(View.GONE);
                holder.idTextView.setVisibility(View.GONE);

            }else {

                String studentName = (new ArrayList<>(studentListNamesWithIDs.values())).get(position);
                String studentID = (new ArrayList<>(studentListNamesWithIDs.keySet())).get(position);
                holder.profilePic.setVisibility(View.VISIBLE);
                holder.idTextView.setVisibility(View.VISIBLE);
                holder.nameTextView.setVisibility(View.VISIBLE);
                holder.nameTextView.setText(studentName);
                holder.idTextView.setText(studentID);
                holder.itemView.setTag(studentID);

            }
        }else if (mContext instanceof PendingActivity && flag == 1){
           if (currentStudentFriendRequestsListNamesWithIDs.size()==0){
               holder.studentNameTextView.setVisibility(View.VISIBLE);
               holder.rejectFloatingActionButton.setVisibility(View.GONE);
               holder.acceptFloatingActionButton.setVisibility(View.GONE);
               holder.studentIdTextView.setVisibility(View.GONE);
               holder.studentNameTextView.setText("No friend requests.");
           }
           else {
               holder.studentNameTextView.setVisibility(View.VISIBLE);
               holder.rejectFloatingActionButton.setVisibility(View.VISIBLE);
               holder.acceptFloatingActionButton.setVisibility(View.VISIBLE);
               holder.studentIdTextView.setVisibility(View.VISIBLE);
               holder.studentNameTextView.setText((new ArrayList<>(currentStudentFriendRequestsListNamesWithIDs.values())).get(position));
               holder.studentIdTextView.setText((new ArrayList<>(currentStudentFriendRequestsListNamesWithIDs.keySet())).get(position));
               holder.acceptFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View view) {
                       addFriend(position);
                   }
               });
               holder.rejectFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View view) {
                       Toast.makeText(mContext, "You Rejected "+holder.studentNameTextView.getText().toString()+" Request.", Toast.LENGTH_LONG).show();
                       cancelRequest(position, 3);
                   }
               });
           }

        }
        else if (mContext instanceof PendingActivity && flag == 2){
            if (currentStudentPendingFriendsListNamesWithIDs.size()==0){
                holder.studentNameTextView.setVisibility(View.VISIBLE);
                holder.rejectFloatingActionButton.setVisibility(View.GONE);
                holder.studentIdTextView.setVisibility(View.GONE);
                holder.studentNameTextView.setText("No pending requests.");
            }
            else {
                holder.studentNameTextView.setVisibility(View.VISIBLE);
                holder.rejectFloatingActionButton.setVisibility(View.VISIBLE);
                holder.studentIdTextView.setVisibility(View.VISIBLE);
                holder.studentNameTextView.setText((new ArrayList<>(currentStudentPendingFriendsListNamesWithIDs.values())).get(position));
                holder.studentIdTextView.setText((new ArrayList<>(currentStudentPendingFriendsListNamesWithIDs.keySet())).get(position));
                holder.rejectFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        cancelRequest(position, 2);
                    }
                });
            }
        }
    }


    @Override
    public int getItemCount() {
        if (mContext instanceof MainActivity) {
            if (currentStudentFriendsListNamesWithIDs.size() == 0) {
                return 1;
            }
            return currentStudentFriendsListNamesWithIDs.size();
        }
        else if (mContext instanceof FriendAddActivity){
            if (studentListNamesWithIDs.size() == 0)
                return 1;
            return studentListNamesWithIDs.size();
        }
        else if (mContext instanceof PendingActivity && flag==2){
            if (currentStudentPendingFriendsListNamesWithIDs.size() == 0)
                return 1;
            return currentStudentPendingFriendsListNamesWithIDs.size();
        }
        else if (mContext instanceof PendingActivity && flag==1){
            if (currentStudentFriendRequestsListNamesWithIDs.size() == 0)
                return 1;
            return currentStudentFriendRequestsListNamesWithIDs.size();
        }
        return 0;
    }

    class FriendViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView nameTextView,idTextView,studentNameTextView,studentIdTextView;
        FloatingActionButton acceptFloatingActionButton,rejectFloatingActionButton;
        CircleImageView profilePic;

        public FriendViewHolder(View itemView) {
            super(itemView);
            if (mContext instanceof PendingActivity){
                studentNameTextView = (TextView)itemView.findViewById(R.id.studentNameTextView);
                studentIdTextView = (TextView)itemView.findViewById(R.id.studentIdTextView);
                acceptFloatingActionButton = (FloatingActionButton)itemView.findViewById(R.id.acceptFloatingActionButton);
                rejectFloatingActionButton = (FloatingActionButton)itemView.findViewById(R.id.rejectFloatingActionButton);
            }else {
                profilePic = (CircleImageView)itemView.findViewById(R.id.profilePic);
                nameTextView = (TextView) itemView.findViewById(R.id.nameTextView);
                idTextView = (TextView) itemView.findViewById(R.id.idTextView);
            }
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            int clickedAdapter = getAdapterPosition();
            String clickedStudentID,clickedStudentName;

            if (mContext instanceof MainActivity) {

                if (currentStudentFriendsListNamesWithIDs.size() == 0) {return;}

                clickedStudentID = (new ArrayList<String>(currentStudentFriendsListNamesWithIDs.keySet())).get(clickedAdapter);
                clickedStudentName = (new ArrayList<String>(currentStudentFriendsListNamesWithIDs.values())).get(clickedAdapter);

                mRecyclerViewAdapterOnClickHandler.onClickListener(clickedStudentID,clickedStudentName);
            }
            else if (mContext instanceof FriendAddActivity){

                if (studentListNamesWithIDs.size() == 0) {return;}

                clickedStudentID = (new ArrayList<String>(studentListNamesWithIDs.keySet())).get(clickedAdapter);
                clickedStudentName = (new ArrayList<String>(studentListNamesWithIDs.values())).get(clickedAdapter);

                mRecyclerViewAdapterOnClickHandler.onClickListener(clickedStudentID,clickedStudentName);
            }

        }

    }

    private void cancelRequest(final int position, int x) {
        if (x==2) {
            final String canceldRequestID = (new ArrayList<>(currentStudentPendingFriendsListNamesWithIDs.keySet())).get(position);
            String canceledRequestStudentName = currentStudentPendingFriendsListNamesWithIDs.get(canceldRequestID);
            currentStudentPendingFriendsListNamesWithIDs.remove(canceldRequestID);
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("StudentInfo").child(currentStudentID).child("pendingfriends");
            databaseReference.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    ArrayList<Long> currentValue = (ArrayList<Long>) mutableData.getValue();

                    currentValue.remove(Long.valueOf(canceldRequestID));
                    if (currentValue.size() == 0)
                        currentValue.add(0L);
                    mutableData.setValue(currentValue);

                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                }
            });

            DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference("StudentInfo").child(canceldRequestID).child("friendrequests");

            databaseReference1.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    ArrayList<Long> currentValue = (ArrayList<Long>) mutableData.getValue();

                    currentValue.remove(Long.valueOf(currentStudentID));
                    if (currentValue.size() == 0)
                        currentValue.add(0L);
                    mutableData.setValue(currentValue);

                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                }
            });
            Toast.makeText(mContext, "You canceled "+canceledRequestStudentName+" Request.", Toast.LENGTH_LONG).show();
        }
        else {
            final String canceldRequestID = (new ArrayList<>(currentStudentFriendRequestsListNamesWithIDs.keySet())).get(position);
            currentStudentFriendRequestsListNamesWithIDs.remove(canceldRequestID);
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("StudentInfo").child(canceldRequestID).child("pendingfriends");
            databaseReference.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    ArrayList<Long> currentValue = (ArrayList<Long>) mutableData.getValue();

                    currentValue.remove(Long.valueOf(currentStudentID));
                    if (currentValue.size() == 0)
                        currentValue.add(0L);
                    mutableData.setValue(currentValue);

                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                }
            });

            DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference("StudentInfo").child(currentStudentID).child("friendrequests");

            databaseReference1.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    ArrayList<Long> currentValue = (ArrayList<Long>) mutableData.getValue();

                    currentValue.remove(Long.valueOf(canceldRequestID));
                    if (currentValue.size() == 0)
                        currentValue.add(0L);
                    mutableData.setValue(currentValue);

                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                }
            });
        }
    }

    private void addFriend(final int position) {

        final String acceptedRequestID = (new ArrayList<>(currentStudentFriendRequestsListNamesWithIDs.keySet())).get(position);
        String acceptedRequestStudentName = currentStudentFriendRequestsListNamesWithIDs.get(acceptedRequestID);
        cancelRequest(position,1);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("StudentInfo").child(currentStudentID).child("friends");

        databaseReference.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                ArrayList<Long> currentValue = (ArrayList<Long>) mutableData.getValue();
                if (currentValue.get(0) == 0) {
                    currentValue.clear();
                    currentValue.add(Long.valueOf(acceptedRequestID));
                } else {
                    currentValue.add(Long.valueOf(acceptedRequestID));
                }
                mutableData.setValue(currentValue);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });

        DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference("StudentInfo").child(acceptedRequestID);

        databaseReference1.child("friends").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                ArrayList<Long> currentValue = (ArrayList<Long>) mutableData.getValue();
                if (currentValue.get(0) == 0) {
                    currentValue.clear();
                    currentValue.add(Long.valueOf(currentStudentID));
                } else {
                    currentValue.add(Long.valueOf(currentStudentID));
                }
                mutableData.setValue(currentValue);
                return Transaction.success(mutableData);
            }
            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
            }
        });
        Toast.makeText(mContext, "You accepted "+acceptedRequestStudentName+".", Toast.LENGTH_SHORT).show();
    }
}