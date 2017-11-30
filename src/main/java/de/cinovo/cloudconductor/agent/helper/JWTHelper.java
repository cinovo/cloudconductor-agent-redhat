package de.cinovo.cloudconductor.agent.helper;

import java.util.Date;

import com.nimbusds.jwt.JWTClaimsSet;

/**
 * 
 * Copyright 2017 Cinovo AG<br>
 * <br>
 * 
 * @author mweise
 *
 */
public class JWTHelper {
	
	private JWTHelper() {
		// hidden constructor
	}
	
	/**
	 * 
	 * @param jwt the jwt claimsset
	 * @return milliseconds until next refresh
	 */
	public static long calcNextRefreshInMillis(JWTClaimsSet jwt) {
		Date expiration = jwt.getExpirationTime();
		return (long) (0.9 * (expiration.getTime() - new Date().getTime()));
	}
	
}
