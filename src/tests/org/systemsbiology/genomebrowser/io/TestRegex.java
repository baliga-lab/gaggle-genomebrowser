package org.systemsbiology.genomebrowser.io;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import static org.junit.Assert.*;


public class TestRegex {

	@Test
	public void test() {
		assertTrue(match(">name: chris"));
		assertTrue(match(">  name   : christopher bare   "));
		assertTrue(match(">  name\\:  : christopher bare   "));
		assertTrue(match(">   full name  :  james christopher bare   "));
	}

	public boolean match(String string) {
		Pattern property = BookmarkReader.property;
		Matcher m = property.matcher(string);
		if (!m.matches()) return false;
		System.out.println("\"" + m.group(1) + "\" = \"" + m.group(2) + "\"");
		return true;
	}

	@Test
	public void testBookmarkProcessing() {
		String a = "It's big\nIt's bad\nit's ugly\nand it's coming to get\t\t\t  you!";
		String target = "It's big\nIt's bad\nit's ugly\nand it's coming to get; you!";
		System.out.println(a);
		String b = a.replaceAll("\\n", "\\\\n").replaceAll("\\t\\s*", "; ");
		System.out.println(b);
		String c = b.replaceAll("\\\\n", "\n");
		System.out.println(c);
		assertEquals(target, c);
	}

	@Test
	public void testForwardRegex() {
		Pattern forwardRegex = Pattern.compile("(?i).*((forward)|(fwd)).*");
		
		assertEquals(false, forwardRegex.matcher("Genes").matches());
		assertEquals(true, forwardRegex.matcher("Transcript Signal Forward").matches());
		assertEquals(true, forwardRegex.matcher("  Segmentation Forward ").matches());
		assertEquals(true, forwardRegex.matcher(" Tiling array log ratio forward OD.0.2").matches());
		assertEquals(true, forwardRegex.matcher(" Tiling array log ratio forward").matches());
		assertEquals(true, forwardRegex.matcher("forward OD.0.2").matches());
	}
}
