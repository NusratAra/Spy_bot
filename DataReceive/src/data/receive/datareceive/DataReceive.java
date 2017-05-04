package data.receive.datareceive;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;



import java.io.IOException;








import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;







import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DataReceive extends Activity implements SurfaceHolder.Callback, Camera.PictureCallback{

	Button Button1;
	  Camera mCamera;
	    SurfaceView mPreview;
	
	
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";

	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;

	MyReceiver mUsbReceiver;
	
	
	class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
		
	}
	
	boolean isBusy=false;
	
	private void doCamera(final String dt) {
	 
		isBusy=true;
		runOnUiThread(new Thread() {
			public void run() {
				
				
				if(dt.contains("snap")){
					Toast aa =Toast.makeText(getApplicationContext(), dt , Toast.LENGTH_SHORT);
					aa.show();
					 Button1.performClick();
				}
			}
		});
		isBusy=false;
	}
	
	class Receiver extends Thread {
		byte[] buffer = new byte[100];
		public void run() {
			
			while(true) {
				try {
					int len= 	mInputStream.read(buffer);
					String dt = new String(buffer);
					
					if(!isBusy){
						doCamera(dt);
					}

					
				} catch (IOException e) {
					
					e.printStackTrace();
				}
				
				
			}
			
			
		}
		
	}
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		
		mUsbReceiver=new MyReceiver();
		
		registerReceiver(mUsbReceiver, filter);
		
		setContentView(R.layout.activity_data_receive);
		
		Button1= (Button) findViewById(R.id.button1);
	       mPreview = (SurfaceView)findViewById(R.id.preview);
	       
	       mPreview.getHolder().addCallback(this);
	        mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	        
	        mCamera = Camera.open();
		
		
	}

	int a=2;	
	public void func(View b) {
		try{
				mCamera.takePicture(null, null, this);
		}catch(Exception ex) {}
				
	}
	
		private void OutputStreamWrite(byte[] data) {
			// TODO Auto-generated method stub
			
		}
		

	public void onResume() {
		super.onResume();

		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
		mCamera.stopPreview();
	}

	@Override
	public void onDestroy() {
		//unregisterReceiver(mUsbReceiver);
		super.onDestroy();
		 mCamera.release();
	}

	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			
		} 
		
		
		Receiver trd = new Receiver();
		trd.start();
	}

	private void closeAccessory() {

		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}
	@Override
	public void onPictureTaken(byte[]  data, Camera camera) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("ss-mm-HH_dd-MM-yyyy");
		String currentDateandTime = sdf.format(new Date());
		
		String sd=""+Environment.getExternalStorageDirectory();
		String ss=sd + "/" + "spy/" + currentDateandTime + ".jpg";
		File file = new File(ss);
		
		
		try{
			FileOutputStream fos= new FileOutputStream(file);
			fos.write(data);
			fos.close();
		}catch(Exception x) {}
		
		camera.startPreview();
		
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        Camera.Size selected = sizes.get(0);
        params.setPreviewSize(selected.width,selected.height);
        mCamera.setParameters(params);

        mCamera.startPreview();
		
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
            mCamera.setPreviewDisplay(mPreview.getHolder());
        } catch (Exception e) {
            e.printStackTrace();
        }
		
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		 Log.i("PREVIEW","surfaceDestroyed");
		
	}


}
