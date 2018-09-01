package de.ecconia.assembler.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileWrap
{
	private File file;
	
	public FileWrap(String filepath)
	{
		file = new File(filepath);
		System.out.println(file.getAbsolutePath());
	}
	
	public boolean exists()
	{
		return file.exists();
	}
	
	public List<String> lines()
	{
		ArrayList<String> lines = new ArrayList<>();
		
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
			
			String line = null;
			while((line = br.readLine()) != null)
			{
				lines.add(line);
			}
			
			br.close();
		}
		catch (FileNotFoundException e)
		{
			throw new FileNotFoundRException();
		}
		catch (IOException e)
		{
			throw new FileIORException();
		}
		
		return lines;
	}
	
	@SuppressWarnings("serial")
	private class FileNotFoundRException extends RuntimeException
	{
		public FileNotFoundRException()
		{
			super("File could not be found. File: " + file.getPath());
		}
	}
	
	@SuppressWarnings("serial")
	private class FileIORException extends RuntimeException
	{
		public FileIORException()
		{
			super("IO exception occured. File: " + file.getPath());
		}
	}
}
