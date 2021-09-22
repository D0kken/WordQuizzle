package wq;

import java.util.Comparator;

public class ClassificaComparator implements Comparator<String> {


	
	@Override
	public int compare(String a,String b) {
		
		return split(b,2).compareTo(split(a,2));
							
	}

		
	
	
	
				////////////////////////////////////
						//METODI DI SUPPORTO

	/**
	 * 
	 * @param mystring
	 * @param numParola
	 * @return numParola-sima parola di "mystring" 
	 */
	public static String split(String mystring ,int numParola) {
	
		String arr[] = mystring.split(" ");
		if(arr.length<numParola)
			return null;
						
		return arr[numParola-1];
	}




}//fine classe