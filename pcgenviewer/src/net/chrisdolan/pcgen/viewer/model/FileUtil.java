package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Holder to keep this fiddly code out of the other classes.
 * @author chris
 */
public class FileUtil {
	public static List<File> deleteTree(File root) {
		List<File> out = new ArrayList<File>();
		File[] subfiles = root.listFiles();
		if (null != subfiles) {
			for (File subf : subfiles)
				out.addAll(deleteTree(subf));
		}
		if (!root.delete()) {
			out.add(root);
		}
		return out;
	}

	public static int unzip(ZipInputStream zis, File destDir) throws IOException {
		int numFiles = 0;
		byte[] buf = new byte[4096];
		while (true) {
			ZipEntry zipEntry = zis.getNextEntry();
			if (null == zipEntry)
				break;
			if (zipEntry.isDirectory())
				continue; // just infer dirs from files instead...
			File outFile = new File(destDir, zipEntry.getName());
			outFile.getParentFile().mkdirs();
			FileOutputStream fos = new FileOutputStream(outFile);
			try {
				while (true) {
					int bytesRead = zis.read(buf);
					if (bytesRead <= 0)
						break;
					fos.write(buf, 0, bytesRead);
				}
				numFiles++;
			} finally {
				fos.close();
			}
        }
		return numFiles;
	}
}
