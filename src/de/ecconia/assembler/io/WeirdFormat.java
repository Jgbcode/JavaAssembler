package de.ecconia.assembler.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class WeirdFormat
{
	private String key = "";
	private String value = null;
	private List<List<WeirdFormat>> groups = new ArrayList<>();
	
	public static WeirdFormat parse(List<String> lines) throws FileParseException
	{
		Stack<WeirdFormat> knots = new Stack<>();
		//root
		WeirdFormat root = new WeirdFormat("", null);
		knots.push(root);
		
		int isLevel = -1;
		
		boolean newGroup = false;
		
		for(int lineNumber = 0; lineNumber < lines.size(); lineNumber++)
		{
			String line = lines.get(lineNumber);
			
			int level = getLevel(line);
			
			String key = "";
			String value = null;
			{
				//Removed the tab's before line
				String content = line.substring(level);
				if(content.length() != content.trim().length())
				{
					throw new FileParseException("A value/key must not end with tab or space.", line);
				}
				
				{
					int firstSpace = content.indexOf(' ');
					if(firstSpace == -1)
					{
						key = content;
					}
					else
					{
						key = content.substring(0, firstSpace);
						value = content.substring(firstSpace+1);
					}
				}
			}
			
			//Empty line
			if(key.isEmpty())
			{
				if(level == 0)
				{
					if(newGroup)
					{
						throw new FileParseException("There must not be multiple empty lines in a row.", lineNumber);
					}
					newGroup = true;
					continue;
				}
				else
				{
					throw new FileParseException("An empty line may only contain 0 tabs or as many tabs as the previous line.", lineNumber);
				}
			}
			
			if(newGroup && level > isLevel)
			{
				throw new FileParseException("A new group cannot have/be a new parent.", lineNumber);
			}
			
			{
				int drop = isLevel - level;
				for(int i = 0; i < drop; i++)
				{
					isLevel--;
					knots.pop();
				}
			}
			
			if(newGroup)
			{
				knots.elementAt(knots.size()-2).newGroup();
			}
			
			if(level == isLevel)
			{
				knots.pop();
				WeirdFormat newWF = new WeirdFormat(key, value);
				knots.lastElement().addToLatestGroup(newWF);
				knots.push(newWF);
			}
			else if(level == isLevel+1)
			{
				isLevel++;
				WeirdFormat newWF = new WeirdFormat(key, value);
				knots.lastElement().addToLatestGroup(newWF);
				knots.push(newWF);
			}
			else
			{
				throw new FileParseException("No parent for this entry.", lineNumber);
			}
			
			newGroup = false;
		}
		
		return root;
	}
	
	private static int getLevel(String line) throws FileParseException
	{
		int level = 0;
		
		for(char c : line.toCharArray())
		{
			if(c == ' ')
			{
				throw new FileParseException("A line may start with '\\t's and must not have a space at the beginning of the key.", line);
			}
			else if(c == '\t')
			{
				level++;
			}
			else
			{
				return level;
			}
		}
		
		return level;
	}
	
	public WeirdFormat(String key, String value)
	{
		this.key = key;
		this.value = value;
	}

	private void newGroup()
	{
		groups.add(new ArrayList<WeirdFormat>());
	}
	
	private void addToLatestGroup(WeirdFormat newWF)
	{
		if(groups.size() == 0)
		{
			newGroup();
		}
		groups.get(groups.size()-1).add(newWF);
	}
	
	public String getKey()
	{
		return key;
	}
	
	public String getValue()
	{
		return value;
	}
	
	public int groupAmount()
	{
		return groups.size();
	}
	
	public boolean hasGroups()
	{
		return !groups.isEmpty();
	}
	
	public List<WeirdFormat> getGroup(int i)
	{
		return groups.get(i);
	}
	
	public void printStructure()
	{
		printStructure("");
	}
	
	private void printStructure(String prefix)
	{
		System.out.print(prefix + "[" + key + ":" + value + "]{");
		for(int i = 0; i < groups.size(); i++)
		{
			System.out.println("<");
				List<WeirdFormat> g = groups.get(i);
				for(int j = 0; j < g.size(); j++)
				{
					g.get(j).printStructure(prefix + "    ");
				}
			System.out.print(prefix + ">");
		}
		System.out.println("}");
	}
	
	public void printTree()
	{
		printTree("", "");
	}
	
	private void printTree(String headerPrefix, String mainPrefix)
	{
		System.out.println(headerPrefix + "" + key + ":" + value);
		
		for(int g = 0; g < groups.size(); g++)
		{
			boolean lastGroup = g == groups.size()-1;
			
			List<WeirdFormat> group = groups.get(g);
			for(int e = 0; e < group.size(); e++)
			{
				boolean lastElement = e == group.size() -1;
				
				String nextHeaderPrefix = mainPrefix;
				if(e == 0)
				{
					nextHeaderPrefix += lastGroup ? "└" : "├";
					nextHeaderPrefix += "─";
					nextHeaderPrefix += lastElement ? "─" : "┬";
				}
				else
				{
					nextHeaderPrefix += lastGroup ? " " : "│";
					nextHeaderPrefix += " ";
					nextHeaderPrefix += lastElement ? "└" : "├";
				}
				nextHeaderPrefix += "─";
				
				String nextMainPrefix = mainPrefix;
				nextMainPrefix += lastGroup   ? "  " : "│ ";
				nextMainPrefix += lastElement ? "  " : "│ ";
				
				group.get(e).printTree(nextHeaderPrefix, nextMainPrefix);
			}
			//TODO: One day proper linebreaks
//			System.out.println(mainPrefix + (lastGroup ? "  " : "│ "));
		}
	}
}
