package wq;

import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

public class GrafoAmici {
	
											//VARIABILI
	  

	
	private  ConcurrentHashMap<String,Allegato> nodi;	//struttura dati che associa ad ogni nome utente, un insieme di informazioni associate

	 							///////////////////////////////////////
											//COSTRUTTORI
	/**
	   * Costruttore senza argomenti
	   */
	  public GrafoAmici() {
	    
		  this.nodi = new ConcurrentHashMap<String,Allegato>();

	  }
	  
	  
		 /**
	   * Costruttore con argomenti
	   */
	
	  public GrafoAmici(ConcurrentHashMap<String,Allegato> x) {
	 
		  this.nodi = x;
	  }
	  						 	///////////////////////////////////////
	  										//METODI
	 
	 
	  		/**
	  		 * ritorna la concurrent hash map che contiene le informazioni sui client
	  		 */
	  		public ConcurrentHashMap<String,Allegato> getMap() {
			
	  				return this.nodi;
			}
	  	
	  	
	  		
			  
			
			  /**
			   * verifica identità e login
			   * 
			   * @return Stringa contenente l'esito dell'operazione
			   */
			  public String loginUtente(String nome,String pass,SelectionKey key,Integer port)  
			  {	
				  	
					  String esito="Errore";
					 
					  if(pass==null)
					
						  return esito;
					 
					  if(this.nodi.containsKey(nome)) 
					  {
						
						  Allegato allegato=this.nodi.get(nome);
						  	
									
						  if(allegato.getPass().compareTo(pass)==0) 
						  {		
							  allegato.setConnectionState(true);	
							  allegato.setPorta(port);
							  allegato.setKey(key);
							  esito="Connesso";
						  }
						  
						  else
							  esito="Password errata";
						
					  }
					  
					  else 
						  esito="Non registrato";
						
					  return esito;
					  
				}//fine loginUtente
	  
			  
			  
			  /**
			   * verifica identità e logout
			   * 
			   * @return true se va a buon fine, false altrimenti
			   */
			  public boolean logoutUtente(String nome,ConcurrentHashMap<SelectionKey, String>  map,SelectionKey key)   
			  {	
				  	boolean bool=false;		
					Allegato allegato=this.nodi.get(nome);
				
					
					if(this.nodi.containsKey(nome)) 
					 	{
							allegato.setConnectionState(false);	
							map.remove(key);
							bool=true;
					 	} 
					 									
					return bool;
				}//fine logoutUtente
	  
			  
			  
			  
			  /**
			   * 
			   * @return  true se arco di amicizia è stato aggiunto, false altrimenti 		
			   */
			  public boolean aggiungiAmico(String nome,String amico)   
			  {	
				
					  boolean bool=false;
					  
					  if(!this.isAmico(nome, amico))
					  				bool=this.add(nome,amico);
					  
					  return bool;
			  }
	  
			  

			  
	
			/*
			 * aggiorna valore del punteggio di "nome"
			 */
			  public void setPunti(String nome,Integer punt)
			  {
				  
			  if(this.nodi.containsKey(nome) && punt!=null) 
				  
				  this.nodi.get(nome).setPunteggio(punt);
			 			  
			  }//fine setPunti
			  
			  
			  
			  
			  
			  /**
			   * @param nome
			   * @return punteggio di "nome" come stringa
			   */
			  public String getPunteggio(String nome)
			  {
				  int punteggio=this.getMap().get(nome).getPunteggio();
				  return Integer.toString(punteggio);
				  
			  }
			  
		
			  /**
			    *   
			    * @return stringa json che rappresenta la classifica degli amici di "nome" compreso egli stesso   
				*/
			  public String[] getClassifica(String nome) {
		
				    String[] array= new String[this.nodi.get(nome).getnAmici()+1];
					Arco arco=null;
					array[0]=nome;
					int i=1;
					Set<Arco> set= this.getEdgeSet(nome);
					Iterator<Arco> iterator = set.iterator();

					  while(iterator.hasNext()) 
					  	{
						
						  arco=iterator.next();
						  if(( nome.compareTo(arco.node1.toString())==0)) 		
				 				
							  array[i]=arco.node2.toString();  
						   
						  else 
						  					  				  
							  array[i]=arco.node1.toString();
						  
						  i++;
	
					  	}
				
					  for(i=0;i<array.length;i++)
					
						  array[i]=array[i]+" "+this.getMap().get(array[i]).getPunteggio();
				
				    Arrays.sort(array, new ClassificaComparator());
				  
				    return array;
	  
				  }//fine  getClassifica
				  
				
					  
			  
			  
			  
	       /**
	        * restituisce il numero di archi 
	        * 
	        * @return il numero di nodi
	        */
	  	   public int getNodes()  {
	   		
	  			return this.nodi.size();
	  		}
	  	
	  
	  	
  
		  /**
		   * aggiunge un nodo al grafo con valore x se x non e' presente nel grafo
		   * L'aggiunta di un nodo significa aggiungere la coppia (x, a) nella HashMap
		   * dove a e' una struttura contenente informazioni su "x".
		   * 
		   * @param x: il nodo (utente) da aggiungere al grafo
		   * @param a: informazioni sull'utente "x"
		   * @param address: indirizzo dell'utente "x"
		   * @param porta: porta udp dell'utente "x"
		   */
		  public boolean addNodo(String x,Allegato a,InetAddress address,Integer porta)  {
	
			  boolean bool=false;
			  if(!(this.nodi.containsKey(x))) {	
		      Set<Arco> lista = new HashSet<Arco>();
		      a.setArco(lista);
		      a.setAddress(address);
		      a.setPorta(porta);
		      
		      this.nodi.putIfAbsent(x, a);
		      bool=true;
		    }
		 return bool;
		  }//fine addNodo
		  

		  

