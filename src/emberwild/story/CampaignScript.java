package emberwild.story;

/**
 * Implement this in a .java file dropped into the campaigns/ folder (or uploaded through the Campaign Forge)
 * and Emberwild will compile and load it at start-up. See campaigns/TheSunkenBell.java for a complete example.
 */
public interface CampaignScript {
    Campaign build();
}
