package ee.ut.f2f.mpi.examples.games;

import java.util.Random;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;

public class Moving extends MPITask {

	private static final int MAX_STEPS = 30;
	private static final int SIZE = 3;

	/**
	 * An example of using MPI commands. The example is in a form of a game. Master is the server and peers are players. Every turn a player moves. If two players are on the same square then one of
	 * them must go. Game ends when there is only one player left or the time is up.
	 */
	public void runTask() {
		getMPIDebug().println("Starting MPI commands example game");
		int rank, size;
		MPI().Init();
		size = MPI().COMM_WORLD().Size();// Number of players + 1
		rank = MPI().COMM_WORLD().Rank();
		Object[] buf = new Object[1];
		Random rnd = new Random();
		int[] dead = new int[size];
		for (int i = 0; i < size; i++) {
			dead[i] = 0;
		}
		int h = 0;
		int alive = size - 1;
		if (rank == 0) {// Master (Server)
			getMPIDebug().println("Number of players : " + (size - 1));
			int[][] cor = new int[size][2];
			// Sets the starting coordinates of players
			for (int i = 0; i < size; i++) {
				cor[i][0] = rnd.nextInt(SIZE);
				cor[i][1] = rnd.nextInt(SIZE);
				getMPIDebug().println("Player " + i + " kordinates: " + cor[i][0] + " " + cor[i][1]);
			}
			// Each player gets its coordinates
			MPI().COMM_WORLD().Scatter(cor, 0, 1, MPI.OBJECT, buf, 0, 1, MPI.OBJECT, 0);
			while (alive > 1 && h < MAX_STEPS) {
				h++;
				getMPIDebug().println("Starting Round " + h + " remaining players " + alive);
				// Gather the coordinates where the players have moved
				MPI().COMM_WORLD().Gather(buf, 0, 1, MPI.OBJECT, cor, 0, 1, MPI.OBJECT, 0);
				for (int i = 1; i < size; i++) {
					if (dead[i] == 0) {
						getMPIDebug().println("Player " + i + " stept to kordinates: " + cor[i][0] + " " + cor[i][1]);
					}
				}
				// Check if two players are on the same square. If they are then remove one.
				for (int i = 1; i < size; i++) {
					for (int j = i + 1; j < size; j++) {
						if (dead[i] == 0 && dead[j] == 0 && cor[i][0] == cor[j][0] && cor[i][1] == cor[j][1]) {
							getMPIDebug().println("Players " + i + " and " + j + " are on same square, one of them must go");
							dead[(Math.random() > 0.5 ? i : j)] = 1;
						}
					}
				}
				// Announce the results of the removal
				MPI().COMM_WORLD().Bcast(dead, 0, dead.length, MPI.INT, 0);
				alive = 0;
				for (int i = 1; i < size; i++) {
					if (dead[i] == 0) {
						alive += 1;
					}
				}
			}
			// If the fame is over then show the result
			getMPIDebug().println("The game is over");
			for (int i = 1; i < size; i++) {
				if (dead[i] == 0) {
					getMPIDebug().println("Player " + i + " survived");
				}
			}
		} else {// Slaves (Players)
			// Get starting coordinates from Server
			MPI().COMM_WORLD().Scatter(null, 0, 1, MPI.OBJECT, buf, 0, 1, MPI.OBJECT, 0);
			while (alive > 1 && h < MAX_STEPS) {
				h++;
				if (dead[rank] == 0) {
					// Get new coordinates for the next move
					int[] mycor = (int[]) buf[0];
					getMPIDebug().println("Round " + h + " Player " + rank + " kordinates: " + mycor[0] + " " + mycor[1]);
					for (int i = 0; i < 2; i++) {
						mycor[i] += rnd.nextInt(3) - 1;
						if (mycor[i] < 0)
							mycor[i] = 0;
						if (mycor[i] >= SIZE)
							mycor[i] = SIZE - 1;
					}
				}
				// Send new coordinates to Server
				MPI().COMM_WORLD().Gather(buf, 0, 1, MPI.OBJECT, null, 0, 1, MPI.OBJECT, 0);
				int state = dead[rank];
				// Get the information that Who is dead from Server
				MPI().COMM_WORLD().Bcast(dead, 0, dead.length, MPI.INT, 0);
				// Show the statuses of the game
				if (state == 0 && dead[rank] == 1) {
					getMPIDebug().println("I lost");
				}
				alive = 0;
				for (int i = 1; i < size; i++) {
					if (dead[i] == 0) {
						alive += 1;
					}
				}
				if (alive == 1) {
					getMPIDebug().println("Round " + h + " Game over");
					if (dead[rank] == 0) {
						getMPIDebug().println("I am the Winner");
					}
				}
			}
		}
		MPI().Finalize();
	}
}
