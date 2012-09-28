package org.systemsbiology.util;


/**
 * class to hold the link text and address (url) of a hyperlink.
 */
public class Hyperlink {
	private String text;
	private String url;

	
	public Hyperlink(String text, String url) {
		this.text = text;
		this.url = url;
	}

	public String getText() {
		return text;
	}

	public String getUrl() {
		return url;
	}
	
}
