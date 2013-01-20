package net.chrisdolan.pcgen.viewer.model;


import java.awt.Color;

import org.junit.Test;

public class Colors {

	@Test
	public void test() {
		c("yellow.darker", Color.yellow.darker());
		c("magenta", Color.magenta);
		c("red", Color.red);
		c("black", Color.black);
	}
	private void c(String name, Color c) {
		System.out.println(name + " - rgb=" + c.getRed() +","+c.getGreen()+","+c.getBlue());
	}

}
