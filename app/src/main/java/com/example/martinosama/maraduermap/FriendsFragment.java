package com.example.martinosama.maraduermap;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;


@SuppressLint("ValidFragment")
public class FriendsFragment extends Fragment implements FriendListAdapter.RecyclerViewAdapterOnClickHandler{

    private static final int PICK_IMAGE_REQUEST = 71;
    MapFragment mapFragment;
    MainActivity mainActivity;
    public FriendsFragment(MapFragment mapFragment,MainActivity mainActivity) {
        this.mapFragment = mapFragment;
        this.mainActivity = mainActivity;

    }
    private EditText friendSearchEditText;
    private TextView currentStudentName;
    private FloatingActionButton addButton;
    private FloatingActionButton friendRequestButton;
    private RecyclerView friendListRecyclerView;
    private Switch availableSwitch;

    private View view;
    private Dialog detailsDialog,settingDialog;
    private CircleImageView imageButton;

    private DatabaseReference studentsDatabase;
    private DatabaseReference friendsDatabase;
    private DatabaseReference removeFriendDatabase;
    private FirebaseStorage storage;
    private StorageReference storageReference;


    private LinkedHashMap<String,String> currentStudentFriendsListNamesWithIDs;
    private LinkedHashMap<String,String> currentStudentFriendsListNamesWithIDsForSearch;

