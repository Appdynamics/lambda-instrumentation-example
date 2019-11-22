/**
 * 
 */
package com.acme.lambda.demo;

import java.util.HashMap;

/**
 * @author wayne.brown
 *
 */
public class WebFrontEndResponse {
	private final int statusCode;
	private final HashMap<String, String> headers;
	private final boolean isBase64Encoded;
	private final String body;		
	
	public WebFrontEndResponse(Builder b)
	{
		this.statusCode = b.statusCode;
		this.headers = b.headers;
		this.isBase64Encoded = b.isBase64Encoded;
		this.body = b.body;
	}
	
	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * @return the headers
	 */
	public HashMap<String, String> getHeaders() {
		return headers;
	}

	/**
	 * @return the isBase64Encoded
	 */
	public boolean isBase64Encoded() {
		return isBase64Encoded;
	}

	/**
	 * @return the body
	 */
	public String getBody() {
		return body;
	}

	static class Builder
	{
		private int statusCode;
		private HashMap<String, String> headers;
		private boolean isBase64Encoded;
		private String body;
		
		public static Builder newInstance() { 
			return new Builder();
		}
		
		private Builder() { }
		
		public Builder withStatusCode(int code)
		{
			this.statusCode = code;
			return this;
		}
		
		public Builder withEmptyHeaders()
		{
			this.headers = new HashMap<String,String>();
			return this;
		}
		
		public Builder withHeader(String key, String val)
		{
			if (this.headers == null)
			{
				this.headers = new HashMap<String,String>();
			}
			this.headers.put(key, val);
			return this;
		}
		
		public Builder withHeaders(HashMap<String, String> _headers)
		{
			this.headers = _headers;
			return this;
		}
		
		public Builder withIsBase64Encoded(boolean encoded)
		{
			this.isBase64Encoded = encoded;
			return this;
		}
		
		public Builder withBody(String body)
		{
			this.body = body;
			return this;
		}
		
		public WebFrontEndResponse create()
		{
			return new WebFrontEndResponse(this);
		}
	}
}
