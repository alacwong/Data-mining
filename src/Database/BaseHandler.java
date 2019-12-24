package Database;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Stack;
import ENUM.DICTIONARY;
import Exceptions.WordNotFoundException;
import Tree.MasterHead;
import Tree.WordNode;

public class BaseHandler {
  //TODO write a search function

  public TweetBase tweetbase;

  public BaseHandler(){
    tweetbase = new TweetBase();
  }

  public void Initalize(Connection con) {
    tweetbase.initialize(con);
  }

  public void insertWord(String text, int frequency, Connection con) {
    try {
      text = text.toLowerCase();
      tweetbase.InsertWord(text, con, frequency);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
  
  public void insertWord(String text, int frequency, Connection con, 
      HashMap<String, Integer> tableMap, ArrayList<String> orderedWords) {
    try {
      text = text.toLowerCase();
      tweetbase.InsertWord(text, con, frequency, tableMap, orderedWords);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public WordNode getWord(String text, Connection con) throws WordNotFoundException {
    text = text.toLowerCase();
    WordNode w = null;
    try {
      w = tweetbase.getWordNode(text, con);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    System.out.println(w.toString());
    return w;
  }

  public MasterHead getTree(Connection con) {
    try {
      return tweetbase.getMasterTree(con);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Returns word from entire tree search.
   * @param head head of entire tree
   * @param word word you are searching
   * @return database queery from tree
   * @throws WordNotFoundException 
   */
  public WordNode searchTree(MasterHead head, String word) throws WordNotFoundException {
    WordNode newHead = new WordNode("/" + word, 0);
    newHead.setNeighbors(head.get(word.charAt(0)).getNeighbors());
    if (word.length() > 1) {
      return searchTree(newHead, word, word.substring(1));
    } else {
      return newHead.getNeighbors().get(null);
    }
  }

  /**
   * Tree traversal
   * @param word
   * @param text
   * @param textRecurse
   * @return
   * @throws WordNotFoundException
   */
  private WordNode searchTree(WordNode word, String text, String textRecurse)
      throws WordNotFoundException {

    if ((text).equals(word.getText())) {
      return word;
    }
    if (textRecurse.length() > 1) {
      if (word.get(textRecurse.charAt(0)) == null) {
        throw new WordNotFoundException();
      } else {
        return searchTree(word.get(textRecurse.charAt(0)), text, textRecurse.substring(1));
      }
    } else {
      if (word.get(null) == null) {
        throw new WordNotFoundException();
      } else {
        return word.get(null);
      }
    }
  }

  public ArrayList<WordNode> treeTraversal(MasterHead head){
    ArrayList<WordNode> list = new ArrayList<WordNode>();
    for (DICTIONARY d: DICTIONARY.getAll()) {
      WordNode dict = head.get(d.getCharValue());
      for (Character c: dict.getNeighbors().keySet()) {
        try {
          treeTraversal(dict.get(c), list);
        } catch (WordNotFoundException e) {
          e.printStackTrace();
        }
      }
    }
    return list;
  }

  private void treeTraversal(WordNode node, ArrayList<WordNode> list) {

    list.add(node);

    for (Character c: node.getNeighbors().keySet()) {
      try {
        treeTraversal(node.get(c), list);
      } catch (WordNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Mins data from textfile.
   * @param filename name of file.
   */
  public void mineBook(String fileName, Connection con) {
    final long startTime = System.currentTimeMillis();
    HashMap<String, Integer> miniDb = new HashMap<String, Integer>();
    try {
      File book = new File(fileName);
      Scanner myReader = new Scanner(book);
      while (myReader.hasNextLine()) {
        Stack<String> words =  TweetBase.extractTweet(myReader.nextLine());
        for (String s: words) {
          if (miniDb.containsKey(s)) {
            miniDb.put(s, miniDb.get(s) + 1);
          } else {
            miniDb.put(s, 1);
          }
        }
      }
      myReader.close();
      for (String s: miniDb.keySet()) {
        insertWord(s, miniDb.get(s), con);
      }
   
    } catch (FileNotFoundException e) {
      System.out.println("File not found");
    } 
    final long endTime = System.currentTimeMillis();
    System.out.println(fileName + ": " + miniDb.size()  + " entries");
    System.out.println("Loaded " + fileName + " into twitter.db in " + getTime(endTime  - startTime));
  }

  public String getTime(long milliseconds)  {
    int seconds = (int) (milliseconds / 1000) % 60 ;
    int minutes = (int) ((milliseconds / (1000*60)) % 60);
    int hours   = (int) ((milliseconds / (1000*60*60)) % 24);
    if (hours > 0) {
      return hours + "h " + minutes + "m " + seconds + "s ";
    }
    else {
      return minutes + "m " + seconds + "s ";
    }
  }

  public Connection start(Connection con) {
    try {
      con = tweetbase.start(con);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return con;
  }
}
