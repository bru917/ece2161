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
	
	/**
	 * Server response codes.
	 * 
	 * @author Brian Rupert
	 */
	public enum CommStatus {
		UploadOk,
		UploadFailed,
		DownloadOk,
		DownloadNotFound,
		DownloadRequestFailed,
		DownloadFileFailed
	}
	
	/**
	 * A callback interface invoked when the server response is received.
	 * 
	 * @author Brian Rupert
	 */
	public interface CommCallback {
		
		/**
		 * Invoked for the client to handle the server response.
		 * @param status The server response code.
		 * @param data The response data, if any response data was acquired.
		 */
		public void execute(CommStatus status, Object data);
	}

	/**
	 * Downloads dimming data from the server, if a dimming scheme exists for
	 * the requested video URL.
	 * @param ctx The current context.
	 * @param videoUrl The URL of the video being played.
	 * @param callback A callback invoked after the server response has been
	 * attained. A Status code will indicate how the server responded.
	 */
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
	
	/**
	 * This handler is responsible for discovering if a dimming scheme is available
	 * for download. The scheme download is a two-stage (i.e. two-handler) process
	 * where a second handler is responsible for the actual file download.
	 * 
	 * @author Brian Rupert
	 */
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
	 * @param ctx The current context.
	 * @param filePath The path of the file to upload.
	 * @param props The properties of the corresponding video for this file.
	 * @param cb The callback invoked to forward the server response. 
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
