package de.ecconia.assembler;

public class CharCheck
{
	public static boolean isNumeric(char currentChar)
	{
		return '0' <= currentChar && currentChar <= '9';
	}
	
	public static boolean isBin(String test)
	{
		for(char c : test.toCharArray())
		{
			if('0' != c && c != '1')
			{
				return false;
			}
		}
		return true;
	}
	
	public static boolean isDec(String test)
	{
		for(char c : test.toCharArray())
		{
			if('0' > c || c < '9')
			{
				return false;
			}
		}
		return true;
	}
	
	public static boolean isHex(String test)
	{
		for(char c : test.toCharArray())
		{
			if(!(('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F')))
			{
				return false;
			}
		}
		return true;
	}
}
