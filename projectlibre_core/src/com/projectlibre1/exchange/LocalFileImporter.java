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
package com.projectlibre1.exchange;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import com.projectlibre1.grouping.core.model.DefaultNodeModel;
import com.projectlibre1.job.Job;
import com.projectlibre1.job.JobRunnable;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.resource.ResourcePoolFactory;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.server.data.DataUtil;
import com.projectlibre1.server.data.DocumentData;
import com.projectlibre1.session.LocalSession;
import com.projectlibre1.session.SessionFactory;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.undo.DataFactoryUndoController;
import com.projectlibre1.util.Alert;
import com.projectlibre1.util.SerializationFilter;

/**
 * Loads/Saves a project from/to a pod file
 */
public class LocalFileImporter extends FileImporter {
	public static final String VERSION="1.0.0"; //$NON-NLS-1$
	private static final String PROJECT_LIBRE_FILE_SEPARATOR="@@@@@@@@@@ProjectLibreSeparator_MSXML@@@@@@@@@@";
	private static final String OLD_FILE="com.projity.server.data.ProjectData";
	private static final String XML_FILE_START="<?xml";
	/**
	 *
	 */
	public LocalFileImporter() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	

	@Override
	public void importFile() throws Exception{
		File f=new File(getFileName());
		FileInputStream fin=new FileInputStream(f);
		Exception ex=null;
		
		if (/*findString(fin, OLD_FILE)*/false) {
			System.out.println("Old file: ignoring binary content");
			project=null;
		}else {
	        try {
				DataUtil serializer=new DataUtil();
				System.out.println("Loading "+getFileName()+"..."); //$NON-NLS-1$ //$NON-NLS-2$

				long t1=System.currentTimeMillis();
				DocumentData projectData;
				try (ObjectInputStream in=new ObjectInputStream(fin)) {
					in.setObjectInputFilter(SerializationFilter.get());
					Object obj=in.readObject();
					if (obj instanceof String) obj=in.readObject(); //check version in the future
					projectData=(DocumentData)obj;
				}
				projectData.setMaster(true);
				projectData.setLocal(true);
				long t2=System.currentTimeMillis();
				System.out.println("Loading...Done in "+(t2-t1)+" ms"); //$NON-NLS-1$ //$NON-NLS-2$


				System.out.println("Deserializing..."); //$NON-NLS-1$
				t1=System.currentTimeMillis();
//	        project=serializer.deserializeProject(projectData,false,true,resourceMap);
				setProject(serializer.deserializeLocalDocument(projectData));
				t2=System.currentTimeMillis();
				System.out.println("Deserializing...Done in "+(t2-t1)+" ms"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (Exception e) {
				ex=e;
				project=null;
			}finally{
				try {
					fin.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
        
        if (project==null){
        	//recreate project
        	
        	BufferedInputStream in=null;
			try {
				//using xml
				System.out.println("Trying to recover with XML...");
				fin=new FileInputStream(f);
				byte[] keyBuf=PROJECT_LIBRE_FILE_SEPARATOR.getBytes();
				byte[] startXmlKeyBuf=XML_FILE_START.getBytes();
				int bufSize=100;
				if (bufSize<keyBuf.length) bufSize=keyBuf.length;
				byte[] buf= new byte[bufSize];
				in=new BufferedInputStream(fin); //use default 8192 bytes size
				
				int keyPos=0;
				int n;
//				int pos=0;
				boolean found=false;
				boolean xmlStartFound=false;
				boolean first=true;
				in.mark(bufSize);
				while ( (n=in.read( buf, 0, bufSize )) != -1 ){
					// testing if it's xml without PROJECT_LIBRE_FILE_SEPARATOR
					if (first && n>startXmlKeyBuf.length) {
						 for (int i=0; i<startXmlKeyBuf.length; i++ ){
							 if (startXmlKeyBuf[i]!=buf[i]) {
								 first=false;
								 break;								 
							 }
						 }
						 if (first) {
							 xmlStartFound=true;
							 break;
						 }
					}
						
				    for (int i=0; i<n; i++ ){
				    	if (keyBuf[keyPos]==buf[i]){
				    		if (keyPos==keyBuf.length-1){
				    			//found keyword
				    			found=true;
				    			in.reset();
				    			in.read(buf,0,i+1);
				    			break;
				    		}else{
				    			keyPos++;
				    		}
				    	}else keyPos=0;
				    }
				    if (found) break;
					in.mark(bufSize);
//				    pos+=n;
				}
				
				if (xmlStartFound) {
					if (in!=null){
						try {
							in.close();
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					fin=new FileInputStream(f);
					in=new BufferedInputStream(fin);
					
				}
				if (found || xmlStartFound) {
					//xml found
					System.out.println("XML found");
					// Parse the recovered XML in this importer so the outer load keeps its result.
					FileImporter importer=LocalSession.getImporter(LocalSession.MICROSOFT_PROJECT_IMPORTER);
					if (importer==null) {
						throw new IOException("Microsoft project importer is unavailable");
					}
					DataFactoryUndoController undoController=new DataFactoryUndoController();
					ResourcePool resourcePool=ResourcePoolFactory.getInstance().createResourcePool("",undoController);
					resourcePool.setLocal(true);
					Project recoveredProject=Project.createProject(resourcePool,undoController);
					((DefaultNodeModel)recoveredProject.getTaskOutline()).setDataFactory(recoveredProject);
					importer.setProject(recoveredProject);
					importer.setFileName(fileName);
					importer.setJobQueue(getJobQueue());
					try {
						project=importer.loadProject(in);
					} finally {
						in.close();
					}
					if (project==null) {
						throw new IOException("XML recovery did not produce a project");
					}
					project.setFileName(fileName);
					project.setMaster(true);
					project.setLocal(true);
//					project=projectFactory.openProject(opt);

					
//					FileImporter importer=LocalSession.getImporter("com.projectlibre1.exchange.MicrosoftImporter");
//					
//					ResourcePool resourcePool=null;
//					DataFactoryUndoController undoController=new DataFactoryUndoController();
//					resourcePool = ResourcePoolFactory.getInstance().createResourcePool("",undoController);
//					resourcePool.setLocal(true);
//					project = Project.createProject(resourcePool,undoController);						
//					((DefaultNodeModel)project.getTaskOutline()).setDataFactory(project);		
//					importer.setProject(project);
//					
//					importer.loadProject(in);
					System.out.println("Recovered with XML");
				}else{
					//unable to recover from xml 
		    		if ( ex!=null &&
		    				ex instanceof ClassNotFoundException &&
		    				"com.projity.server.data.ProjectData".equals(ex.getMessage())) {
		    			SwingUtilities.invokeLater(new Runnable(){
		    				public void run(){
				    			Alert.error(Messages.getString("Message.ImportOldFormatError"));
		    				}
		    			});
		    		}else {
		    			SwingUtilities.invokeLater(new Runnable(){
		    				public void run(){
				    			Alert.error(Messages.getString("Message.ImportError"));
		    				}
		    			});
		    			
		    		}
					
					
					if (ex!=null) throw ex;
				}
			} catch (Exception e) {
				if (in!=null){
					try {
						in.close();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				throw e;
			}
        }
	}

	
	private static boolean findString(InputStream fin, String stringToSearch) {
		BufferedInputStream in=null;
		try {
			byte[] keyBuf=stringToSearch.getBytes();
			int bufSize=100;
			if (bufSize<keyBuf.length) bufSize=keyBuf.length;
			byte[] buf= new byte[bufSize];
			in=new BufferedInputStream(fin); //use default 8192 bytes size
			
			int keyPos=0;
			int n;
			in.mark(bufSize);
			while ( (n=in.read( buf, 0, bufSize )) != -1 ){
			    for (int i=0; i<n; i++ ){
			    	if (keyBuf[keyPos]==buf[i]){
			    		if (keyPos==keyBuf.length-1){
			    			//found keyword
			    			return true;
			    		}else{
			    			keyPos++;
			    		}
			    	}else keyPos=0;
			    }
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in!=null){
				try {
					in.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
		return false;
    }



	@Override
	public void exportFile() throws Exception{
		if (fileName == null || fileName.length() == 0) {
			throw new IOException("No output file was selected");
		}

		String extension="";
		String name=fileName;
		int lastSeparator=Math.max(fileName.lastIndexOf(File.separatorChar), fileName.lastIndexOf('/'));
		int extensionIndex=fileName.lastIndexOf('.');
		if (extensionIndex>lastSeparator && extensionIndex>0){
			extension=fileName.substring(extensionIndex);
			name=fileName.substring(0, extensionIndex);
		}

		File file=new File(fileName);
		File tmpFile;
		int count=0;
		do {
			tmpFile=new File(name+"_tmp"+count+extension);
			count++;
		} while (tmpFile.exists());

		try {
			DataUtil serializer=new DataUtil();
			System.out.println("Serialization..."); //$NON-NLS-1$
			long t1=System.currentTimeMillis();
			DocumentData projectData=serializer.serializeDocument(getProject());
			projectData.setMaster(true);
			projectData.setLocal(true);
			long t2=System.currentTimeMillis();
			System.out.println("Serialization...Done in "+(t2-t1)+" ms"); //$NON-NLS-1$ //$NON-NLS-2$

			ByteArrayOutputStream serializedData=new ByteArrayOutputStream();
			try (ObjectOutputStream out=new ObjectOutputStream(serializedData)) {
				out.writeObject(VERSION);
				out.writeObject(projectData);
			}

			System.out.println("Saving "+file+"..."); //$NON-NLS-1$ //$NON-NLS-2$
			t1=System.currentTimeMillis();
			try (FileOutputStream fout=new FileOutputStream(tmpFile);
				 BufferedOutputStream bout=new BufferedOutputStream(fout)) {
				bout.write(serializedData.toByteArray());
				bout.write(PROJECT_LIBRE_FILE_SEPARATOR.getBytes(StandardCharsets.UTF_8));
				FileImporter importer=LocalSession.getImporter("com.projectlibre1.exchange.MicrosoftImporter");
				if (!importer.saveProject(project, bout)) {
					throw new IOException("Project data could not be written");
				}
			}
			moveFile(tmpFile, file);
			t2=System.currentTimeMillis();
			System.out.println("Saving...Done in "+(t2-t1)+" ms"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			tmpFile.delete();
			final String message=Messages.getString("Message.saveErrorTmpFile")+tmpFile.getPath();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					Alert.error(message);
				}
			});
			throw e;
		}
	}

	private static void moveFile(File source, File target) throws IOException {
		try {
			Files.move(source.toPath(), target.toPath(),
					StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException | FileAlreadyExistsException e) {
			Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}



	public Job getImportFileJob(){
		return getImportFileJob(this);
	}

    public static Job getImportFileJob(final FileImporter importer){
    	final Job job=new Job(importer.getJobQueue(),"importFile",Messages.getString("LocalFileImporter.Importing"),true); //$NON-NLS-1$ //$NON-NLS-2$
        job.addRunnable(new JobRunnable("Import",1.0f){ //$NON-NLS-1$
    		public Object run() throws Exception{
    			importer.importFile();
    			setProgress(1.0f);
                return null;
    		}
        });
        return job;
    }

    public Job getExportFileJob(){
    	return getExportFileJob(this);
    }
    public static Job getExportFileJob(final FileImporter importer){
    	final Job job=new Job(importer.getJobQueue(),"exportFile",Messages.getString("LocalFileImporter.Exporting"),true); //$NON-NLS-1$ //$NON-NLS-2$
        job.addRunnable(new JobRunnable("Export",1.0f){ //$NON-NLS-1$
    		public Object run() throws Exception{
    			importer.exportFile();
     			setProgress(1.0f);
                return null;
    		}
        });
        return job;
    }
    
    //disabled
    @Override
	public boolean saveProject(Project project,OutputStream out) throws Exception{
		return false;
	}
    
    @Override
	public Project loadProject(InputStream in)  throws Exception{
    	//disabled
    	return null;
	}

}