		  /**
		   * aggiunge un arco tra gli utenti "nome" e "amico" se tale arco non e' gia' presente
		   *
		   * @param x: primo nodo dell'arco
		   * @param y: secondo nodo dell'arco
		   * @return vero se l'arco e' stato aggiunto false altrimenti
		   */
		  public boolean add(String nome, String amico){
		    boolean flag = false, flag1 = false;
		    
		    if (!this.nodi.containsKey(nome))
		    
		    	addNodo(nome,new Allegato(),null,null);
		    
		    if (!this.nodi.containsKey(amico))
		    
		    	addNodo(amico,new Allegato(),null,null);
		    
		    Arco a = new Arco(nome,amico);
		    
		    flag = (this.nodi.get(nome).getSetArco() ).add(a);
		    flag1 =(this.nodi.get(amico).getSetArco() ).add(a);
		    flag = flag && flag1;

		    return flag;
		  }//fine add
		  
		  
		  

		  
		  /**
		   * @param a: l'arco da aggiungere
		   * @return true se l'arco e' stato aggiunto, false altrimenti
		   */
		  public boolean add(Arco a)  {
		   
			  return add(a.getNode1(),a.getNode2());
		 
		  }//fine add
	



		  /**
		   *
		   * 
		   * @return l'insieme di tutti gli archi presenti nel grafo
		   */   
		  public Set<Arco> getEdgeSet() {
		   
			  Set<Arco> setArchi = new HashSet<Arco>();
		    
			  Iterator<Allegato> hashSetI =this.nodi.values().iterator();

			  while (hashSetI.hasNext())
				  setArchi.addAll(hashSetI.next().getSetArco());
		   
			  return setArchi;
		  }//fine getEdgeSet

		 
		  
		  /**
		   * @param nome: nodo di cui si vuole conoscere l'insieme di archi incidenti
		   * @return l'insieme di archi incidenti sul nodo "nome", null altrimenti
		   *
		   */
		  public Set<Arco> getEdgeSet(Object nome) {
		   
			  if (this.nodi.containsKey(nome))
				 
				  return this.nodi.get(nome).getSetArco();
			 
			  else
				  return null;
		  }//fine getEdgeSet

		  
		  /**
		   * 
		   * @param nome: nomed utente di cui voglio la lista amici
		   * @return lista amici di "nome"
		   */
		  
		  public String[] getListaAmici(String nome) {
			 
			  if(this.nodi.containsKey(nome) && (this.nodi.get(nome).getnAmici()>0))
			 {
			  String[] array= new String[this.nodi.get(nome).getnAmici()];
					
			  Arco arco=null;
			  int i=0;
			  
			  Set<Arco> set= this.getEdgeSet(nome);
			 
			  Iterator<Arco> iterator = set.iterator();

			 
			  while(iterator.hasNext()) 
			  	{
				
				  arco=iterator.next();
				  
				  if(( ((String)nome).compareTo(arco.node1.toString())==0)) 		
				 				
					  array[i]=arco.node2.toString();  
				   
				  else 
				  					  				  
					  array[i]=arco.node1.toString();
				  
									
				  i++;
			  	}
			  return array;
			 } 		  
		   return null;
		  }		  
		  
		  
	
		  
		  
		  /**
		   * @return l'insieme di nodi del grafo
		   */ 
		  public Set<String> getNodeSet()  {
		    return this.nodi.keySet();
		  }



			/**
			 * 	@param nome: utente
			 *  @return true se "nome" è connesso, false altrimenti
			 */
		  public boolean isConnected(String nome) {
			  
			  		Allegato allegato=this.nodi.get(nome);
			  
			  return allegato.getConnectionState();
		  }
		  

		  
		  /**
		   * @param nome: utente
		   * @param amico: utente
		   * @return true se "nome" e "amico" sono amici, false altrimenti
		   */
		  public boolean isAmico(String nome,String amico) {
			  
			  int i;
			  boolean bool=false;
			
			  if(this.getMap().get(nome).getnAmici()>0) 
			 {
				 String[] array=this.getListaAmici(nome);
				 for (i = 0; i < array.length; i++)
			  		{
					 if(array[i].compareTo(amico)==0)
					 	{
						 bool=true;
						 break;
					 	}
			  		}
			 }
				 return bool;
		  }//fine isAmico
		  


}

