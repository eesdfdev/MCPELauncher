package com.mojang.minecraftpe;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import java.nio.ByteBuffer;

import java.text.DateFormat;

import java.util.*;

import android.app.Activity;
import android.app.NativeActivity;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.view.View;
import android.view.KeyCharacterMap;
import android.util.DisplayMetrics;
import android.widget.Toast;

import android.preference.*;

import net.zhuoweizhang.mcpelauncher.*;

import net.zhuoweizhang.pokerface.PokerFace;



public class MainActivity extends NativeActivity
{

	public static final int INPUT_STATUS_IN_PROGRESS = 0;

	public static final int INPUT_STATUS_OK = 1;

	public static final int INPUT_STATUS_CANCELLED = 2;

	public static final int DIALOG_CREATE_WORLD = 1;

	public static final int DIALOG_SETTINGS = 3;

	protected DisplayMetrics displayMetrics;

	protected TexturePack texturePack;

	protected Context minecraftApkContext;

	protected boolean fakePackage = false;

	private static final String MC_NATIVE_LIBRARY_DIR = "/data/data/com.mojang.minecraftpe/lib/";
	private static final String MC_NATIVE_LIBRARY_LOCATION = "/data/data/com.mojang.minecraftpe/lib/libminecraftpe.so";

	protected int inputStatus = INPUT_STATUS_IN_PROGRESS;

