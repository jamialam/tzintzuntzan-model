package tzintzuntzan;


import uchicago.src.sim.network.DefaultDrawableNode;
import uchicago.src.sim.gui.OvalNetworkItem;

/** A node in a (social) network; refers to a person 
 *  or household.
 * 
 * @author Ruth Meyer
 *
 */
public class NetworkNode extends DefaultDrawableNode {
	public int id = -1;
	
	public NetworkNode(int newId){
		setId(newId);
		init();
	}
	
	public NetworkNode() {
		init();
	}
	
	private void init(){
		OvalNetworkItem d = new OvalNetworkItem(10,10);
		setDrawable(d);
	}
	
	
	public void setId(int newId){
		this.id = newId;
	}
	
	public int getID() {
		return this.id;
	}
	
	public void addInEdge(NetworkLink link){		
		super.addInEdge(link);
	}
	
	public void addOutEdge(NetworkLink link){		
		super.addOutEdge(link);
	}
	
	
	public String toString(){
		return "Node---" + this.label;
	}
	
	// needed for tests in contains() operation of node lists
	public boolean equals(Object o){
		if (this == o) return true;  // identity
		if ((o == null) || !(o instanceof NetworkNode)) return false; // null reference or not a network node
		NetworkNode otherNode = (NetworkNode)o;
		if (this.getId() != null && this.getId() == otherNode.getId()){
			return true; // refers to same agent
		}
		return false;
	}
}