package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.junit.Test;

public class StartupTest {
	@Test
	public void testHexString() {
		t("");
		t("00", 0x00);
		t("0f", 0x0f);
		t("2f", 0x2f);
		t("ff", 0xff);
		t("ffff", 0xff, 0xff);
		t("0000", 0x00, 0x00);
		t("123456789abcde", 0x12, 0x34, 0x56, 0x78, 0x9a, 0xbc, 0xde);
	}
	private void t(String expected, int... buf) {
		byte[] bytes = new byte[buf.length];
		for (int i=0; i<buf.length; ++i)
			bytes[i] = (byte)buf[i];
		Assert.assertEquals(expected, Startup.toHexString(bytes));
	}

	@Test
	public void testExplodeFiles() throws IOException {
		File tempFile = File.createTempFile(StartupTest.class.getSimpleName(), "test");
		try {
			File tempDir = new File(tempFile.getAbsolutePath() + "dir");
			tempDir.mkdir();
			try {
				testExplodeFiles(tempDir, new File("../pcgenviewer/bin/pcgenviewer.apk"));
			} finally {
				FileUtil.deleteTree(tempDir);
			}
		} finally {
			tempFile.delete();
		}
	}
	private void testExplodeFiles(File tmpDir, final File apkFile) throws IOException {
		Opener opener = new Opener(apkFile);
		File cacheDir = Startup.Exploder.explodeFiles(apkFile, tmpDir, opener);
		Assert.assertNotNull(cacheDir);
		Assert.assertTrue(new File(cacheDir, "system").exists());
		Assert.assertEquals(1, opener.nOpened);
		//Startup.recursiveShowDir("exploded dir "+cacheDir, cacheDir, cacheDir, System.out);

		// Second time should be a no-op, so the opener should not be re-accessed
		cacheDir = Startup.Exploder.explodeFiles(apkFile, tmpDir, opener);
		Assert.assertNotNull(cacheDir);
		Assert.assertTrue(new File(cacheDir, "system").exists());
		Assert.assertEquals(1, opener.nOpened);
	}

	private final class Opener implements Startup.LazyStreamOpener {
		private final File apkFile;
		public int nOpened = 0;
		private ZipFile zipFile = null;

		private Opener(File apkFile) {
			this.apkFile = apkFile;
		}

		@Override
		public InputStream open() throws IOException {
			nOpened++;
			zipFile = new ZipFile(apkFile);
			ZipEntry entry = zipFile.getEntry("assets/datafiles.zip");
			return zipFile.getInputStream(entry);
		}

		@Override
		public void close() throws IOException {
			if (zipFile != null) {
				zipFile.close();
				zipFile = null;
			}
		}
	}
}
