package hadar.iago.agent;

import java.util.ArrayList;

import edu.usc.ict.iago.utils.BehaviorPolicy;
import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;

public class RepeatedFavorBehavior extends IAGOCoreBehavior implements BehaviorPolicy {
		
	private AgentUtilsExtension utils;
	private GameSpec game;	
	private Offer allocated;
	private LedgerBehavior lb = LedgerBehavior.NONE;
	private int adverseEvents = 0;
	
	public enum LedgerBehavior
	{
		FAIR,
		LIMITED,
		BETRAYING,
		NONE;
	}
	
	public RepeatedFavorBehavior (LedgerBehavior lb)
	{
		super();
		this.lb = lb;
	}
		
	@Override
	protected void setUtils(AgentUtilsExtension utils)
	{
		this.utils = utils;
		
		this.game = this.utils.getSpec();
		allocated = new Offer(game.getNumberIssues());
		for(int i = 0; i < game.getNumberIssues(); i++)
		{
			int[] init = {0, game.getIssueQuantities().get(i), 0};
			allocated.setItem(i, init);
		}
	}
	
	@Override
	protected void updateAllocated (Offer update)
	{
		allocated = update;
	}
	
	@Override
	protected void updateAdverseEvents (int change)
	{
		adverseEvents = Math.max(0, adverseEvents + change);
	}
	
	
	@Override
	protected Offer getAllocated ()
	{
		return allocated;
	}
	
	@Override
	protected Offer getConceded ()
	{
		return allocated;
	}
	
	@Override
	protected Offer getFinalOffer(History history)
	{
		Offer propose = new Offer(game.getNumberIssues());
		int totalFree = 0;
		do 
		{
			totalFree = 0;
			for(int issue = 0; issue < game.getNumberIssues(); issue++)
			{
				totalFree += allocated.getItem(issue)[1]; // adds up middle row of board, calculate unclaimed items
			}
			propose = getNextOffer(history);
			updateAllocated(propose);
		} while(totalFree > 0); // Continue calling getNextOffer while there are still items left unclaimed
		return propose;
	}

