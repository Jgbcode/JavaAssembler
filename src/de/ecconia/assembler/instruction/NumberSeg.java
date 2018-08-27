package de.ecconia.assembler.instruction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberSeg extends Segment
{
	public static final String bin = "([\\-+]?)([01]+)b";
	public static final String dec = "([\\-+]?)([0-9]+)";
	public static final String hex = "([\\-+]?)0x([0-9a-fA-F]+)";
	
	public NumberSeg(int bitWidth)
	{
		super(bitWidth);
	}
	
	@Override
	public void parse(String binary)
	{
		String sign;
		int number;
		
		if(binary.matches(hex))
		{
			Matcher m = Pattern.compile(hex).matcher(binary);
			m.matches();
			sign = m.group(1);
			number = Integer.parseInt(m.group(2), 16);
		}
		else if(binary.matches(bin))
		{
			Matcher m = Pattern.compile(bin).matcher(binary);
			m.matches();
			sign = m.group(1);
			number = Integer.parseInt(m.group(2), 2);
		}
		else if(binary.matches(dec))
		{
			Matcher m = Pattern.compile(dec).matcher(binary);
			m.matches();
			sign = m.group(1);
			number = Integer.parseInt(m.group(2), 10);
		}
		else
		{
			throw new InstructionParseException("Expected a number but received: " + binary);
		}
		
		if(sign.isEmpty())
		{
			super.parse(integerToUnsigned(number));
		}
		else
		{
			if(sign == "+")
			{
				super.parse(integerToSigned(number));
			}
			else
			{
				super.parse(integerToSigned(number * -1));
			}
		}
	}
}