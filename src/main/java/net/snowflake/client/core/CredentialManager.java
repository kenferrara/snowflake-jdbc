/*
 * Copyright (c) 2012-2019 Snowflake Computing Inc. All rights reserved.
 */

package net.snowflake.client.core;

import com.google.common.base.Strings;
import java.net.MalformedURLException;
import java.net.URL;
import net.snowflake.client.jdbc.ErrorCode;
import net.snowflake.client.log.SFLogger;
import net.snowflake.client.log.SFLoggerFactory;

public class CredentialManager {
  private static final SFLogger logger = SFLoggerFactory.getLogger(CredentialManager.class);
  private SecureStorageManager secureStorageManager;

  private static CredentialManager instance;

  private CredentialManager() {
    try {
      if (Constants.getOS() == Constants.OS.MAC) {
        secureStorageManager = SecureStorageAppleManager.builder();
      } else if (Constants.getOS() == Constants.OS.WINDOWS) {
        secureStorageManager = SecureStorageWindowsManager.builder();
      } else if (Constants.getOS() == Constants.OS.LINUX) {
        secureStorageManager = SecureStorageLinuxManager.builder();
      } else {
        logger.error("Unsupported Operating System. Expected: OSX, Windows, Linux");
      }
    } catch (NoClassDefFoundError error) {
      logger.info(
          "JNA jar files are needed for Secure Local Storage service. Please follow the Snowflake JDBC instruction for Secure Local Storage feature. Fall back to normal process.");
    }
  }

  public static CredentialManager getInstance() {
    if (instance == null) {
      synchronized (CredentialManager.class) {
        if (instance == null) {
          instance = new CredentialManager();
        }
      }
    }
    return instance;
  }

  /**
   * Reuse the cached id token stored locally
   *
   * @param loginInput login input to attach id token
   */
  synchronized void fillCachedIdToken(SFLoginInput loginInput) throws SFException {
    if (secureStorageManager == null) {
      logger.info(
          "JNA jar files are needed for Secure Local Storage service. Please follow the Snowflake JDBC instruction for Secure Local Storage feature. Fall back to normal process.");
      return;
    }

    String idToken = null;
    try {
      idToken =
          secureStorageManager.getCredential(
              extractHostFromServerUrl(loginInput.getServerUrl()), loginInput.getUserName());
    } catch (NoClassDefFoundError error) {
      logger.info(
          "JNA jar files are needed for Secure Local Storage service. Please follow the Snowflake JDBC instruction for Secure Local Storage feature. Fall back to normal process.");
      return;
    }

    if (idToken == null) {
      logger.debug("retrieved idToken is null");
    }

    loginInput.setIdToken(idToken); // idToken can be null
    return;
  }

  /**
   * Store the temporary credential
   *
   * @param loginInput loginInput to denote to the cache
   * @param loginOutput loginOutput to denote to the cache
   */
  synchronized void writeTemporaryCredential(SFLoginInput loginInput, SFLoginOutput loginOutput)
      throws SFException {
    if (secureStorageManager == null) {
      logger.info(
          "JNA jar files are needed for Secure Local Storage service. Please follow the Snowflake JDBC instruction for Secure Local Storage feature. Fall back to normal process.");
      return;
    }

    String idToken = loginOutput.getIdToken();
    if (Strings.isNullOrEmpty(idToken)) {
      logger.debug("no idToken is given.");
      return; // no idToken
    }

    try {
      secureStorageManager.setCredential(
          extractHostFromServerUrl(loginInput.getServerUrl()), loginInput.getUserName(), idToken);
    } catch (NoClassDefFoundError error) {
      logger.info(
          "JNA jar files are needed for Secure Local Storage service. Please follow the Snowflake JDBC instruction for Secure Local Storage feature. Fall back to normal process.");
    }
  }

  /** Delete the id token cache */
  void deleteIdTokenCache(String host, String user) {
    if (secureStorageManager == null) {
      logger.info(
          "JNA jar files are needed for Secure Local Storage service. Please follow the Snowflake JDBC instruction for Secure Local Storage feature. Fall back to normal process.");
      return;
    }

    try {
      secureStorageManager.deleteCredential(host, user);
    } catch (NoClassDefFoundError error) {
      logger.info(
          "JNA jar files are needed for Secure Local Storage service. Please follow the Snowflake JDBC instruction for Secure Local Storage feature. Fall back to normal process.");
    }
  }

  /**
   * Used to extract host name from a well formated internal serverUrl, e.g., serverUrl in
   * SFLoginInput.
   */
  private String extractHostFromServerUrl(String serverUrl) throws SFException {
    URL url = null;
    try {
      url = new URL(serverUrl);
    } catch (MalformedURLException e) {
      logger.error("Invalid serverUrl for retrieving host name");
      throw new SFException(ErrorCode.INTERNAL_ERROR, "Invalid serverUrl for retrieving host name");
    }
    return url.getHost();
  }
}
