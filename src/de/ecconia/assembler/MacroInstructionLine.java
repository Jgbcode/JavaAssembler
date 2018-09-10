package de.ecconia.assembler;

public class MacroInstructionLine extends InstructionLine implements Cloneable {
	int macroLine;
	InstructionLine macro;
	
	public MacroInstructionLine(String content, int line, String file, int macroLine, InstructionLine macro) {
		super(content, line, file);	
		this.macroLine = macroLine;
		this.macro = macro;
	}
	
	public void setSplitRawContent(String[] content) {
		rawContent = "";
		
		if(!content[0].isEmpty())
			rawContent += content[0] + ":";
		
		if(content.length > 1) {
			rawContent += " " + content[1];
			for(int i = 2; i < content.length; i++)
				rawContent += ", " + content[i];
		}
	}

	@Override
	public String toString() {
		return super.toString() + " -> line " + macroLine + " of macro expansion\nat " + macro;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		MacroInstructionLine clone = (MacroInstructionLine)super.clone();
		clone.macro = (InstructionLine)macro.clone();
		return clone;
	}
}
