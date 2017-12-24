package com.playlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import jcifs.smb.SmbException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.AbstractID3v2;
import org.farng.mp3.id3.AbstractID3v2Frame;
import org.farng.mp3.id3.FrameBodyCOMM;

public class Main {

	private static final Logger logger = Logger.getLogger(Main.class);

	private static String MUSIC_HOME;
	private static String PLAYLISTS_OUTDIR;

	private static Integer fileCount = 0;

	public static void main(String[] args) throws MalformedURLException,
			SmbException {

		PropertyConfigurator.configure("log4j.properties");

		//MUSIC_HOME = args[0];
		//PLAYLISTS_OUTDIR = args[1];

		// Parcourir tous les fichiers audios contenu dans l'arborescence
		// MUSIC_HOME

		directoryIterator(MUSIC_HOME);

		logger.info("nombre de fichier traités: " + fileCount);

	}

	public static void directoryIterator(String directory)
			throws MalformedURLException, SmbException {

		// String user = "jluccisano:malili011004";
		// NtlmPasswordAuthentication auth = new
		// NtlmPasswordAuthentication(user);
		//
		// String path = "smb://192.168.0.32/Music/release/";
		//
		// SmbFile dir = new SmbFile(path,auth);
		File dir = new File("smb://192.168.0.32/Music/release/");
		// System.out.println(directory);

		if (dir.isDirectory()) {

			String s[] = dir.list();

			for (int i = 0; i < s.length; i++) {

				File dirTemp = new File(directory + "/" + s[i]);

				if (dirTemp.isDirectory()) {
					directoryIterator(directory + "/" + s[i]);
				}

				if (!dirTemp.isDirectory()) {

					int dotPos = dirTemp.getName().lastIndexOf(".");

					String extension = dirTemp.getName().substring(dotPos);

					if (extension.equals(".mp3")) {

						StringBuilder sb = new StringBuilder();

						sb.append("[" + fileCount + "] | ");
						sb.append("fichier : " + s[i] + " | ");
						fileCount++;
						getComment(dirTemp, sb);
					}

				}
			}
		}
	}

	private static void getComment(File currentFile, StringBuilder sb) {

		String comment = "";

		try {

			MP3File mp3file = new MP3File(currentFile);

			if (mp3file.hasID3v2Tag()) {

				AbstractID3v2 id3v2tag = mp3file.getID3v2Tag();

				comment = id3v2tag.getSongComment();
				AbstractID3v2Frame frame = id3v2tag.getFrame("COMM"
						+ ((char) 0) + "eng" + ((char) 0) + "");
				if (frame != null) {
					FrameBodyCOMM body = (FrameBodyCOMM) frame.getBody();
					sb.append(body.getLanguage() + " | ");
					sb.append(comment + " | ");
				} else {
					sb.append("NO COMMENT ENG" + " | ");
				}

				sb.append(id3v2tag.getAlbumTitle() + " | ");
				sb.append(id3v2tag.getSongTitle() + " | ");
				sb.append(id3v2tag.getYearReleased() + " | ");
				sb.append(id3v2tag.getSongGenre() + " | ");

				sb.append(mp3file.getID3v2Tag().getIdentifier() + " | ");

			}
			// if (mp3file.hasID3v1Tag()) {
			// comment = mp3file.getID3v1Tag().getComment();
			// sb.append(comment + " | ");
			// }
			//
			if (!comment.equals("") && !comment.equals("*")
					&& !comment.equals("-") && !comment.equals(" ")) {

				String[] playlists = comment.split(";");

				for (String playlistName : playlists) {

					// Enlever l'espace vide s'il y en a un
					String firstChar = playlistName.substring(0, 1);

					if (firstChar.equals(" ")) {
						playlistName = playlistName.substring(1);
					}

					try {
						boolean success = (new File(PLAYLISTS_OUTDIR + "/"
								+ playlistName)).mkdir();
						if (success) {
							sb.append("the directory" + PLAYLISTS_OUTDIR + "/"
									+ playlistName + " has been created"
									+ " | ");
						}
					} catch (Exception e) {// Catch exception if any
						logger.error("Error: " + e.getMessage());
					}

					copy(currentFile.getAbsolutePath(), PLAYLISTS_OUTDIR + "/"
							+ playlistName + "/" + currentFile.getName());
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			sb.append("ERROR: IOException |");
		} catch (TagException e) {
			// TODO Auto-generated catch block
			sb.append("ERROR: TagException |");
		} catch (StringIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			// logger.error("Error: " + e.getMessage());
			sb.append("ERROR: StringIndexOutOfBoundsException |");
		} catch (NegativeArraySizeException e) {
			// TODO Auto-generated catch block
			sb.append("ERROR: NegativeArraySizeException |");
		} catch (UnsupportedOperationException e) {
			// TODO Auto-generated catch block
			sb.append("ERROR: UnsupportedOperationException |");
		} finally {
			logger.info(sb.toString());
		}
	}

	public static void copy(String fromFileName, String toFileName)
			throws IOException {

		File fromFile = new File(fromFileName);
		File toFile = new File(toFileName);

		if (!fromFile.exists())
			throw new IOException("FileCopy: " + "no such source file: "
					+ fromFileName);
		if (!fromFile.isFile())
			throw new IOException("FileCopy: " + "can't copy directory: "
					+ fromFileName);
		if (!fromFile.canRead())
			throw new IOException("FileCopy: " + "source file is unreadable: "
					+ fromFileName);

		if (toFile.isDirectory())
			toFile = new File(toFile, fromFile.getName());

		if (toFile.exists()) {
			if (!toFile.canWrite())
				throw new IOException("FileCopy: "
						+ "destination file is unwriteable: " + toFileName);
			System.out.print("Overwrite existing file " + toFile.getName()
					+ "? (Y/N): ");
			System.out.flush();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					System.in));
			String response = in.readLine();
			if (!response.equals("Y") && !response.equals("y"))
				throw new IOException("FileCopy: "
						+ "existing file was not overwritten.");
		} else {
			String parent = toFile.getParent();
			if (parent == null)
				parent = System.getProperty("user.dir");
			File dir = new File(parent);
			if (!dir.exists())
				throw new IOException("FileCopy: "
						+ "destination directory doesn't exist: " + parent);
			if (dir.isFile())
				throw new IOException("FileCopy: "
						+ "destination is not a directory: " + parent);
			if (!dir.canWrite())
				throw new IOException("FileCopy: "
						+ "destination directory is unwriteable: " + parent);
		}

		FileInputStream from = null;
		FileOutputStream to = null;
		try {
			from = new FileInputStream(fromFile);
			to = new FileOutputStream(toFile);
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = from.read(buffer)) != -1)
				to.write(buffer, 0, bytesRead); // write
		} finally {
			if (from != null)
				try {
					from.close();
				} catch (IOException e) {
					;
				}
			if (to != null)
				try {
					to.close();
				} catch (IOException e) {
					;
				}
		}
	}

}
