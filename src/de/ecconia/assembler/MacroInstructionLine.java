package de.ecconia.assembler;

public class MacroInstructionLine extends InstructionLine {
	int macroLine;
	String macro;
	
	public MacroInstructionLine(String content, int line, String file, int macroLine, String macro) {
		super(content, line, file);	
		this.macroLine = macroLine;
		this.macro = macro;
	}

	@Override
	public String toString() {
		return super.toString() + " -> line " + macroLine + " of macro expansion of \"" + macro + "\"";
	}
}
