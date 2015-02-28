package edu.pitt.ece2161.spring2015.optiplayer;

/**
 * Holds properties of a video as well as optimization data.
 * 
 * @author Brian Rupert
 *
 */
public class VideoProperties {

	private String id;
	
	private String name;
	
	private String url;
	
	private Long length;
	
	
	private String optStepLgh;
	
	private String optDimLv;
	
	/**
	 * Constructor.
	 * @param id Video ID
	 * @param name The name of the video.
	 * @param url
	 * @param length
	 */
	public VideoProperties(String id, String name, String url, Long length) {
		this.id = id;
		this.name = name;
		this.url = url;
		this.length = length;
	}
	
	public String getLengthString() {
		if (length != null) {
			return String.valueOf(this.length);
		}
		return null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
