package Tree;

import java.util.HashMap;

public class MasterHead {
  
  private  HashMap<Character, WordNode> dictionary;
  
  public MasterHead(HashMap<Character, WordNode> dictionary) {
    this.dictionary = dictionary;
  }
  
  public HashMap<Character, WordNode> getDictionary(){
    return dictionary;
  }

  public WordNode get(char charAt) {
    return dictionary.get(charAt);
  }

}
