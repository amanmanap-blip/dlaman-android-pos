package com.dlaman.ordering;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.JavascriptInterface;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
  private static final String PREFS = "dlaman_standalone";
  private WebView web;
  private ProgressBar progress;
  private static final int EXPORT_REQUEST=817;
  private static final int IMPORT_REQUEST=818;
  private String pendingExport="";
  private static final UUID SPP_UUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  private final Object btLock=new Object();
  private String printerMac="";
  private boolean printerBusy=false;
  private static final int BT_PERMISSION=305;
  private boolean hasBluetoothPermission(){return Build.VERSION.SDK_INT<31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
  private void askBluetoothPermission(){if(Build.VERSION.SDK_INT>=31&&!hasBluetoothPermission())requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},BT_PERMISSION);}
  private void status(String msg){runOnUiThread(()->android.widget.Toast.makeText(this,msg,android.widget.Toast.LENGTH_LONG).show());}
  private void choosePrinter(){
    if(!hasBluetoothPermission()){askBluetoothPermission();status("Benarkan Nearby devices, kemudian tekan Bluetooth lagi.");return;}
    BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
    if(adapter==null){status("Tablet ini tiada Bluetooth.");return;}
    if(!adapter.isEnabled()){status("Aktifkan Bluetooth dan pair POS58B dalam Android Settings.");return;}
    Set<BluetoothDevice> devices=adapter.getBondedDevices();
    if(devices==null||devices.isEmpty()){status("Pair POS58B di Settings > Bluetooth terlebih dahulu.");return;}
    ArrayList<BluetoothDevice> list=new ArrayList<>(devices);
    String[] labels=new String[list.size()];
    for(int i=0;i<list.size();i++){BluetoothDevice d=list.get(i);labels[i]=(d.getName()==null?"BT Printer":d.getName())+"  ("+d.getAddress()+")";}
    runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Pilih POS58B (Paired)").setItems(labels,(dialog,index)->{
      printerMac=list.get(index).getAddress();getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("printerMac",printerMac).apply();status("Printer dipilih: "+labels[index]);
    }).setNegativeButton("Batal",null).show());
  }
  private void sendPrinter(String payloadJson){
    final org.json.JSONObject job;
    try{job=new org.json.JSONObject(payloadJson);}catch(Exception e){status("Format cetakan tidak sah.");return;}
    final String text=job.optString("text","");
    final boolean drawer=job.optBoolean("drawer",false);
    if(text.isEmpty()||text.length()>15000){status("Resit kosong atau terlalu panjang.");return;}
    if(printerMac.isEmpty()){status("Pilih printer Bluetooth dahulu (ikon BT di header).");return;}
    if(!hasBluetoothPermission()){askBluetoothPermission();return;}
    // Only one printer operation at a time; avoids interleaving cash drawer and print jobs.
    synchronized(btLock){if(printerBusy){status("Printer sibuk. Cuba Cetak Semula kemudian.");return;}printerBusy=true;}
    new Thread(()->{
      BluetoothSocket socket=null;
      try{
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        if(adapter==null||!adapter.isEnabled())throw new Exception("Bluetooth dimatikan");
        BluetoothDevice device=adapter.getRemoteDevice(printerMac);
        socket=device.createRfcommSocketToServiceRecord(SPP_UUID);
        socket.connect();
        OutputStream out=socket.getOutputStream();
        out.write(new byte[]{0x1b,0x40}); // ESC @ initialise
        out.write(new byte[]{0x1b,0x61,0}); // left aligned
        String clean=text.replace('’',(char)39).replace('–','-').replace('×','x').replace('✓',' ').replace('−','-');
        // Uses standard single-byte ESC/POS text. Unsupported glyphs are replaced.
        out.write(clean.getBytes(Charset.forName("windows-1252")));
        out.write(new byte[]{0x0a,0x0a,0x0a});
        if(drawer){
          // ESC p m t1 t2. Only called following newly confirmed CASH payment.
          out.write(new byte[]{0x1b,0x70,0x00,0x32,0x32});
        }
        out.flush();
        status(drawer?"Resit dihantar; arahan buka laci dihantar (Cash sahaja).":"Cetakan dihantar ke POS58B.");
      }catch(Exception e){status("Cetakan Bluetooth gagal: "+e.getMessage()+". Semak pairing / bateri / printer.");}
      finally{try{if(socket!=null)socket.close();}catch(Exception ignored){} synchronized(btLock){printerBusy=false;}}
    }).start();
  }
  public final class PrinterBridge{
    @JavascriptInterface public void printJob(String json){sendPrinter(json);}
    @JavascriptInterface public boolean isAvailable(){return !printerMac.isEmpty();}
  }

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
    layout.setBackgroundColor(Color.rgb(247,245,239));
    LinearLayout top = new LinearLayout(this); top.setPadding(18,12,18,12); top.setGravity(Gravity.CENTER_VERTICAL); top.setBackgroundColor(Color.rgb(32,59,49));
    TextView name = new TextView(this); name.setText("d’Laman · Sistem Pesanan"); name.setTextColor(Color.WHITE); name.setTextSize(17); name.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
    
    TextView bluetooth = new TextView(this);bluetooth.setText("▣ BT Printer");bluetooth.setTextColor(Color.WHITE);bluetooth.setPadding(12,12,12,12);bluetooth.setOnClickListener(v->choosePrinter());
    top.addView(name);top.addView(bluetooth);
    layout.addView(top);
    progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setVisibility(View.GONE);layout.addView(progress,new LinearLayout.LayoutParams(-1,5));
    web = new WebView(this); web.setBackgroundColor(Color.WHITE); layout.addView(web,new LinearLayout.LayoutParams(-1,0,1));
    setContentView(layout);
    printerMac=getSharedPreferences(PREFS,MODE_PRIVATE).getString("printerMac","");
    askBluetoothPermission();
    WebSettings opts=web.getSettings();opts.setJavaScriptEnabled(true);opts.setDomStorageEnabled(true);opts.setAllowFileAccess(false);opts.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);opts.setSupportMultipleWindows(false);opts.setJavaScriptCanOpenWindowsAutomatically(false);
    web.addJavascriptInterface(new PrinterBridge(),"DLamanPrinter");
    web.addJavascriptInterface(new DataBridge(),"DLamanData");
    web.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView w,int value){progress.setProgress(value);progress.setVisibility(value>=100?View.GONE:View.VISIBLE);}});
    web.setWebViewClient(new WebViewClient(){
      @Override public void onPageFinished(WebView view,String url){
        java.io.File cache=new java.io.File(getFilesDir(),"business-data.json");
        if(cache.exists())try{
          String json=new String(java.nio.file.Files.readAllBytes(cache.toPath()),java.nio.charset.StandardCharsets.UTF_8);
          view.evaluateJavascript("restoreNativeCache("+org.json.JSONObject.quote(json)+")",null);
        }catch(Exception ignored){}
      }
      @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
        Uri uri=request.getUrl();
        if("file".equals(uri.getScheme()) && uri.toString().startsWith("file:///android_asset/"))return false;
        return true;
      }
    });
    web.loadUrl("file:///android_asset/index.html");
  }
  public final class DataBridge {
    @JavascriptInterface public void save(String json){
      try{
        if(json.length()>15000000)return;
        java.io.File file=new java.io.File(getFilesDir(),"business-data.json");
        java.io.File temp=new java.io.File(getFilesDir(),"business-data.tmp");
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(temp)){
          out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();
        }
        if(!temp.renameTo(file))throw new java.io.IOException("Could not replace backup");
      }catch(Exception e){status("Tidak dapat menyimpan salinan data: "+e.getMessage());}
    }
    @JavascriptInterface public void exportBackup(String data){
      if(data.length()>15000000)return;
      pendingExport=data;
      runOnUiThread(()->{
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE,"dlaman-backup-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmm",java.util.Locale.US).format(new java.util.Date())+".json");
        startActivityForResult(intent,EXPORT_REQUEST);
      });
    }
    @JavascriptInterface public void importBackup(){runOnUiThread(()->{
      Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("application/json");
      startActivityForResult(intent,IMPORT_REQUEST);
    });}
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(result!=RESULT_OK||data==null||data.getData()==null)return;
    try{
      Uri uri=data.getData();
      if(request==EXPORT_REQUEST){
        try(OutputStream out=getContentResolver().openOutputStream(uri)){
          out.write(pendingExport.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        status("Backup disimpan.");pendingExport="";
      }else if(request==IMPORT_REQUEST){
        byte[] bytes;
        try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
          java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
          byte[] buf=new byte[8192];int n;
          while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>15000000)throw new Exception("Backup terlalu besar");}
          bytes=out.toByteArray();
        }
        String backup=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
        // JavaScript validates structure and obtains user confirmation before overwriting.
        web.evaluateJavascript("receiveStandaloneRestore("+org.json.JSONObject.quote(backup)+")",null);
      }
    }catch(Exception e){status("Operasi sandaran gagal: "+e.getMessage());}
  }

  @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else new AlertDialog.Builder(this).setMessage("Tutup aplikasi d’Laman?").setPositiveButton("Tutup",(d,w)->finish()).setNegativeButton("Batal",null).show();}
  @Override protected void onDestroy(){web.destroy();super.onDestroy();}
}
