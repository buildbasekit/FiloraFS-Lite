package com.file.dtos;

import java.io.InputStream;

public class FileStream {
	private InputStream is;
	private String mimeType;

	public InputStream getIs() {
		return is;
	}

	public FileStream(InputStream is, String mimeType) {
		super();
		this.is = is;
		this.mimeType = mimeType;
	}

	public void setIs(InputStream is) {
		this.is = is;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

}
