package de.ecconia.assembler;

public class InstructionLine
{
	private final String rawContent;
	private final int line;
	
	private final boolean label;
	private final String content;
	
	private Integer address;
	
	public InstructionLine(String content, int line)
	{
		this.rawContent = content;
		this.line = line;
		
		//If first character is not a tab the instruction has to be a label;
		label = !content.startsWith("\t");
		
		int commentStart = content.indexOf(';');
		if(commentStart != -1)
		{
			content = content.substring(0, commentStart);
		}
		
		this.content = content.trim();
	}
	
	public boolean hasContent()
	{
		return !content.isEmpty();
	}
	
	public String getRawContent()
	{
		return rawContent;
	}
	
	public boolean isLabel()
	{
		return label;
	}

	public void setAddress(int i)
	{
		address = i;
	}
	
	public Integer getAddress()
	{
		return address;
	}
	
	public String getContent()
	{
		return content;
	}
	
	@Override
	public String toString()
	{
		return "Line " + line + ": " + rawContent;
	}

	public int getLineNumber()
	{
		return line;
	}
}