    public static String currentStudentID;
    private LinkedHashMap<String ,String > studentsIDs;
    private ArrayList<Long> currentStudentFriendsListIDs;
    private String currentName;
    private boolean checkSwitch;
    private String friendLastKnownLocation;
    private Uri filePath;
    private   SharedPreferences sharedPreferences;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_friends,container,false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        currentStudentID = sharedPreferences.getString("USERNAME",null);
        if (currentStudentID!=null) {
            currentStudentFriendsListNamesWithIDsForSearch = new LinkedHashMap<>();
            currentStudentFriendsListNamesWithIDs = new LinkedHashMap<>();
            studentsIDs = new LinkedHashMap<String, String>();

            friendListRecyclerView = (RecyclerView) view.findViewById(R.id.allFriendsRecyclerView);
            friendSearchEditText = (EditText) view.findViewById(R.id.friendNameOrIdEditText);
            currentStudentName = (TextView)view.findViewById(R.id.currentStudentName);
            addButton = (FloatingActionButton) view.findViewById(R.id.addFriendsFloatingActionButton);
            friendRequestButton = (FloatingActionButton) view.findViewById(R.id.friendRequestsFloatingActionButton);
            imageButton = (CircleImageView) view.findViewById(R.id.settingBtn);

            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    settingDialog = new Dialog(getActivity());
                    settingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    settingDialog.setContentView(R.layout.setting_dialog);
                    availableSwitch = (Switch) settingDialog.findViewById(R.id.available);
                    TextView changePPTextView = (TextView) settingDialog.findViewById(R.id.changeProfileBtn);
                    final TextView button = (TextView) settingDialog.findViewById(R.id.logoutBtn);
                    TextView timeTableBtn = (TextView) settingDialog.findViewById(R.id.timeTableBtn);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            sharedPreferences.edit().clear().apply();
                            mainActivity.replaceToLoginFragment();
                            settingDialog.cancel();
                        }
                    });
                    changePPTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setType("image/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
                        }
                    });
                    timeTableBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(getActivity(),TimeTableActivity.class);
                            startActivity(i);
                            settingDialog.cancel();
                        }
                    });
                    settingDialog.show();
                    availableSwitch.setChecked(sharedPreferences.getBoolean("AVAILABILITY",false));
                    availableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                            sharedPreferences.edit().putBoolean("AVAILABILITY", isChecked).apply();
                            DatabaseReference availabilityDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo").child(currentStudentID).child("available");
                            availabilityDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    dataSnapshot.getRef().setValue(isChecked);
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    });
                }
            });
            friendListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();
            studentsDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo");
            friendsDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo").child(currentStudentID).child("friends");

            //DISPLAY Current User Friends
            studentsDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot dataSnapshot1) {
                    for (DataSnapshot data : dataSnapshot1.getChildren()) {
                        String studentName = (String)data.child("name").getValue();
                        String studentID = data.getKey();
                        studentsIDs.put(studentID,studentName);
                    }

                    currentName = studentsIDs.get(currentStudentID);

                    friendsDatabase.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            currentStudentFriendsListNamesWithIDs.clear();
                            currentStudentFriendsListIDs = (ArrayList<Long>) dataSnapshot.getValue();
                            for (int i = 0; i < currentStudentFriendsListIDs.size(); i++) {
                                String id = String.valueOf(currentStudentFriendsListIDs.get(i));
                                if (!Objects.equals(id, "0"))
                                currentStudentFriendsListNamesWithIDs.put(id, studentsIDs.get(currentStudentFriendsListIDs.get(i).toString()));

                            }
                            currentStudentName.setText("Welcome, "+currentName);
                            friendListRecyclerView.setAdapter(new FriendListAdapter(getActivity(), currentStudentFriendsListNamesWithIDs, 0,"", FriendsFragment.this));
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
            //DISPLAY Current User Friends

            //SWIPE To Remove A Friend
            ItemTouchHelper itemTouchHelper =new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                    String friendId, friendName = null;
                    final String removedFriendID = viewHolder.itemView.getTag().toString();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    for (int x = 0; x < currentStudentFriendsListNamesWithIDs.size(); x++) {

                        friendId = (new ArrayList<String>(currentStudentFriendsListNamesWithIDs.keySet())).get(x);
                        friendName = (new ArrayList<String>(currentStudentFriendsListNamesWithIDs.values())).get(x);

                        if (friendId.equals(removedFriendID)) {
                            break;
                        }
                    }

                    builder.setTitle("Remove " + friendName + "?");

                    builder.setMessage("This will remove " + friendName + " from your friend list therefore last known location won't be available.")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    removeFriend(removedFriendID);
                                    dialog.cancel();

                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    friendListRecyclerView.setAdapter(new FriendListAdapter(getActivity(), currentStudentFriendsListNamesWithIDs, 0,"", FriendsFragment.this));
                                    dialog.cancel();
                                }
                            });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }

                @Override
                public boolean isItemViewSwipeEnabled() {
                    if(currentStudentFriendsListIDs.get(0) == 0)
                        return false;
                    return true;
                }
            });
            itemTouchHelper.attachToRecyclerView(friendListRecyclerView);
            //SWIPE To Remove A Friend


            //NAVIGATE To Add Friends Activity

            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent startFriendAddActivity = new Intent(getActivity(), FriendAddActivity.class);
                    startFriendAddActivity.putExtra("ID", currentStudentID);
                    startFriendAddActivity.putExtra("Friends", currentStudentFriendsListIDs);
                    startActivity(startFriendAddActivity);

                }
            });
            //NAVIGATE To Add Friends Activity

            //NAVIGATE To Friend Request Activity
            friendRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent startPendingActivity = new Intent(getActivity(), PendingActivity.class);
                    startActivity(startPendingActivity);

                }
            });
            //NAVIGATE To Friend Request Activity

            //SEARCH For A Friend
            friendSearchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    String userInput = charSequence.toString().toLowerCase();
                    String friendId, friendName;

                    currentStudentFriendsListNamesWithIDsForSearch.clear();
                    if (userInput.isEmpty()) {
                        friendListRecyclerView.setAdapter(new FriendListAdapter(getActivity(), currentStudentFriendsListNamesWithIDs, 0,"", FriendsFragment.this));
                        return;
                    }
                    for (int x = 0; x < currentStudentFriendsListNamesWithIDs.size(); x++) {

                        friendId = (new ArrayList<String>(currentStudentFriendsListNamesWithIDs.keySet())).get(x);
                        friendName = (new ArrayList<String>(currentStudentFriendsListNamesWithIDs.values())).get(x).toLowerCase();

                        if (friendId.startsWith(userInput) || friendName.startsWith(userInput)) {

                            currentStudentFriendsListNamesWithIDsForSearch.put((new ArrayList<String>(currentStudentFriendsListNamesWithIDs.keySet())).get(x), (new ArrayList<String>(currentStudentFriendsListNamesWithIDs.values())).get(x));
                        }
                    }
                    friendListRecyclerView.setAdapter(new FriendListAdapter(getActivity(), currentStudentFriendsListNamesWithIDsForSearch, 0,"", FriendsFragment.this));
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
            StorageReference ref = storageReference.child("images/"+ sharedPreferences.getString("USERNAME",null));
            GlideApp.with(this)
                    .load(ref)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(R.drawable.profile_pic)
                    .dontAnimate()
                    .into(imageButton);
        }
        return view;
    }

    private void removeFriend(final String  removedFriendID){

        friendsDatabase.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                ArrayList<Long> currentValue = (ArrayList<Long>) mutableData.getValue();

                currentValue.remove(Long.valueOf(removedFriendID));
                if (currentValue.size() == 0)
                    currentValue.add(0L);
                mutableData.setValue(currentValue);

                return Transaction.success(mutableData);
            }
            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {}
        });

        removeFriendDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo").child(removedFriendID).child("friends");

        removeFriendDatabase.runTransaction(new Transaction.Handler() {
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
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {}
        });
    }


    @Override
    public void onClickListener(final String friendID, final String friendName) {
        detailsDialog = new Dialog(getActivity());
        detailsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        detailsDialog.setContentView(R.layout.details_dialog);


        TextView nameTextView = (TextView) detailsDialog.findViewById(R.id.nameTextView);
        TextView idTextView = (TextView) detailsDialog.findViewById(R.id.idTextView);
        Button onMapBtn = (Button) detailsDialog.findViewById(R.id.onMapBtn);
        CircleImageView friendImage = (CircleImageView) detailsDialog.findViewById(R.id.friendImageView);
        StorageReference ref = storageReference.child("images/"+ friendID);
        GlideApp.with(this)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.d)
                .dontAnimate()
                .into(friendImage);

        final TextView lastKnownLocationTextView = (TextView)detailsDialog.findViewById(R.id.lastKnownLocationTextView);
        DatabaseReference lastKnownLocationDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo").child(friendID).child("lastKnownLocation");
        lastKnownLocationDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                friendLastKnownLocation = String.valueOf(dataSnapshot.getValue());
                lastKnownLocationTextView.setText(friendLastKnownLocation);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        nameTextView.setText(friendName);
        idTextView.setText(String.valueOf(friendID));

        onMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference availabilityDatabase = FirebaseDatabase.getInstance().getReference("StudentInfo").child(friendID).child("available");
                availabilityDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        checkSwitch = (boolean)dataSnapshot.getValue();

                        if (checkSwitch){
                            mapFragment.friendOnMap(friendLastKnownLocation,friendName,friendID);
                            detailsDialog.cancel();
                        }
                        else Toast.makeText(getActivity(), friendName+" disabled on Map", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
        detailsDialog.show();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null ){
            filePath = data.getData();
            try {
                final Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), filePath);
                if(filePath != null)
                {
                    final ProgressDialog progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setTitle("Uploading...");
                    progressDialog.show();
                    StorageReference ref = storageReference.child("images/"+ sharedPreferences.getString("USERNAME",null));
                    ref.putFile(filePath)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    progressDialog.dismiss();
                                    Toast.makeText(getActivity(), "Uploaded", Toast.LENGTH_SHORT).show();
                                    imageButton.setImageBitmap(bitmap);


                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(getActivity(), "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                    double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                            .getTotalByteCount());
                                    progressDialog.setMessage("Uploaded "+(int)progress+"%");
                                }
                            });
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}