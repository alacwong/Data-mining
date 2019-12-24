import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import Database.BaseHandler;
import Database.TweetBase;
import ENUM.DICTIONARY;
import ENUM.TWEETS;
import Exceptions.WordNotFoundException;
import Tree.MasterHead;
import Tree.WordNode;
import twitter4j.IDs;
import twitter4j.PagableResponseList;
import twitter4j.Relationship;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

//Planned features
//graphing feature, twitter trends
//messaging
//commands upon dm
//TODO graph the data, so write a gui?
//basically show distrbution from set of words and compare

//TODO fix code with underscore

//TODO messenger api? pick out most used words against 

//TODO sentence tree
//predictive 
//add twitter data to db
//keyword search algorithm, 

public class Main {

  public static void main(String [] args) throws TwitterException, SQLException {
    //Test printing tweet from user input
//    TwitterUser user = new TwitterUser();
//    tweetHandler(user);
//    handleDelete(user);
//    String text = "I am curry";
//    System.out.println(TweetBase.extractTweet(Arrays.toString(TweetBase.extractTweet(text))));
    Connection con = null;
    BaseHandler b = new BaseHandler();
    con = b.start(con);
    b.Initalize(con);
    b.mineBook("AliceInWonderLand", con);
    b.mineBook("PrideandPrejudice", con);
      //TODO
      //fix insertion, not inserting in new tables
    b.mineBook("CharlesDicken.txt", con);

  }

  /**
   * Handles tweeting messages from user input
   * @param user
   * @throws TwitterException 
   */
  private static void tweetHandler(TwitterUser user) throws TwitterException {
    Scanner input = new Scanner(System.in);
      String message = "";
    
    while(!message.equals("stop")) {
      System.out.println("Enter message to be tweeted");
      System.out.println("Enter stop to cancel");
      if (message.length() > 0) {
        user.tweet(message);
      }
      message = input.nextLine();
    }
  }

  /**
   * Handles tweet deleting
   * @param user
   * @throws TwitterException
   */
  private static void handleDelete(TwitterUser user) throws TwitterException {
    List<Status> tweets = user.getTweets(TWEETS.MYTWEETS);
    Scanner input = new Scanner(System.in);
    for (Status status: tweets) {
      System.out.println(user.shortTweet(status.getText()));
      System.out.println("Retweets: " + status.getRetweetCount() + "Favorites: " + status.getFavoriteCount());
      System.out.println("Tweeted on: " + status.getCreatedAt().toString());
      System.out.println("Delete Y/N?");
      String message = "";
      while(!message.toLowerCase().equals("y") && !message.toLowerCase().equals("n") ) {
        message = input.next();
        if (message.toLowerCase().equals("y")) {
          user.deleteTweet(status);
          break;
        } else if (message.toLowerCase().equals("n")) {
          break;
        } else {
          
        }
      }
    }
  }
}