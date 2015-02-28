/*+----------------------------------------------------------------------
  ||  Class [ECE 2160 Embedded System II ]
  ||         Author:  [Dr. Yiguang Gong]
  ||                  [PhD. Candidate Xiang Chen]
  ||        Purpose:  [Server Connection Demo]
  ||    Description:  [getRemoteData_WebService: Standard network communication functions.]
  ||   Notification:  [Please don't try to modify this code by yourself.]
  ||        Contact:  [If you have any question, please contact xic33@pitt.edu.]
  ++-----------------------------------------------------------------------*/

package edu.pitt.ece2161.spring2015.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.ksoap2.transport.ServiceConnection;
import org.ksoap2.transport.ServiceConnectionSE;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class getRemoteData_WebService {
    public static String serverIP="104.154.40.219"; // Our course server.
    private String url ="http://"+serverIP+"/webservice.asmx?op=";
    private String nameSpace = "http://tempuri.org/"; // http://en.wikipedia.org/wiki/Tempuri
    public String resStr = "";
    private String methodName = null;
    private SoapObject request = null;
    //private final int timeOut1 = 4 * 1000; //
    public final static String files_PATH = Environment.getExternalStorageDirectory() + "/";

    // construct function
    public getRemoteData_WebService() {
    }


    //construct function: get webservice
    public getRemoteData_WebService(Context mContext, String methodname,String[] paraName,String[] paraValue,final Handler netHandler) {
        if (!checkNet(mContext)) {
            String jgstr="Error:cannot access Internet.";
            Message msg=new Message();
            msg.what=1;
            Bundle data=new Bundle();
            data.putString("jgstr",jgstr);
            msg.setData(data);
            netHandler.sendMessage(msg);
            return;
        }

        this.methodName = methodname;
        request = new SoapObject(nameSpace, methodName);
        String ss="";
        if (paraName!=null && paraName.length>0) {
            for (int i = 0; i < paraName.length; i++) {
                request.addProperty(paraName[i], paraValue[i]);
                ss+=paraName[i]+"="+paraValue[i]+";";
            }
        }
        Log.d("netconn","methodName="+this.methodName+";paraName:"+ss);

        //run thread to go to internet
        Runnable downloadRun = new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                String jgstr="";
                try {
                    jgstr = CallWebService(url+methodName); // save the result
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
                Message msg=new Message();
                msg.what=1;
                Bundle data=new Bundle();
                data.putString("jgstr",jgstr);
                msg.setData(data);
                netHandler.sendMessage(msg);
            }
        };
        new Thread(downloadRun).start();
    }


    //get http download
    public getRemoteData_WebService(Context mContext, final String fileUrl,final Handler netHandler)  {
        Runnable downloadRun = new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                String jgstr="";
                String newFilename = fileUrl.substring(fileUrl.lastIndexOf("/")+1);
                newFilename = files_PATH + newFilename;
                File dirFile = new File(files_PATH);
                if (!dirFile.exists()) {
                    try {
                        // Create a folder
                        dirFile.mkdirs();
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
                }

                try {
                    URL url = new URL(fileUrl);
                    URLConnection con = url.openConnection();
                    InputStream is = con.getInputStream();
                    byte[] bs = new byte[1024];
                    int len;
                    File file = new File(newFilename);
                    if(file.exists()) {
                        file.delete();
                    }
                    FileOutputStream os= new FileOutputStream(file,true);
                    while ((len = is.read(bs)) != -1) {
                        os.write(bs, 0, len);
                        if(jgstr.length()<1)jgstr="OK";
                    }
                    os.close();
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    jgstr=e.getMessage();
                }

                Message msg=new Message();
                msg.what=1;
                Bundle data=new Bundle();
                data.putString("jgstr",jgstr);
                msg.setData(data);
                netHandler.sendMessage(msg);
            }
        };
        new Thread(downloadRun).start();
    }


    public static boolean checkNet(Context context) {
        ConnectivityManager mConnMgr = (ConnectivityManager) context
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = mConnMgr
            .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mMobile = mConnMgr
            .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean flag = false;
        if ((mWifi != null) && ((mWifi.isAvailable()) || (mMobile.isAvailable()))) {
            if ((mWifi.isConnected()) || (mMobile.isConnected())) {
                flag = true;
            }
        }
        return flag;
    }



    private String CallWebService(String url) throws Exception {
        String SOAP_ACTION = nameSpace + methodName;
        String response = "";
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.bodyOut = request;
        envelope.dotNet = true; // .net support
        envelope.setOutputSoapObject(request);

        // AndroidHttpTransport androidHttpTrandsport=new
        // AndroidHttpTransport(url);
        HttpTransport1 androidHttpTrandsport = null;
        try {
            androidHttpTrandsport = new HttpTransport1(url, 8000);
        } catch (Exception e) {
            // TODO: handle exception
            return "Error:Connect timeout or error, please try again.";
        }

        androidHttpTrandsport.debug = true;
        // SoapObject result=null;

        try {
            // send web service
            androidHttpTrandsport.call(SOAP_ACTION, envelope);
            Object temp = (Object) envelope.getResponse();
            response = temp.toString();
        } catch (Exception ex) {
            return  "Error:Connect timeout or error, please try again.";
        }
        Log.d("netconn","response="+response);
        return response;
    }


    class HttpTransport1 extends HttpTransportSE {
        private int timeout = 4000;
        public HttpTransport1(String url) {
            super(url);
        }

        public HttpTransport1(String url, int timeout) {
            super(url);
            this.timeout = timeout;
        }

        protected ServiceConnection getServiceConnection() throws IOException {
            ServiceConnectionSE serviceConnection = new ServiceConnectionSE(
                    this.url, timeout);
            return serviceConnection;
        }
    }
}
