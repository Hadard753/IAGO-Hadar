package hadar.iago.agent;

import javax.websocket.Session;

import edu.usc.ict.iago.utils.GameSpec;


/**
 * @author Hadar
 * 
 */
public class IAGOHadarVH extends IAGOCoreVH {

	/**
	 * @author Hadar
	 * Instantiates a new  VH.
	 *
	 * @param name: agent's name
	 * @param game: gamespec value
	 * @param session: the session
	 */
	public IAGOHadarVH(String name, GameSpec game, Session session)
	{
		super("HadarFavor", game, session, new RepeatedFavorBehavior(RepeatedFavorBehavior.LedgerBehavior.LIMITED), new RepeatedFavorExpression(), 
				new RepeatedFavorMessage(false, false, RepeatedFavorBehavior.LedgerBehavior.LIMITED, game));	
		
		super.safeForMultiAgent = true;
	}

	@Override
	public String getArtName() {
		return "Brad";
	}

	@Override
	public String agentDescription() {
			return "<h1>Richard</h1><p>I believe in a win win negotation!</p>";
	}
}