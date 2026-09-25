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
package com.projectlibre1.main;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.prefs.Preferences;

import com.projectlibre1.dialog.UserInfoDialog;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.Environment;


/**
 *
 */
public class Main {
	private static final String FIRST_RUN_KEY = "projectlibreFirstRun";
	private static final String LEGACY_FIRST_RUN_KEY = "projectlibrefirstRun";

	public static void main(String[] args) {
		configureCrispText();
		int runNumber=getRunNumber()+1;
		long firstRun=getFirstRun();
		Preferences.userNodeForPackage(Main.class).putInt("projectlibreRunNumber",runNumber);
		Preferences.userNodeForPackage(Main.class).putLong(FIRST_RUN_KEY,firstRun);		
		System.setProperty("projectlibre.runNumber", runNumber+"");
		System.setProperty("projectlibre.firstRun", firstRun+"");
		System.setProperty("projectlibre.projectLibreRunNumber", getProjectLibreRunNumber()+"");
		System.setProperty("projectlibre.projectLibreFirstRun", getProjectLibreFirstRun()+"");
		
//		System.setProperty("file.encoding", "UTF-8");

		Environment.setStandAlone(true);
		String[] formatedArgs;
		if (args!=null && args.length>0){
			ArrayList<String> nonEmptyArgs=new ArrayList<String>(args.length);
			for (int i=0;i<args.length;i++){
				if (args[i]!=null&& args[i].length()>0) nonEmptyArgs.add(args[i]);
			}
			if (nonEmptyArgs.size()>0){
				ArrayList<String> formatedList=new ArrayList<String>();
				String s1,s2;
				for (Iterator<String> i=nonEmptyArgs.iterator();i.hasNext();){
					s1=i.next();
					if (i.hasNext()){
						s2=i.next();
					}else{
						s2=s1;
						s1="--fileNames";
					}
					formatedList.add(s1);
					formatedList.add(s2);
				}
				formatedArgs=formatedList.toArray(new String[]{});
			} else formatedArgs=args;
		} else formatedArgs=args;

		com.projectlibre1.pm.graphic.gantt.Main.main(formatedArgs);
	}
	public static int getRunNumber() {
		return Preferences.userNodeForPackage(Main.class).getInt("projectlibreRunNumber",0);
	}
	public static int getProjectLibreRunNumber() {
		int runNumber=Preferences.userNodeForPackage(Main.class).getInt("projectlibreRunNumber",-1);
		if (runNumber < 0) {
			runNumber=Preferences.userNodeForPackage(Main.class).getInt("runNumber",0);
		}
		return runNumber;
	}
	public static long getFirstRun() {
		Preferences preferences = Preferences.userNodeForPackage(Main.class);
		long firstRun = preferences.getLong(FIRST_RUN_KEY, 0L);
		if (firstRun == 0L) {
			firstRun = preferences.getLong(LEGACY_FIRST_RUN_KEY, System.currentTimeMillis());
		}
		return firstRun;
	}
	public static long getProjectLibreFirstRun() {
		return getFirstRun();
	}
	public static String getRunSinceMessage() {
		return MessageFormat.format(Messages.getString("Text.runsSinceMessage"),new Object[] {getRunNumber(),new Date(getFirstRun())});
	}

	/**
	 * Enables subpixel/LCD font smoothing and fractional metrics before any
	 * Swing component is created. Without this, text on Linux HiDPI looks
	 * blocky/pixelated (Metal LAF + default grayscale AA).
	 * Honors explicit -D flags from launcher scripts.
	 */
	private static void configureCrispText() {
		if (System.getProperty("awt.useSystemAAFontSettings") == null)
			System.setProperty("awt.useSystemAAFontSettings", "on");
		if (System.getProperty("swing.aatext") == null)
			System.setProperty("swing.aatext", "true");
		if (System.getProperty("sun.java2d.xrender") == null)
			System.setProperty("sun.java2d.xrender", "true");
		if (System.getProperty("sun.java2d.uiScale.enabled") == null)
			System.setProperty("sun.java2d.uiScale.enabled", "true");
	}

}
