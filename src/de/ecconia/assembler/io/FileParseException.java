package de.ecconia.assembler.io;

import de.ecconia.assembler.InstructionLine;

@SuppressWarnings("serial")
public class FileParseException extends Exception
{
	public FileParseException(String message)
	{
		super(message);
	}
	
	public FileParseException(String message, String line)
	{
		super(message + " Line: >" + line + "<");
	}
	
	public FileParseException(String message, int linenumber)
	{
		super(message + " At line: " + (linenumber-1));
	}
	
	public FileParseException(String message, InstructionLine line) {
		super(message + "\nat " + line);
	}
}