package havis.device.rf.common.util;

import static mockit.Deencapsulation.setField;
import static org.junit.Assert.fail;
import havis.device.rf.common.Environment;
import havis.device.rf.exception.ImplementationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.junit.Assert;
import org.junit.Test;

public class FileUtilsTest {

	@Test
	public void testFileUtils() {
		try {
			new FileUtils();
		} catch (Exception e) {
			fail(e.toString());
		}
	}

	@Test
	public void testReadTextFileString() {

		/* test file in tmp dir */
		File testFile = Paths.get(System.getProperty("java.io.tmpdir"),
				String.format("testFile_%d.txt", new Date().getTime()))
				.toFile();

		try {

			/* some unicode chars */
			String writeString = "\u0910\u0911\u0912\u0913\u0914\u0915\u0916\u0917\u0918\u0919\u0920";

			/* write file using using Java utils only */
			try (OutputStreamWriter osw = new OutputStreamWriter(
					new FileOutputStream(testFile), Charset.forName("UTF-8"))) {
				osw.write(writeString);
			}

			/* read the file using FileUtils class */
			String readString = FileUtils.readTextFile(testFile.toString());

			/* assert equality of written and read string */
			Assert.assertEquals(readString, writeString);

		}

		/* fail test on exception */
		catch (Exception ex) {
			fail(ex.toString());
		}

		/* try to delete test file */
		finally {
			try {
				testFile.delete();
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void testReadTextFileInputStream() {

		/* test file in tmp dir */
		File testFile = Paths.get(System.getProperty("java.io.tmpdir"),
				String.format("testFile_%d.txt", new Date().getTime()))
				.toFile();

		try {

			/* some unicode chars */
			String writeString = "\u0910\u0911\u0912\u0913\u0914\u0915\u0916\u0917\u0918\u0919\u0920";

			/* write file using using Java utils only */
			try (OutputStreamWriter osw = new OutputStreamWriter(
					new FileOutputStream(testFile), Charset.forName("UTF-8"))) {
				osw.write(writeString);
			}

			/* read the file using FileUtils class */
			String readString;
			try (FileInputStream inputStream = new FileInputStream(testFile)) {
				readString = FileUtils.readTextFile(inputStream);
			}

			/* assert equality of written and read string */
			Assert.assertEquals(readString, writeString);

		}

		/* fail test on exception */
		catch (Exception ex) {
			fail(ex.toString());
		}

		/* try to delete test file */
		finally {
			try {
				testFile.delete();
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void testWriteTextFile() {
		/* test file in tmp dir */
		File testFile = Paths.get(System.getProperty("java.io.tmpdir"),
				String.format("testFile_%d.txt", new Date().getTime()))
				.toFile();

		try {

			/* some unicode chars */
			String writeString = "\u0910\u0911\u0912\u0913\u0914\u0915\u0916\u0917\u0918\u0919\u0920";

			/* write file using using FileUtils class */
			FileUtils.writeTextFile(testFile, writeString);

			/* read the file using Java utils */
			StringBuffer sb = new StringBuffer();
			try (InputStreamReader isr = new InputStreamReader(
					new FileInputStream(testFile), Charset.forName("UTF-8"))) {
				final int bufLen = 1024 * 4; // 4 kB
				int charsRead = 0;
				char[] charBuf = new char[bufLen];
				while ((charsRead = isr.read(charBuf, 0, charBuf.length)) > -1)
					sb.append(charBuf, 0, charsRead);
			}

			/* assert equality of written and read string */
			Assert.assertEquals(sb.toString(), writeString);

		}

		/* fail test on exception */
		catch (Exception ex) {
			fail(ex.toString());
		}

		/* try to delete test file */
		finally {
			try {
				testFile.delete();
			} catch (Exception ex) {
			}
		}
	}

	public class SomePojoClass {
		private String foo;
		private String bar;

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}

	@Test
	public void testDeserialize(@Mocked final JsonSerializer jsonizer) {

		final File testFile = Paths.get(System.getProperty("java.io.tmpdir"),
				String.format("testFile_%d.txt", new Date().getTime()))
				.toFile();

		final SomePojoClass pojo = new SomePojoClass();
		pojo.setBar("bar");
		pojo.setFoo("foo");

		final String someJsonString = "Some JSON string";

		try {

			new NonStrictExpectations() {
				{
					jsonizer.deserialize(withEqual(someJsonString));
					result = pojo;
				}
			};

			FileUtils.writeTextFile(testFile, someJsonString);
			FileUtils.deserialize(testFile.toString(), SomePojoClass.class);

			new Verifications() {
				{
					String capt = null;
					jsonizer.deserialize(capt = withCapture());
					times = 1;

					Assert.assertEquals(capt, someJsonString);
				}
			};
		}

		catch (Exception ex) {
			fail(ex.toString());
		}

		finally {
			try {
				testFile.delete();
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testSerialize(@Mocked final JsonSerializer jsonizer) throws IOException,
			ImplementationException {

		final File testFile = Paths.get(System.getProperty("java.io.tmpdir"),
				String.format("testFile_%d.txt", new Date().getTime()))
				.toFile();

		final SomePojoClass pojo = new SomePojoClass();
		pojo.setBar("bar");
		pojo.setFoo("foo");

		try {

			new NonStrictExpectations() {
				{
					jsonizer.serialize(withEqual(pojo));
					result = "Some JSON string";
					setField(Environment.class, "SERIALIZER_PRETTY_PRINT", false);
				}
			};

			FileUtils.serialize(testFile, pojo);

			new Verifications() {
				{
					SomePojoClass capt = null;
					jsonizer.serialize(capt = withCapture());

					times = 1;
					Assert.assertTrue(capt == pojo);

					String fileContent = FileUtils.readTextFile(testFile
							.toString());
					Assert.assertEquals(fileContent, "Some JSON string");
				}
			};
		}

		catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.toString());
		}

		finally {
			try {
				testFile.delete();
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testCopyFileFileFileBooleanBoolean() throws IOException {
		String testRoot = System.getProperty("java.io.tmpdir");
		String fileName = String
				.format("testFile_%d.txt", new Date().getTime());
		String dirName = String.format("test_%d", new Date().getTime());

		File src = new File(testRoot + File.separator + fileName);

		File dest = new File(System.getProperty("java.io.tmpdir")
				+ File.separator + dirName + File.separator + fileName);

		src.createNewFile();

		try {

			try {
				FileUtils.copyFile(src, dest, false, false);
				fail("Exception expected but none thrown.");
			} catch (IOException e) {
			}

			try {
				FileUtils.copyFile(src, dest, false, true);
				Assert.assertTrue(dest.exists());
				Assert.assertTrue(src.exists());
			} catch (IOException e) {
				fail(e.toString());
			}

			try {
				FileUtils.copyFile(src, dest, false, false);
				fail("Exception expected but none thrown.");
			} catch (IOException e) {
			}

			try {
				FileUtils.copyFile(src, dest, true, false);
				Assert.assertTrue(dest.exists());
				Assert.assertTrue(src.exists());

			} catch (IOException e) {
				fail(e.toString());
			}

		} finally {
			try {
				src.delete();
			} catch (Exception ex) {
			}
			try {
				dest.delete();
			} catch (Exception ex) {
			}
			try {
				new File(testRoot + File.separator + dirName).delete();
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void testCopyFileStringStringBooleanBoolean() throws IOException {
		String testRoot = System.getProperty("java.io.tmpdir");
		String fileName = String
				.format("testFile_%d.txt", new Date().getTime());
		String dirName = String.format("test_%d", new Date().getTime());

		File src = new File(testRoot + File.separator + fileName);

		File dest = new File(System.getProperty("java.io.tmpdir")
				+ File.separator + dirName + File.separator + fileName);

		src.createNewFile();

		try {
			try {
				FileUtils.copyFile(src.toString(), dest.toString(), false,
						false);
				fail("Exception expected but none thrown.");
			} catch (IOException e) {
			}

			try {
				FileUtils
						.copyFile(src.toString(), dest.toString(), false, true);
				Assert.assertTrue(dest.exists());
				Assert.assertTrue(src.exists());
			} catch (IOException e) {
				fail(e.toString());
			}

			try {
				FileUtils.copyFile(src.toString(), dest.toString(), false,
						false);
				fail("Exception expected but none thrown.");
			} catch (IOException e) {
			}

			try {
				FileUtils
						.copyFile(src.toString(), dest.toString(), true, false);
				Assert.assertTrue(dest.exists());
				Assert.assertTrue(src.exists());
			} catch (IOException e) {
				fail(e.toString());
			}
		} finally {
			try {
				src.delete();
			} catch (Exception ex) {
			}
			try {
				dest.delete();
			} catch (Exception ex) {
			}
			try {
				new File(testRoot + File.separator + dirName).delete();
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void testMoveFile() throws IOException {
		String testRoot = System.getProperty("java.io.tmpdir");
		String fileName = String
				.format("testFile_%d.txt", new Date().getTime());
		String dirName = String.format("test_%d", new Date().getTime());

		File src = new File(testRoot + File.separator + fileName);

		File dest = new File(System.getProperty("java.io.tmpdir")
				+ File.separator + dirName + File.separator + fileName);

		src.createNewFile();

		try {

			try {
				FileUtils.moveFile(src.toString(), dest.toString(), false,
						false);
				fail("Exception expected but none thrown.");
			} catch (IOException e) {
			}

			try {
				FileUtils
						.moveFile(src.toString(), dest.toString(), false, true);
				Assert.assertTrue(dest.exists());
				Assert.assertTrue(!src.exists());
			} catch (IOException e) {
				fail(e.toString());
			}

			src.createNewFile();

			try {
				FileUtils.moveFile(src.toString(), dest.toString(), false,
						false);
				fail("Exception expected but none thrown.");
			} catch (IOException e) {
			}

			try {
				FileUtils
						.moveFile(src.toString(), dest.toString(), true, false);
				Assert.assertTrue(dest.exists());
				Assert.assertTrue(!src.exists());

			} catch (IOException e) {
				fail(e.toString());
			}

		} finally {
			try {
				src.delete();
			} catch (Exception ex) {
			}
			try {
				dest.delete();
			} catch (Exception ex) {
			}
			try {
				new File(testRoot + File.separator + dirName).delete();
			} catch (Exception ex) {
			}
		}

	}

	@Test
	public void testCreateFullPath() {
		Path testRoot = Paths.get(System.getProperty("java.io.tmpdir"),
				String.format("test_%d", new Date().getTime()));
		Path testPath = testRoot.resolve("sub1/sub2/sub3");
		try {
			FileUtils.createFullPath(testPath.toString());
			Assert.assertTrue(testPath.toFile().exists());

			try {
				FileUtils.createFullPath("/dev/test");
				fail("Exception expected but none thrown.");
			} catch (Exception ex) {
			}

		} catch (Exception ex) {
			fail(ex.toString());
		}

		finally {
			testPath.toFile().delete();
			testPath.getParent().toFile().delete();
			testPath.getParent().getParent().toFile().delete();
			testRoot.toFile().delete();
		}
	}

	@Test
	public void testGetFileName() {

		String path = String.format("folder%ssubfolder%s.hidden",
				File.separator, File.separator);
		String file = "some~file_with-a.strange.name";

		String res = FileUtils.getFileName(path + File.separator + file);
		Assert.assertEquals(res, file);

		res = FileUtils.getFileName(path + File.separator);
		Assert.assertEquals(res.length(), 0);

		res = FileUtils.getFileName(path);
		Assert.assertEquals(res.length(), 0);
	}

	@Test
	public void testGetPathName() {
		String path = String.format("a%sb%sc", File.separator, File.separator);
		String file = "some~file_with-a.strange.name";

		String res = FileUtils.getPathName(path + File.separator + file);
		Assert.assertEquals(res, path + File.separator);

		res = FileUtils.getPathName(path + File.separator);
		Assert.assertEquals(res, path + File.separator);

		res = FileUtils.getPathName(path);
		Assert.assertEquals(res, path + File.separator);
	}

	@Test
	public void testPath() {
		String sep = System.getProperty("file.separator");
		String path = FileUtils.path("a", "b", "c", "file.txt");
		Assert.assertEquals(path,
				String.format("a%sb%sc%sfile.txt", sep, sep, sep));
	}

}
