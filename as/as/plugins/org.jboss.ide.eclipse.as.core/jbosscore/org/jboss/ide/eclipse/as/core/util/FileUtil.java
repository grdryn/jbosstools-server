/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.ide.eclipse.as.core.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.ide.eclipse.as.core.JBossServerCorePlugin;
import org.jboss.ide.eclipse.as.core.Messages;

public class FileUtil {

	public static interface IFileUtilListener {
		public void fileDeleted(File file, boolean result, Exception e);
		public void folderDeleted(File file, boolean result, Exception e);
		public void fileCopied(File source, File dest, boolean result, Exception e);
	}
	
	
	// Delete the file. If it's a folder, delete all children.
	public static void safeDelete(File file) {
		safeDelete(file, null);
	}
	public static void safeDelete(File file, IFileUtilListener listener) {
		if( file.isDirectory() ) {
			File[] children = file.listFiles();
			if( children != null ) {
				for( int i = 0; i < children.length; i++ ) {
					safeDelete(children[i], listener);
				}
			}
			
			if( file.exists()) {
				try {
					boolean tmp = file.delete();
					if( listener != null ) listener.folderDeleted(file, tmp, null);
				} catch( SecurityException sex) {
					if( listener != null ) listener.folderDeleted(file, false, sex);
				}
			}
		}
		
		// files only
		if( file.exists() ) {
			try {
				boolean tmp = file.delete();
				if( listener != null ) listener.fileDeleted(file, tmp, null);
			} catch( SecurityException sex) {
				if( listener != null ) listener.fileDeleted(file, false, sex);
			}
		}
	}
	
	// calls safedelete, but also deletes empty parent folders
	public static void completeDelete(File file) {
		completeDelete(file, null);
	}
	
	public static void completeDelete(File file, IFileUtilListener listener) {
		completeDelete(file, null, listener);
	}
	public static void completeDelete(File file, File archiveRoot, IFileUtilListener listener) {
		safeDelete(file, listener);
		//delete all empty parent folders
		while(!file.getParentFile().equals(archiveRoot) && file.getParentFile().listFiles().length == 0 ) {
			file = file.getParentFile();
			try {
				boolean tmp = file.delete();
				if( listener != null ) listener.folderDeleted(file, tmp, null);
			} catch( SecurityException sex ) {
				if( listener != null ) listener.folderDeleted(file, false, sex);
			}
		}
	}
	
	public static boolean fileSafeCopy(File src, File dest) {
		return fileSafeCopy(src, dest, null);
	}
	public static boolean fileSafeCopy(File src, File dest, IFileUtilListener listener) {
		File parent = dest.getParentFile();
		parent.mkdirs();
		if( dest.exists()) 
			safeDelete(dest);
		
		if (src.isDirectory()) {
			File[] subFiles = src.listFiles();
			boolean copied = true;
			dest.mkdirs();
			for (int i = 0; i < subFiles.length; i++) {
				File newDest = new File(dest, subFiles[i].getName());
				copied = copied && fileSafeCopy(subFiles[i], newDest, listener);
			}
			return copied;
		} else {
			try {
			    FileInputStream fis  = new FileInputStream(src);
			    FileOutputStream fos = new FileOutputStream(dest);
			    byte[] buf = new byte[1024];
			    int i = 0;
			    while((i=fis.read(buf))!=-1) {
			      fos.write(buf, 0, i);
			      }
			    fis.close();
			    fos.close();
			    if( listener != null ) listener.fileCopied(src, dest, true, null);
				return true;
			} catch( Exception e ) {
			    if( listener != null ) listener.fileCopied(src, dest, false, e);
				return false;
			}
		}
	}

	public static void touch(FileFilter filter, File root, boolean recurse) {
		if( filter.accept(root)) 
			root.setLastModified(new Date().getTime());
		if( recurse && root.isDirectory() ) {
			File[] children = root.listFiles();
			if( children != null ) {
				for( int i = 0; i < children.length; i++ ) {
					touch(filter, children[i], recurse);
				}
			}
		}
	}
	
	public static class FileUtilListener implements IFileUtilListener {
		protected ArrayList<IStatus> errors = new ArrayList<IStatus>();
		public void fileCopied(File source, File dest, boolean result,
				Exception e) {
			if(!result)
				errors.add(new Status(IStatus.ERROR, JBossServerCorePlugin.PLUGIN_ID, 
						NLS.bind(Messages.CopyFileError, source.toString(), dest.toString()), e));
		}
		public void fileDeleted(File file, boolean result, Exception e) {
			if(!result)
				errors.add(new Status(IStatus.ERROR, 
						JBossServerCorePlugin.PLUGIN_ID, 
						NLS.bind(Messages.DeleteFolderError, file.toString(), e)));
		}

		public void folderDeleted(File file, boolean result, Exception e) {
			if(!result)
				errors.add(new Status(IStatus.ERROR, JBossServerCorePlugin.PLUGIN_ID, 
						NLS.bind(Messages.DeleteFolderError, file.toString()), e));
		} 
		
		public IStatus[] getStatuses() {
			return (IStatus[]) errors.toArray(new IStatus[errors.size()]);
		}
	}
	
}