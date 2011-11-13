package net.fabene.butone;


public class Variables {

	static Variables instance;
	private String imageURL;

	public static synchronized Variables getInstance() {
		if (instance == null)
			instance = new Variables();
		return instance;
	}
	
	public String getURL() {
		if( imageURL == null){
			return "no image";
		}
		return imageURL;
	}

	public void setURL(String globalVariable) {
		imageURL = globalVariable;
	}

}