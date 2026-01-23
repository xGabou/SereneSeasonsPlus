import sereneseasons.api.season.Season;
import java.util.Arrays;
public class PrintSubs {
  public static void main(String[] args) {
    System.out.println(Arrays.toString(Season.SubSeason.values()));
    for (Season.SubSeason s : Season.SubSeason.values()) {
      System.out.println(s.name()+":" + s.ordinal() + " -> " + s.getSeason());
    }
  }
}
