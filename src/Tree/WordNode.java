package Tree;
import java.util.ArrayList;
import java.util.HashMap;
import Exceptions.WordNotFoundException;

public class WordNode {
  
  private String text;
  private int frequency;
  private HashMap<Character, WordNode> neighbors;
  private HashMap<Character, Double> probabilityMap;
  
  public WordNode(String text, int frequency) {
    this.text = text;
    this.frequency = frequency;
    neighbors = new HashMap<Character, WordNode>();
    probabilityMap = new HashMap<Character, Double>();
  }
  
  public String getText() {
    return text;
  }
  
  public int getFrequnecy() {
    return frequency;
  }

  public void setNeighbors(HashMap<Character, WordNode>neighbors) {
    this.neighbors = neighbors;
  }
  
  public HashMap<Character, WordNode> getNeighbors(){
    return neighbors;
  }
  
  public WordNode get(Character c) throws WordNotFoundException {
    if (neighbors.get(c) != null) {
      return neighbors.get(c);
    } else {
      throw new WordNotFoundException();
    }
  }
  
  public ArrayList<WordNode> getNeighborList(){
    ArrayList<WordNode> list = new ArrayList<WordNode>();
    for (Character c: neighbors.keySet()) {
      list.add(neighbors.get(c));
    }
    return list;
  }
  
  public void setProbabilityMap(HashMap<Character, Double> probabilityMap) {
    this.probabilityMap = probabilityMap;
  }
  
  public HashMap<Character, Double> getProbabilityMap(){
    return probabilityMap;
  }
  
  @Override
  public String toString() {
    return "[Word: " + text + " Freqeuncy: " + frequency+ "]";
  }

}
