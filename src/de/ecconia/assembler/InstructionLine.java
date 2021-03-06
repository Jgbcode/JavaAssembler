package de.ecconia.assembler;

import java.util.ArrayList;

import de.ecconia.assembler.io.FileParseException;
import de.ecconia.assembler.preprocessor.StringHelper;

public class InstructionLine implements Cloneable
{
	protected String rawContent;
	private final int line;
	private final String file;
	
	private String label;
	private String content;
	
	private Integer address;
	
	public InstructionLine(String content, int line, String file)
	{
		this.rawContent = content;
		this.line = line;
		this.file = file;
		
		int commentStart = content.indexOf(';');
		if(commentStart != -1)
		{
			content = content.substring(0, commentStart);
		}
		
		int labelIndex = StringHelper.indexOfColon(content);
		if(labelIndex != -1) {
			label = content.substring(0, labelIndex).trim();
			content = content.substring(labelIndex + 1).trim();
		}
		else
			label = "";
		
		this.content = content.trim();
	}
	
	public boolean hasContent()
	{
		return !(content.isEmpty());
	}
	
	public void setContent(String content)
	{
		this.content = content.trim();
	}
	
	public String getRawContent()
	{
		return rawContent;
	}
	
	public String getLabel()
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
		return file + ":" + line + " : " + rawContent;
	}

	public int getLineNumber()
	{
		return line;
	}
	
	public String getFile() 
	{
		return file;
	}
	
	public void setSplitContent(String[] split) {
		if(split.length == 0) {
			label = "";
			content = "";
		}
		else if(split.length == 1) {
			label = split[0];
			content = "";
		}
		else {
			label = split[0];
			content = split[1];
		}
		
		for(int i = 2; i < split.length; i++) {
			content += " " + split[i] + ",";
		}
		
		if(split.length > 2)
			content = content.substring(0, content.length() - 1);
	}
	
	// Splits format: label: opcode arg1, arg2, arg3
	// Result String[] {label, opcode, arg1, arg2, arg3}
	public String[] splitOnCommas(int k) throws FileParseException {
		if(content.isEmpty())
			return new String[]{label, content};
		
		try {
			ArrayList<String> split = new ArrayList<String>();
			split.add(label);
			
			int i = 0;
			while(i < content.length() && !Character.isWhitespace(content.charAt(i)))
				i++;
			
			split.add(content.substring(0, i));
			split.addAll(StringHelper.splitOn(content.substring(i), ','));
			
			int resultSize = (k > 0) ? k : split.size();
			String[] result = new String[resultSize];
			int j;
			for(j = 0; j != k && j < split.size(); j++)
				result[j] = split.get(j);
			
			for(; j < split.size(); j++)
				result[k] += " " + split.get(j);
			
			return result;
		}
		catch(Exception e) {
			throw new FileParseException("Error parsing line", this);
		}
	}
	
	// Splits format: label: opcode arg1 arg2 arg3
	// Result String[] {label, opcode, arg1, arg2, arg3}
	public String[] splitOnSpaces(int i) throws FileParseException {
		if(content.isEmpty())
			return new String[]{label, content};
		
		try {
			ArrayList<String> split = new ArrayList<String>();
			split.add(label);
			
			split.addAll(StringHelper.splitOnWhitespace(content.trim()));
			
			int resultSize = (i > 0) ? Math.min(i, split.size()) : split.size();
			String[] result = new String[resultSize];
			int j;
			for(j = 0; j != i && j < split.size(); j++)
				result[j] = split.get(j);
			
			for(; j < split.size(); j++)
				result[resultSize - 1] += " " + split.get(j);
			
			return result;
		}
		catch(Exception e) {
			throw new FileParseException("Error parsing line", this);
		}
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		InstructionLine clone = (InstructionLine)super.clone();
		return clone;
	}
}
