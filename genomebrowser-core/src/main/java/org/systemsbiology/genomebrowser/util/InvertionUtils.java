package org.systemsbiology.genomebrowser.util;

public class InvertionUtils {
	public static String inversion(String str){
		String result = "";
		String seqInvert = new StringBuffer(str).reverse().toString();
		for (int i = 0; i < seqInvert.length(); i++) {
			if (Character.toString(seqInvert.charAt(i)).equals("A") ){
				result = result + "T";
			}
			else if (Character.toString(seqInvert.charAt(i)).equals("T")){
				result = result + "A";
			}
			else if (Character.toString(seqInvert.charAt(i)).equals("C")){
				result = result + "G";
			}
			else if (Character.toString(seqInvert.charAt(i)).equals("G")){
				result = result + "C";
			}
		}
		return result; 
	}
}


