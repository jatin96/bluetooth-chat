package com.example.android.bluetoothchat;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.content.pm.PackageInfo;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.android.common.logger.Log;
import java.util.ArrayList;
import java.util.Locale;
//DroidRush project by Jatin and Manveer


 //This classd controls Bluetooth to communicate with other devices.
public class BluetoothChatFragment extends Fragment{
    private static final String TAG = "BluetoothChatFragment";
    // Intent request codes......
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int CHECK=11;
     private static final int REQUEST_CAPTURE = 12;
     private ImageButton camera;
    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
     private ImageView profilepic;
    private ImageButton mSendButton;
    private  ImageButton mButtonAudio;
     //Name of the conected device
    private String mConnectedDeviceName = null;

     //Array adapter for storing messages
    private ArrayAdapter<String> mConversationArrayAdapter;
     //String buffer for outgoing message
    private StringBuffer mOutStringBuffer;
     //Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Getting local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null)//means no BT on deivce
         {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
        if(!hasCamera())
            camera.setEnabled(false);
    }
     private boolean hasCamera(){
         return getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
     }
     private void launchCamera(View v)
     {
         try {
             Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
             startActivityForResult(i, REQUEST_CAPTURE);
         }
         catch (Exception e)
         {}
     }
    @Override
    public void onStart() {
        super.onStart();
        //if BT is not enabled, we are requesting to enable it
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // if BT is enabled ,do this
        } else if (mChatService == null) {
            setupChat();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (ImageButton) view.findViewById(R.id.button_send);
        mButtonAudio=(ImageButton) view.findViewById(R.id.voice);
        camera=(ImageButton)view.findViewById(R.id.photo);
        profilepic=(ImageView)view.findViewById(R.id.profilepic);
    }
    //back-end for chat
    private void setupChat() {
        Log.d(TAG, "setupChat()");
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(),R.layout.message);
        mConversationView.setAdapter(mConversationArrayAdapter);
        mOutEditText.setOnEditorActionListener(mWriteListener);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCamera(v);
            }
        });
        mButtonAudio.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Start Your GupShup!");
                    i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                    i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
                    startActivityForResult(i, CHECK);
                }
                catch(Exception e)//some debugging part left here,App crashes here sometimes !!
                {}
            }
        });
        // Initialize the send button
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
            }
        });

        //performing bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);
        // Initialize the Stringbuffer of java
        mOutStringBuffer = new StringBuffer("");
    }
    //making device discoverable
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    private void sendMessage(String message) {
        // Checking BT before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check if user sends an empty message
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };
    //Updates the status on the action bar.
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }
    // Updates the status on the action bar
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }
    // Handler to get information back from BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // stringBuffer for storage
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // making string from bytes
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // when message is typed from voice recognition
            case 11:
                 if(resultCode!=0) {
                     ArrayList<String> results;
                     results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                     mOutEditText.setText(results.get(0));
                 }
                break;
            case 12:
                if(resultCode!=0)
                {
                    Bundle extras=data.getExtras();
                    Bitmap photo=(Bitmap)extras.get("data");
                    profilepic.setImageBitmap(photo);
                }
                break;
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with request to connect to a device
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // request for enabling BT
                if (resultCode == Activity.RESULT_OK) {
                    // set up chat session after BT is enabled
                    setupChat();
                } else {
                    // if user did not enable BT
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }
    private void connectDevice(Intent data, boolean secure) {
        // to get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // to connect to the device
        mChatService.connect(device, secure);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // to see devices ans scan other available devices
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // to ensure that this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

}
