package de.ecconia.assembler.instruction;

public class RegSeg extends Segment
{
	public RegSeg(int bitWidth)
	{
		super(bitWidth);
	}

	@Override
	public void parse(String binary)
	{
		if(binary.charAt(0) != 'r' || binary.length() == 1)
		{
			throw new InstructionParseException("Expected register address, got: " + binary);
		}
		String numberString = binary.substring(1);
		
		try
		{
			int number = Integer.parseInt(numberString);
			super.parse(integerToUnsigned(number));
		}
		catch (NumberFormatException e)
		{
			throw new InstructionParseException("Expected register address, got: " + binary);
		}
	}
}
