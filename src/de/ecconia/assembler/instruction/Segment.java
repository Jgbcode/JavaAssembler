package de.ecconia.assembler.instruction;

public class Segment
{
	private String binary;
	protected final int bitWidth;
	private boolean reversed = false;
	private boolean written = false;

	public Segment(int bitWidth)
	{
		this.bitWidth = bitWidth;
	}
	
	public void setReversed()
	{
		reversed = true;
	}
	
	public void parse(String binary)
	{
		this.binary = binary;
		written = true;
	}
	
//	public static void main(String[] args)
//	{
//		Segment s = new Segment(2);
//		System.out.println(s.integerToSigned(1));
//	}
	
	protected String integerToUnsigned(int i)
	{
		String binary = Integer.toBinaryString(i);
		
		if(binary.length() > bitWidth)
		{
			throw new InstructionParseException("Attempted to parse a " + binary.length() + " bit long number, while allowed bits are " + bitWidth);
		}
		while(binary.length() < bitWidth)
		{
			binary = '0' + binary;
		}
		
		return binary;
	}
	
	protected String integerToSigned(int i)
	{
		String binary = Integer.toBinaryString(i);
		
		boolean positive = true;
		if(i < 0)
		{
			positive = false;
		}
		
		if(positive)
		{
			if(binary.length() >= bitWidth)
			{
				throw new InstructionParseException("Attempted to parse a " + (binary.length()+1) + " bit long signed number, while allowed bits are " + bitWidth);
			}
			while(binary.length() < bitWidth)
			{
				binary = '0' + binary;
			}
		}
		else
		{
			System.out.println("Binary before: " + binary);
			while(binary.length() > 0 && binary.charAt(0) == '1')
			{
				binary = binary.substring(1);
			}
			binary = '1' + binary;
			System.out.println(" Binary after: " + binary);
			if(binary.length() > bitWidth)
			{
				throw new InstructionParseException("Attempted to parse a " + binary.length() + " bit long signed number, while allowed bits are " + bitWidth);
			}
			while(binary.length() < bitWidth)
			{
				binary = '1' + binary;
			}
		}
		
		return binary;
	}
	
	public boolean hasParsed()
	{
		return written;
	}

	@Override
	public String toString()
	{
		return reversed ? new StringBuilder(binary).reverse().toString() : binary;
	}
}
