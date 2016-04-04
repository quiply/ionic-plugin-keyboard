package io.ionic.keyboard;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;

// import additionally required classes
import android.view.Display;
import android.graphics.Point;
import android.os.Build;

public class IonicKeyboard extends CordovaPlugin {

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if ("close".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    //http://stackoverflow.com/a/7696791/1091751
                    InputMethodManager inputManager = (InputMethodManager) cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    View v = cordova.getActivity().getCurrentFocus();

                    if (v == null) {
                        callbackContext.error("No current focus");
                    } else {
                        inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        callbackContext.success(); // Thread-safe.
                    }
                }
            });
            return true;
        }
        if ("show".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    ((InputMethodManager) cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
                    callbackContext.success(); // Thread-safe.
                }
            });
            return true;
        }
        if ("init".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    //calculate density-independent pixels (dp)
                    //http://developer.android.com/guide/practices/screens_support.html
                    DisplayMetrics dm = new DisplayMetrics();
                    cordova.getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
                    final float density = dm.density;

                    //http://stackoverflow.com/a/4737265/1091751 detect if keyboard is showing
                    final View rootView = cordova.getActivity().getWindow().getDecorView().findViewById(android.R.id.content).getRootView();

                    OnGlobalLayoutListener list = new OnGlobalLayoutListener() {
                        // https://github.com/driftyco/ionic-plugin-keyboard/pull/188
//                        int initialHeightDiff = -1;
                        int previousHeightDiff = 0;
                        @Override
                        public void onGlobalLayout() {
                            Rect r = new Rect();
                            //r will be populated with the coordinates of your view that area still visible.
                            rootView.getWindowVisibleDisplayFrame(r);

                            // get status bar height in pixels
                            Rect rectgle= new Rect();
                            rootView.getWindowVisibleDisplayFrame(rectgle);
                            int pixelStatusBarHeight = (int)(rectgle.top / density);

                            PluginResult result;

                            // cache properties for later use
                            int rootViewHeight = rootView.getRootView().getHeight();
                            int resultBottom = r.bottom;
                            int resultTop = r.top;

                            // calculate screen height differently for android versions >= 21: Lollipop 5.x, Marshmallow 6.x
                            //http://stackoverflow.com/a/29257533/3642890 beware of nexus 5
                            int screenHeight;

                            if (Build.VERSION.SDK_INT >= 21) {
                                Display display = cordova.getActivity().getWindowManager().getDefaultDisplay();
                                Point size = new Point();
                                display.getSize(size);
                                screenHeight = size.y;
                            } else {
                                screenHeight = rootViewHeight;
                            }

                            int heightDiff = screenHeight - resultBottom;
                            int pixelHeightDiff = (int)(heightDiff / density);

//                            int qyHeightDifference = screenHeight - (resultBottom - resultTop);
//                            System.out.println("--- qyHeightDifference: " + qyHeightDifference);

                            // http://stackoverflow.com/a/29257533/3642890 get status bar height another way
//                            int resourceId = cordova.getActivity().getResources().getIdentifier("status_bar_height", "dimen", "android");
//                            int statusBarHeight2 = -1;
//                            if (resourceId > 0) {
//                                statusBarHeight2 = cordova.getActivity().getResources().getDimensionPixelSize(resourceId);
//                                qyHeightDifference -= statusBarHeight2;
//                            }

//                            System.out.println("--- screenHeight: " + screenHeight);
//                            System.out.println("--- rootViewHeight: " + rootViewHeight);
//                            System.out.println("--- resultBottom: " + resultBottom);
//                            System.out.println("--- resultTop: " + resultTop);
//                            System.out.println("--- heightDiff: " + heightDiff);
//                            System.out.println("--- density: " + density);
//                            System.out.println("--- pixelHeightDiff: " + pixelHeightDiff);
//                            System.out.println("--- previousHeightDiff: " + previousHeightDiff);
//                            System.out.println("--- qyHeightDifference: " + qyHeightDifference);
//                            System.out.println("--- pixelStatusBarHeight: " + pixelStatusBarHeight);

                            // https://github.com/driftyco/ionic-plugin-keyboard/pull/188
//                            if (initialHeightDiff != -1) {
//                                pixelHeightDiff = pixelHeightDiff - initialHeightDiff;
//                                System.out.println("--- set custom pixelHeightDiff: " + pixelHeightDiff);
//                                System.out.println("--- initialHeightDiff was: " + initialHeightDiff);
//                            }

                            if (pixelHeightDiff > 100 && pixelHeightDiff != previousHeightDiff) { // if more than 100 pixels, its probably a keyboard...
                                // String msg = "S" + Integer.toString(pixelHeightDiff);
                                // return status bar height in addition to keyboard height
                                String msg = "S" + Integer.toString(pixelHeightDiff) + ";" + Integer.toString(pixelStatusBarHeight);
                                result = new PluginResult(PluginResult.Status.OK, msg);
                                result.setKeepCallback(true);
                                callbackContext.sendPluginResult(result);
                            }
                            else if ( pixelHeightDiff != previousHeightDiff && ( previousHeightDiff - pixelHeightDiff ) > 100 ){
                                String msg = "H";
                                result = new PluginResult(PluginResult.Status.OK, msg);
                                result.setKeepCallback(true);
                                callbackContext.sendPluginResult(result);
                            }

                            // https://github.com/driftyco/ionic-plugin-keyboard/pull/188
//                            if (initialHeightDiff == -1) {
//                                initialHeightDiff = pixelHeightDiff;
//                                System.out.println("--- set new custom initialHeightDiff at end: " + initialHeightDiff);
//                            }

                            previousHeightDiff = pixelHeightDiff;
                        }
                    };

                    rootView.getViewTreeObserver().addOnGlobalLayoutListener(list);

                    PluginResult dataResult = new PluginResult(PluginResult.Status.OK);
                    dataResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(dataResult);
                }
            });
            return true;
        }
        return false;  // Returning false results in a "MethodNotFound" error.
    }
}
