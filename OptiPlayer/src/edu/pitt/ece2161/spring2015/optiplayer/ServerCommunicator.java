package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import org.kobjects.base64.Base64;

import edu.pitt.ece2161.spring2015.server.getRemoteData_WebService;
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
	
	public enum CommStatus {
		UploadOk,
		UploadFailed,
		DownloadOk,
		DownloadNotFound,
		DownloadRequestFailed,
		DownloadFileFailed
	}
	
	public interface CommCallback {
		public void execute(CommStatus status, Object data);
	}

	public void download(Context ctx, String videoUrl, CommCallback callback) {
		
		if (videoUrl == null) {
			throw new IllegalArgumentException("Video URL should not be null");
		}
		
        new getRemoteData_WebService(ctx, "downloadSchemedb",
        		new String[] {"TeamName", "VideoURL" },
        		new String[] {TEAM_NAME, videoUrl},
            new DownloadHandler(ctx, callback));
	}
	
	public void download(Context ctx, VideoProperties videoProps, CommCallback callback) {
        download(ctx, videoProps.getUrl(), callback);
	}
	
	private static class DownloadHandler extends Handler {
		
		private Context mContext;
		private CommCallback cb;
		
		DownloadHandler(Context ctx, CommCallback callback) {
			mContext = ctx;
			this.cb = callback;
		}
		
		@Override
		public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_OK) {
                String resStr = msg.getData().getString(MESSAGE_MSG);
                HashMap<String, String> resmap = tranStr(resStr);
                
                // This points to the optimization file to request for download.
                String OptPath = resmap.get("OptPath");
                
                if (OptPath != null && OptPath.length() > 1) {
                    String url = "http://" + getRemoteData_WebService.serverIP + OptPath.substring(1);
                    new getRemoteData_WebService(mContext, url, new DownloadFileHandler(cb));
                } else {
                	// File does not exist.
                	cb.execute(CommStatus.DownloadNotFound, null);
                }
            } else {
            	// Some other error?
            	cb.execute(CommStatus.DownloadRequestFailed, msg);
            }
        }
		
	}
	
	private static class DownloadFileHandler extends Handler {
		
		private CommCallback cb;
		
		DownloadFileHandler(CommCallback cb) {
			this.cb = cb;
		}
		
		@Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_OK) {
                String resStr = msg.getData().getString(MESSAGE_MSG);
                
                // File downloaded...
                cb.execute(CommStatus.DownloadOk, resStr);
            } else {
            	// unexpected error?
            	cb.execute(CommStatus.DownloadFileFailed, msg);
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
	public void upload(Context ctx, String filePath, VideoProperties props, CommCallback cb) {
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
	        uploadfile(ctx, uploadBuffer, props, cb);
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
	
    private void uploadfile(final Context ctx, String fileBytes, VideoProperties vidProps, CommCallback cb) {
        new getRemoteData_WebService(ctx, "UploadSchemedb",
        		new String[] {"TeamName","VideoName" ,"VideoURL","VideoLgh",
        						"OptStepLgh","OptDimLv","fileBase64Datas"},
        		new String[] {TEAM_NAME, vidProps.getServerName(), vidProps.getUrl(),
        						vidProps.getLengthString(), vidProps.getOptStepLgh(),
        						vidProps.getOptDimLv(), fileBytes},
        		new UploadHandler(cb));

    }
    
    private static class UploadHandler extends Handler {
    	
		private CommCallback cb;
		
		UploadHandler(CommCallback cb) {
			this.cb = cb;
		}
    	
    	@Override
    	public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_OK) {
            	// Should be "true" when upload succeeds.
                String resStr = msg.getData().getString(MESSAGE_MSG);
                cb.execute(CommStatus.UploadOk, resStr);
            } else {
            	cb.execute(CommStatus.UploadFailed, msg);
            }
        }
    }
    
    private static HashMap<String, String> tranStr(String ss){
        HashMap<String, String> res = new HashMap<String, String>();
        String[] sg = ss.split("><");
        
        if (!ss.contains("><")) {
        	return res;
        }

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
