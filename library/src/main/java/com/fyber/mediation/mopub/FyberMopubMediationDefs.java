package com.fyber.mediation.mopub;

/**
 * Public mediation related definitions class.
 * <br>Used mainly by our custom adapter classes, in order to read data from Mopub's local and remote extras
 * <br>If you would like to pass parameters to Fyber
 * <br>Please look at the mediation sample, and use values from this definition class
 */
public class FyberMopubMediationDefs {
	
	// Local params definitions. Ad passed through local extras
	public final static String KEY_GENDER = "gender";
	public final static String GENDER_MALE = "m";
	public final static String GENDER_FEMALE = "f";
	public final static String KEY_AGE = "age";
	public final static String KEY_ZIPCODE = "zipCode";
	public final static String KEY_KEYWORDS = "keywords";
	
	// Remote params definitions. As received from remote mediation console definition
	public final static String REMOTE_KEY_SPOT_ID = "spotID";
	public final static String REMOTE_KEY_APP_ID = "appID";
	public final static String REMOTE_KEY_DEBUG = "debug";
}
