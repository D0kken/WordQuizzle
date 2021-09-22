package wq;


public class Arco {

	
protected String node1, node2;	//nomi degli utenti legati da questo arco 



/**
 * Costruttore senza argomenti
 */
public Arco() {
 
	this.node1 = this.node2 = null;

}



/**
 * Costruttore con argomenti
 * 
 * @param x1 primo nodo
 * @param y1 secondo nodo
 */
public Arco(String x1, String y1) {
  
	this.node1 = x1;
	this.node2 = y1;
 
}



/**
 * 
 * @return il primo nodo
 */
public String getNode1() { 
	
	return this.node1; 
}



/**
 * 
 * @return il secondo nodo
 */
public String getNode2() { 

	return this.node2;
}



   
}
