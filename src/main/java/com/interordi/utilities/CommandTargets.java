package com.interordi.utilities;

import java.util.ArrayList;
import java.util.List;

public class CommandTargets {
	//Position of the argument
	public int position;

	//The list of targets
	public List< String > targets;


	public CommandTargets() {
		position = -1;
		targets = new ArrayList< String >();
	}
}