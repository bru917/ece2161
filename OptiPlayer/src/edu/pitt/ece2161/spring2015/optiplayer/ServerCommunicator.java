package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import org.kobjects.base64.Base64;

import edu.pitt.ece2161.spring2015.server.getRemoteData_WebService;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

/**
 * This class handles the file upload and download requests to the server.
 * 
 * @author Brian Rupert
 *
 */
public class ServerCommunicator {
	
	private static final String TEAM_NAME = "SES174";

	public void download(Context ctx, String videoUrl) {
        new getRemoteData_WebService(ctx, "downloadSchemedb",
        		new String[] {"TeamName", "VideoURL" },
        		new String[] {TEAM_NAME, videoUrl},
            new DownloadHandler(ctx));
	}
	
	public void download(Context ctx, VideoProperties videoProps) {
        download(ctx, videoProps.getUrl());
	}
	
	private static class DownloadHandler extends Handler {
		
		private Context mContext;
		
		DownloadHandler(Context ctx) {
			mContext = ctx;
		}
		
		@Override
		public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_OK) {
                String resStr = msg.getData().getString(MESSAGE_MSG);
                HashMap<String, String> resmap = tranStr(resStr);
                
                // This points to the optimization file to request for download.
                String OptPath = resmap.get("OptPath");
                
                new AlertDialog.Builder(mContext)
                	.setTitle("Download Information")
                	.setMessage(resStr)
                    .setPositiveButton("OK", null)
                    .show();
                
                if (OptPath != null && OptPath.length() > 1) {
                    String url="http://" + getRemoteData_WebService.serverIP + OptPath.substring(1);
                    new getRemoteData_WebService(mContext, url, new DownloadFileHandler(mContext));
                }
            } else {
            	// File does not exist??
            	
                new AlertDialog.Builder(mContext)
	            	.setTitle("Download Information")
	            	.setMessage("ERROR? -> " + msg.what)
	                .setPositiveButton("Dismiss", null)
	                .show();
            }
        }
		
	}
	
	private static class DownloadFileHandler extends Handler {
		
		private Context mContext;
		
		DownloadFileHandler(Context ctx) {
			mContext = ctx;
		}
		
		@Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_OK) {
                String resStr = msg.getData().getString(MESSAGE_MSG);
                
                // File downloaded...
                
                
                new AlertDialog.Builder(mContext)
                	.setTitle("Download File Information")
                	.setMessage(resStr)
                    .setPositiveButton("OK", null)
                    .show();
            } else {
            	// unexpected error?
            	
                new AlertDialog.Builder(mContext)
	            	.setTitle("Download File Information")
	            	.setMessage("ERROR? -> " + msg.what)
	                .setPositiveButton("Dismiss", null)
	                .show();
            }
        }
	}
	
	/**
	 * Upload to the server.
	 * 
	 * @param ctx
	 * @param filePath
	 * @param props
	 */
	public void upload(Context ctx, String filePath, VideoProperties props) {
        FileInputStream fis = null;
        try {
	        fis = new FileInputStream(filePath);
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        byte[] buffer = new byte[1024];
	        int count = 0;
	        while ((count = fis.read(buffer)) >= 0) {
	            baos.write(buffer, 0, count);
	        }
	        String uploadBuffer = new String(Base64.encode(baos.toByteArray()));
	        uploadfile(ctx, uploadBuffer, props);
        } catch (IOException e) {
        	// TODO: handle exception
        } finally {
        	if (fis != null) {
        		try {
					fis.close();
				} catch (IOException e) {
					// SOL
				}
        	}
        }
	}
	
	public static final int MESSAGE_OK = 1;
	public static final String MESSAGE_MSG = "jgstr";
	
    private void uploadfile(final Context ctx, String fileBytes, VideoProperties vidProps) {
        new getRemoteData_WebService(ctx, "UploadSchemedb",
        		new String[] {"TeamName","VideoName" ,"VideoURL","VideoLgh",
        						"OptStepLgh","OptDimLv","fileBase64Datas"},
        		new String[] {TEAM_NAME, vidProps.getServerName(), vidProps.getUrl(),
        						vidProps.getLengthString(), vidProps.getOptStepLgh(),
        						vidProps.getOptDimLv(), fileBytes},
        		new UploadHandler(ctx));

    }
    
    private static class UploadHandler extends Handler {
    	
		private Context mContext;
		
		UploadHandler(Context ctx) {
			mContext = ctx;
		}
    	
    	@Override
    	public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_OK) {
            	// Should be "true" when upload succeeds.
                String resStr = msg.getData().getString(MESSAGE_MSG);
                
                new AlertDialog.Builder(mContext)
                	.setTitle("Upload Information")
                	.setMessage(resStr)
                    .setPositiveButton("OK", null)
                    .show();
            }
        }
    }
    
    private static HashMap<String, String> tranStr(String ss){
        HashMap<String, String> res=new HashMap<String, String>();
        String[] sg=ss.split("><");

        for (int i = 0; i < sg.length; i++) {
            String fieldname = sg[i].substring(0, sg[i].indexOf(">"));
            if (fieldname.substring(0, 1).equalsIgnoreCase("<")) {
            	fieldname=fieldname.substring(1);
            }
            String fieldvalue = sg[i].substring(sg[i].indexOf(">")+1, sg[i].indexOf("</"));
            if (fieldname!=null && fieldname.length()>0 && fieldvalue!=null && fieldvalue.length()>0) {
                res.put(fieldname, fieldvalue);
            }
        }
        return res;
    }
}
