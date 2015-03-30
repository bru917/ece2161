package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.Serializable;

import android.graphics.Bitmap;

/**
 * Holds properties of a video as well as optimization data.
 * 
 * @author Brian Rupert
 *
 */
public class VideoProperties implements Serializable {

	private static final long serialVersionUID = 8077152057372795374L;

	private String id;
	
	private String videoId;
	
	/** Server database field: VideoName. */
	private String title;
	
	/** Server database field: VideoURL. */
	private String url;
	
	/** Server database field: VideoLgh. */
	private Long length;
	
	/** Server database field: OptStepLgh. */
	private String optStepLgh;
	
	/** Server database field: OptDimLv. */
	private String optDimLv;
	
	
	/** For UI display. */
	private transient Bitmap thumbnail;
	
	
	public VideoProperties() {
		
	}

	public VideoProperties(String id, String title, String url, Long length) {
		this.id = id;
		this.title = title;
		this.url = url;
		this.length = length;
	}
	
	public String getLengthString() {
		if (length != null) {
			return String.valueOf(this.length);
		}
		return null;
	}
	
	public String getServerName() {
		return this.title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getVideoId() {
		return videoId;
	}

	public void setVideoId(String videoId) {
		this.videoId = videoId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Long getLength() {
		return length;
	}

	public void setLength(Long length) {
		this.length = length;
	}

	public String getOptStepLgh() {
		return optStepLgh;
	}

	public Bitmap getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(Bitmap thumbnail) {
		this.thumbnail = thumbnail;
	}

	public void setOptStepLgh(String optStepLgh) {
		this.optStepLgh = optStepLgh;
	}

	public String getOptDimLv() {
		return optDimLv;
	}

	public void setOptDimLv(String optDimLv) {
		this.optDimLv = optDimLv;
	}
}