	@Override
	public Offer getNextOffer(History history) 
	{	
			
		//start from where we currently have accepted
		Offer propose = new Offer(game.getNumberIssues());
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
			propose.setItem(issue, allocated.getItem(issue));
		
		
		// Assign ordering to the player based on perceived preferences. Ideally, they would be opposite the agent's (integrative)
		ArrayList<Integer> playerPref = utils.getMinimaxOrdering(); 
		ArrayList<Integer> vhPref = utils.getMyOrdering();
		
		// Array representing the middle of the board (undecided items)
		int[] free = new int[game.getNumberIssues()];
		
		for(int issue = 0; issue < game.getNumberIssues(); issue++)
		{
			free[issue] = allocated.getItem(issue)[1];
		}
	
		int userFave = -1;
		int userSecFave = -1;
		int opponentFave = -1;
		int opponentSecFave = -1;
		
		// Find most valued issue for player and VH (of the issues that have undeclared items)
		int max = game.getNumberIssues() + 1;
		for(int i  = 0; i < game.getNumberIssues(); i++)
			if(free[i] > 0 && playerPref.get(i) < max)
			{
				userFave = i;
				userSecFave = max;
				max = playerPref.get(i);
			}
		max = game.getNumberIssues() + 1;
		for(int i  = 0; i < game.getNumberIssues(); i++)
			if(free[i] > 0 && vhPref.get(i) < max)
			{
				opponentFave = i;
				opponentSecFave = i;
				max = vhPref.get(i);
			}
		
		
		if (utils.getVerbalLedger() < 0) //we have favors to cash!
		{
			//we will naively cash them immediately regardless of game importance
			//take entire category
			utils.modifyOfferLedger(-1);
			propose.setItem(opponentFave, new int[] {allocated.getItem(opponentFave)[0] + free[opponentFave], 0, allocated.getItem(opponentFave)[2]});
			return propose;	
		}
		else if (utils.getVerbalLedger() > 0) //we have favors to return!
		{
			if(lb == LedgerBehavior.FAIR)//this agent returns an entire column!
			{
				//return entire category
				utils.modifyOfferLedger(1);
				propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0], 0, allocated.getItem(userFave)[2] + free[userFave]});
				return propose;
			}
		}

		if (userFave == -1 && opponentFave == -1) // We already have a full offer (no undecided items), try something different
		{
			//just repeat and keep allocated
		}			
		else if(userFave == opponentFave)// Both agent and player want the same issue most
		{
			if(free[userFave] >= 2) // If there are more than two of that issue, propose an offer where the VH and player each get one more of that issue
				propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0] + 1, free[userFave] - 2, allocated.getItem(userFave)[2] + 1});
			else if(free[userSecFave] >=2)//Otherwise, give the other side his 1 of his fav in exchange to two of our next fav INFO: naive would just give the one item left to us, the agent
			{
				if (utils.adversaryRow == 2) {
					propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0], free[userFave] - 1, allocated.getItem(userFave)[2] + 1});
					propose.setItem(userSecFave, new int[] {allocated.getItem(userSecFave)[0]+2, free[userSecFave] - 2, allocated.getItem(userSecFave)[2]});
				} else if (utils.adversaryRow == 0) {
					propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0] + 1, free[userFave] - 1, allocated.getItem(userFave)[2]});
					propose.setItem(userSecFave, new int[] {allocated.getItem(userSecFave)[0], free[userSecFave] - 2, allocated.getItem(userSecFave)[2] +2});
				}
			}
			else if(free[opponentSecFave] >=2)//Otherwise, give our agent 1 of his fav in exchange to two of our opponent next fav INFO: naive would just give the one item left to us, the agent
			{
				if (utils.adversaryRow == 2) {
					propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0] + 1, free[userFave] - 1, allocated.getItem(userFave)[2]});
					propose.setItem(opponentSecFave, new int[] {allocated.getItem(opponentSecFave)[0], free[opponentSecFave] - 2, allocated.getItem(opponentSecFave)[2]+2});
				} else if (utils.adversaryRow == 0) {
					propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0], free[userFave] - 1, allocated.getItem(userFave)[2] + 1});
					propose.setItem(opponentSecFave, new int[] {allocated.getItem(opponentSecFave)[0] +2, free[opponentSecFave] - 2, allocated.getItem(opponentSecFave)[2]});
				}
			}
		}
		else // If the agent and player have different top picks
		{
			// Give both the VH and the player all of the item they want most INFO: the naive agent would only give one here and would ignore what rows is his
			if (utils.adversaryRow == 2) {
				propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0] + free[userFave],  0, allocated.getItem(userFave)[2]});
				propose.setItem(opponentFave, new int[] {allocated.getItem(opponentFave)[0],  0, allocated.getItem(opponentFave)[2] + free[opponentFave]});
			} else if (utils.adversaryRow == 0) {
				propose.setItem(userFave, new int[] {allocated.getItem(userFave)[0],  0, allocated.getItem(userFave)[2] + free[userFave]});
				propose.setItem(opponentFave, new int[] {allocated.getItem(opponentFave)[0] + free[opponentFave],  0, allocated.getItem(opponentFave)[2]});
			}
			
		}
		
		return propose;
	}

	@Override
	protected Offer getTimingOffer(History history) {
		return null;
	}

	@Override
	protected Offer getAcceptOfferFollowup(History history) {
		return null;
	}
	
	@Override
	protected Offer getFirstOffer(History history) {
		return getNextOffer(history);
	}

	@Override
	protected int getAcceptMargin() {
		return Math.max(0, Math.min(game.getNumberIssues(), adverseEvents));//basic decaying will, starts with fair
	}

	@Override
	protected Offer getRejectOfferFollowup(History history) {
		return null;
	}
	

}
