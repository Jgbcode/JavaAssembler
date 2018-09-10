package de.ecconia.assembler.preprocessor;

import java.util.HashMap;

public class Context {
	public HashMap<String, Macro> current;
	public HashMap<String, Macro> previous;
	
	public Context(HashMap<String, Macro> previous) {
		this.current = new HashMap<String, Macro>();
		this.previous = previous;
	}
}
