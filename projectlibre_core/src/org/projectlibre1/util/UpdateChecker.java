/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package org.projectlibre1.util;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.Alert;
import com.projectlibre1.util.BrowserControl;
import com.projectlibre1.util.VersionUtils;

/*
 * Format:
 * version: series of integers separated by dots
 * name: string
 *
 * The response is treated as untrusted data. Remote formulas are ignored;
 * version comparison uses UpdateCheckerFormula's fixed comparator.
 */
public class UpdateChecker {
	private static final int UPDATE_CHECKER_VERSION=1;
	private static final int MAX_UPDATE_RESPONSE_BYTES=4096;
	private static final String updateAddress = "https://projectlibre.org/versions-"+UPDATE_CHECKER_VERSION; 
	private static final String downloadAddress = "https://sourceforge.net/projects/projectlibre/files/latest/download";
	private static void checkForUpdate() {
		if (! Preferences.userNodeForPackage(UpdateChecker.class).getBoolean("checkForUpdates", true) ) {
			return;
		}
		String thisVersion = VersionUtils.getVersion();//Messages.getString("Release.version");
		if (thisVersion==null) thisVersion="0";
		URL url;
		try {
//			System.out.println("Encoded: "+URLEncoder.encode(System.getProperty("java.vendor"), StandardCharsets.UTF_8));
//			System.exit(1);
			//identify installed version, locale, jvm , to know if an update is available
			url = URI.create(updateAddress+
					"?version="+URLEncoder.encode(thisVersion==null?"0":thisVersion, StandardCharsets.UTF_8)+
					"&locale="+URLEncoder.encode(Locale.getDefault().toString(), StandardCharsets.UTF_8)+
					"&timeZone="+URLEncoder.encode(TimeZone.getDefault().getID().toString(), StandardCharsets.UTF_8)+
					"&osName="+URLEncoder.encode(System.getProperty("os.name"), StandardCharsets.UTF_8)+
					"&osVersion="+URLEncoder.encode(System.getProperty("os.version"), StandardCharsets.UTF_8)+
					"&osArch="+URLEncoder.encode(System.getProperty("os.arch"), StandardCharsets.UTF_8)+
					"&javaVersion="+URLEncoder.encode(System.getProperty("java.version"), StandardCharsets.UTF_8)+
					"&javaVendor="+URLEncoder.encode(System.getProperty("java.vendor"), StandardCharsets.UTF_8)+
					"&validation="+URLEncoder.encode(System.getProperty("projectlibre.validation","0"), StandardCharsets.UTF_8)+
					"&runNumber="+URLEncoder.encode(System.getProperty("projectlibre.runNumber","0"), StandardCharsets.UTF_8)+
					"&firstRun="+URLEncoder.encode(System.getProperty("projectlibre.firstRun","0"), StandardCharsets.UTF_8)+
					"&openprojRunNumber="+URLEncoder.encode(System.getProperty("projectlibre.projectLibreRunNumber","0"), StandardCharsets.UTF_8)+  //deprecated
					"&openprojFirstRun="+URLEncoder.encode(System.getProperty("projectlibre.projectLibreFirstRun","0"), StandardCharsets.UTF_8)+   //deprecated
					"&projectLibreRunNumber="+URLEncoder.encode(System.getProperty("projectlibre.projectLibreRunNumber","0"), StandardCharsets.UTF_8)+
					"&projectLibreFirstRun="+URLEncoder.encode(System.getProperty("projectlibre.projectLibreFirstRun","0"), StandardCharsets.UTF_8)+
					"&email="+URLEncoder.encode(System.getProperty("projectlibre.userEmail","0"), StandardCharsets.UTF_8)
					).toURL(); //$NON-NLS-1$
			if (!"https".equalsIgnoreCase(url.getProtocol())) {
				return;
			}
			HttpURLConnection connection=(HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(10000);
			connection.setRequestMethod("GET");
			try {
				connection.connect();
				try (InputStream input = connection.getInputStream()) {
					byte[] response=input.readNBytes(MAX_UPDATE_RESPONSE_BYTES);
					int lineEnd=0;
					while (lineEnd<response.length && response[lineEnd]!='\n' && response[lineEnd]!='\r') {
						lineEnd++;
					}
					if (lineEnd==response.length && response.length==MAX_UPDATE_RESPONSE_BYTES) {
						return;
					}
					String latestVersion=new String(response, 0, lineEnd, StandardCharsets.UTF_8).trim();
					if (latestVersion.isEmpty()) {
						return;
					}
					UpdateCheckerFormula formula=new UpdateCheckerFormula();
					if (formula.mainCompare(thisVersion, latestVersion)>=0) {
						return;
					}
					String lastVersionChecked=Preferences.userNodeForPackage(UpdateChecker.class)
							.get("lastVersionChecked","-1");
					if (formula.mainCompare(lastVersionChecked, latestVersion)>=0) {
						int runNumber=Preferences.userNodeForPackage(Class.forName("com.projectlibre1.main.Main"))
								.getInt("projectlibreRunNumber",0);
						int showEvery=Integer.parseInt(Messages.getString("UpdateDialog.showEvery"));
						int showEveryStagger=Integer.parseInt(Messages.getString("UpdateDialog.showEveryStagger"));
						if (showEvery > 0 && (runNumber-showEveryStagger)%showEvery != 0) {
							return;
						}
					}

					final String message=MessageFormat.format(Messages.getString("Text.newVersion"),
							new Object[] {latestVersion,thisVersion});
					Preferences.userNodeForPackage(UpdateChecker.class).put("lastVersionChecked", latestVersion);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							if (Alert.okCancel(message)) {
								BrowserControl.displayURL(downloadAddress);
							}
						}
					});
				}
			} finally {
				connection.disconnect();
			}

		} catch (Exception e) {
			//e.printStackTrace();
		}
	}



	public static void checkForUpdateInBackground() {
		Thread checker=new Thread(new Runnable() {
			public void run() {
				checkForUpdate();
			}
		}, "projectlibre-update-checker");
		checker.setDaemon(true);
		checker.start();
	}

}
