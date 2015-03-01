package edu.pitt.ece2161.spring2015.optiplayer;

import android.graphics.Bitmap;

/**
 * Holds properties of a video as well as optimization data.
 * 
 * @author Brian Rupert
 *
 */
public class VideoProperties {

	private String id;
	
	private String videoId;
	
	private String title;
	
	private String url;
	
	private Bitmap thumbnail;
	
	private Long length;
	
	
	private String optStepLgh;
	
	private String optDimLv;
	
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
