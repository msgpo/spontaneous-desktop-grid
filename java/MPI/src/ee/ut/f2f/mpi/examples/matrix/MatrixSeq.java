package ee.ut.f2f.mpi.examples.matrix;

public class MatrixSeq {
	public static void main(String[] args) {
		int N = 1500;
		int[] a = new int[N * N];
		int[] b = new int[N * N];
		int[] c = new int[N * N];
		long start, stop;
		// Init Matrix A, B
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				a[(i * N) + j] = 1;
				b[(i * N) + j] = 2;
			}
		}

		// Calculation: C = A * B
		start = System.currentTimeMillis();
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				c[(i * N) + j] = 0;
				for (int k = 0; k < N; k++)
					c[(i * N) + j] = c[(i * N) + j] + a[(i * N) + k] * b[(k * N) + j];
			}
		}
		stop = System.currentTimeMillis();
		System.out.println("Calculation Time = " + (stop - start));

		/*
		 * for(int i = 0; i < N; i++) { for(int j = 0; j < N; j++) { if(c[(i*N)+j] != 2000) { System.out.println("Error: result is not correct"); System.out.exit(1); } } }
		 */
		System.out.println("c[0] = " + c[0] + "c[100*100] = " + c[1000 * 1000]);
		System.out.println("SUCCESSFUL");

	}
}
