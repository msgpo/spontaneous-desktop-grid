package ee.ut.f2f.core;

/**
 * This exception is thrown if master task calls submitTasks() and the number of
 * new tasks to create is greater than provided peers.
 */
@SuppressWarnings("serial")
public class NotEnoughPeersException extends F2FComputingException
{
	NotEnoughPeersException(int tasks, int peers)
	{
		super("Wanted to submit "+tasks+" new tasks but provided only "+peers+" peers!");
	}
}