	public static ByteBuffer minecraftLibBuffer;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("oncreate");
		try {
			System.load(MC_NATIVE_LIBRARY_LOCATION);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "Can't load libminecraftpe.so from the original APK", Toast.LENGTH_LONG).show();
			finish();
		}

		nativeRegisterThis();

		displayMetrics = new DisplayMetrics();

		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

		setFakePackage(true);

		super.onCreate(savedInstanceState);

		setFakePackage(false);

		try {
			String filePath = getSharedPreferences(MainMenuOptionsActivity.PREFERENCES_NAME, 0).getString("texturePack", null);
			if (filePath != null) {
				File file = new File(filePath);
				System.out.println("File!! " + file);
				if (!file.exists()) {
					texturePack = null;
				} else {
					texturePack = new ZipTexturePack(file);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.texture_pack_unable_to_load, Toast.LENGTH_LONG).show();
		}
		
		try {
			if (this.getPackageName().equals("com.mojang.minecraftpe")) {
				minecraftApkContext = this;
			} else {
				minecraftApkContext = createPackageContext("com.mojang.minecraftpe", 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "Can't create package context for the original APK", Toast.LENGTH_LONG).show();
			finish();
		}

		try {
			applyPatches();
		} catch (Exception e) {
			e.printStackTrace();
		}
		//setContentView(R.layout.main);
	}

	public void onStart() {
		super.onStart();
	}

	private void setFakePackage(boolean enable) {
		fakePackage = enable;
	}

	@Override
	public PackageManager getPackageManager() {
		if (fakePackage) {
			return new RedirectPackageManager(super.getPackageManager(), MC_NATIVE_LIBRARY_DIR);
		}
		return super.getPackageManager();
	}
		

	public native void nativeRegisterThis();
	public native void nativeUnregisterThis();

	public void buyGame() {
	}

	public int checkLicense() {
		System.err.println("CheckLicense");
		return 0;
	}

	/** displays a dialog. Not called from UI thread. */
	public void displayDialog(int dialogId) {
		System.out.println("displayDialog: " + dialogId);
		inputStatus = INPUT_STATUS_CANCELLED;
		switch (dialogId) {
			case DIALOG_CREATE_WORLD:
				System.out.println("World creation");
				inputStatus = INPUT_STATUS_CANCELLED;
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(MainActivity.this, "Not supported :(", Toast.LENGTH_SHORT).show();
					}
				});
				break;
			case DIALOG_SETTINGS:
				System.out.println("Settings");
				Intent intent = new Intent(this, MainMenuOptionsActivity.class);
				inputStatus = INPUT_STATUS_OK;
				startActivityForResult(intent, 1234);
				break;
		}
	}

	/*protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == DIALOG_SETTINGS) {
			inputStatus = INPUT_STATUS_OK;
		}
	}*/

	/**
	 * @param time Unix timestamp
	 * @returns a formatted time value
	 */

	public String getDateString(int time) {
		System.out.println("getDateString: " + time);
		return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).format(new Date(((long) time) * 1000));
	}

	public byte[] getFileDataBytes(String name) {
		System.out.println("Get file data: " + name);
		try {
			InputStream is = getInputStreamForAsset(name);
			if (is == null) return null;
			// can't always find length - use the method from 
			// http://www.velocityreviews.com/forums/t136788-store-whole-inputstream-in-a-string.html
			// instead
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while(true) {
				int len = is.read(buffer);
				if(len < 0) {
					break;
				}
				bout.write(buffer, 0, len);
			}
			byte[] retval = bout.toByteArray();

			return retval;
		} catch (Exception e) {
			return null;
		}
	}

	private InputStream getInputStreamForAsset(String name) {
		InputStream is = null;
		try {
			if (texturePack == null) {
				is = minecraftApkContext.getAssets().open(name);
			} else {
				System.out.println("Trying to load  " +name + "from tp");
				is = texturePack.getInputStream(name);
				if (is == null) {
					System.out.println("Can't load " + name + " from tp");
					is = minecraftApkContext.getAssets().open(name);
				}
			}
			return is;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private long getSizeForAsset(String name) {
		long size = 0;
		try {
			if (texturePack == null) {
				return minecraftApkContext.getAssets().openFd(name).getLength();
			}
			size = texturePack.getSize(name);
			if (size == -1) {
				size = minecraftApkContext.getAssets().openFd(name).getLength();
			}
			return size;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	public int[] getImageData(String name) {
		System.out.println("Get image data: " + name);
		try {
			InputStream is = getInputStreamForAsset(name);
			if (is == null) return null;
			Bitmap bmp = BitmapFactory.decodeStream(is);
			int[] retval = new int[(bmp.getWidth() * bmp.getHeight()) + 2];
			retval[0] = bmp.getWidth();
			retval[1] = bmp.getHeight();
			bmp.getPixels(retval, 2, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
			is.close();

			return retval;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		/* format: width, height, each integer a pixel */
		/* 0 = white, full transparent */
	}

	public String[] getOptionStrings() {
		System.err.println("OptionStrings");
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		Map prefsMap = sharedPref.getAll();
		Set<Map.Entry> prefsSet = prefsMap.entrySet();
		String[] retval = new String[prefsSet.size() * 2];
		int i = 0;
		for (Map.Entry e: prefsSet) {
			retval[i] = (String) e.getKey();
			retval[i + 1] = e.getValue().toString();
			i+= 2;
		}
		System.out.println(Arrays.toString(retval));
		return retval;
	}

	public float getPixelsPerMillimeter() {
		System.out.println("Pixels per mm");
		return ((float) displayMetrics.densityDpi) / 25.4f ;
	}

	public String getPlatformStringVar(int a) {
		System.out.println("getPlatformStringVar: " +a);
		return "";
	}

	public int getScreenHeight() {
		System.out.println("height");
		return displayMetrics.heightPixels;
	}

	public int getScreenWidth() {
		System.out.println("width");
		return displayMetrics.widthPixels;
	}

	public int getUserInputStatus() {
		System.out.println("User input status: " + inputStatus);
		return inputStatus;
	}

	public String[] getUserInputString() {
		System.out.println("User input string");
		/* for the seed input: name, world type, seed */
		return new String[] {"elephant", "potato", "strawberry"};
	}

	public boolean hasBuyButtonWhenInvalidLicense() {
		return false;
	}

	/** Seems to be called whenever displayDialog is called. Not on UI thread. */
	public void initiateUserInput(int a) {
		System.out.println("initiateUserInput: " + a);
	}

	public boolean isNetworkEnabled(boolean a) {
		System.out.println("Network?:" + a);
		return true;
	}


	public boolean isTouchscreen() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("ctrl_usetouchscreen", true);
	}

	public void postScreenshotToFacebook(String name, int firstInt, int secondInt, int[] thatArray) {
	}

	public void quit() {
		finish();
	}

	public void setIsPowerVR(boolean powerVR) {
		System.out.println("PowerVR: " + powerVR);
	}

	public void tick() {
	}

	public void vibrate(int duration) {
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("zz_longvibration", false)) {
			duration *= 5;
		}
		((Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(duration);
	}

	public int getKeyFromKeyCode(int keyCode, int metaState, int deviceId) {
		KeyCharacterMap characterMap = KeyCharacterMap.load(deviceId);
		return characterMap.get(keyCode, metaState);
	}

	public static void saveScreenshot(String name, int firstInt, int secondInt, int[] thatArray) {
	}


	public void applyPatches() throws Exception {
		long pageSize = PokerFace.sysconf(PokerFace._SC_PAGESIZE);
		System.out.println(Long.toString(pageSize, 16));
		long minecraftLibLocation = findMinecraftLibLocation();
		long minecraftLibLength = findMinecraftLibLength();
		long mapPageLength = ((minecraftLibLength / pageSize) + 1) * pageSize;
		System.out.println("Calling mprotect with " + minecraftLibLocation + " and " + mapPageLength);
		int returnStatus = PokerFace.mprotect(minecraftLibLocation, mapPageLength, PokerFace.PROT_WRITE | PokerFace.PROT_READ | PokerFace.PROT_EXEC);
		System.out.println("mprotect result is " + returnStatus);
		if (returnStatus < 0) {
			System.out.println("Well, that sucks!");
			return;
		}
		ByteBuffer buffer = PokerFace.createDirectByteBuffer(minecraftLibLocation, minecraftLibLength);
		//findMinecraftLibLocation();
		System.out.println("Has the byte buffer: " + buffer);
		minecraftLibBuffer = buffer;
		buffer.position(0x1b6d50);//"v0.6.1" offset
		byte[] testBuffer = new byte[6];
		buffer.get(testBuffer);
		System.out.println("Before: " + Arrays.toString(testBuffer));
		if (testBuffer.equals(">9000!".getBytes())) {
			System.out.println("This lib has been patched already!!!");
			Toast.makeText(this, "Already patched!", Toast.LENGTH_LONG).show();
		}
		buffer.position(0x1b6d50);//"v0.6.1" offset
		buffer.put(">9000!".getBytes());
		buffer.position(0x1b6d50);//"v0.6.1" offset
		buffer.get(testBuffer);
		System.out.println("After " + Arrays.toString(testBuffer));
	}

	public static long findMinecraftLibLocation() throws Exception {
		Scanner scan = new Scanner(new File("/proc/self/maps"));
		long minecraftLocation = -1;
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			//System.out.println(line);
			String[] parts = line.split(" ");
			if (parts[parts.length - 1].indexOf("libminecraftpe.so") >= 0 && parts[1].indexOf("x") >= 0) {
				System.out.println("Found minecraft location");
				minecraftLocation = Long.parseLong(parts[0].substring(0, parts[0].indexOf("-")), 16);
				break;
			}
		}
		scan.close();
		return minecraftLocation;
	}

	public static long findMinecraftLibLength() throws Exception {
		return new File(MC_NATIVE_LIBRARY_LOCATION).length(); //TODO: don't hardcode the 0x1000 page for relocation .data.rel.ro.local
	}

}