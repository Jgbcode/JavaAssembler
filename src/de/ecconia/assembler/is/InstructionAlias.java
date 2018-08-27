package de.ecconia.assembler.is;

public class InstructionAlias
{
	private String opcode;
	private String operation;
	private InstructionFormat format;
	
	public InstructionAlias(String opcode, String operation, InstructionFormat format)
	{
		this.opcode = opcode;
		this.operation = operation;
		this.format = format;
	}
	
	public InstructionFormat getFormat()
	{
		return format;
	}
	
	public String getOpcode()
	{
		return opcode;
	}
	
	public String getOperation()
	{
		return operation;
	}
}
