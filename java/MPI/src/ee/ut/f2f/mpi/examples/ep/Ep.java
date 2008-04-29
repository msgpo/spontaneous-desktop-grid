package ee.ut.f2f.mpi.examples.ep;

import ee.ut.f2f.core.mpi.MPI;
import ee.ut.f2f.core.mpi.MPITask;

public class Ep extends MPITask {
      double[] start;
      double[] elapsed;
      double[] x;
      double[] q;
      double[] qq;
      double[] dum = {1.,1.,1.};
      double x1, x2, sx, sy, tm, an, tt, gc;
      double Mops;
      double epsilon=1.0E-8, a = 1220703125., s=271828183.;
      double t1, t2, t3, t4; 
      double timer_read;
      double sx_verify_value, sy_verify_value, sx_err, sy_err;
      String _class = "A"; //FIXME : how to define class A,S, ?
      int    mk=16, 
             // --> set by make : in npbparams.h
             //m=28, // for CLASS=A
             m=30, // for CLASS=B
             //npm=2, // NPROCS
             mm = m-mk, 
             nn = (int)(Math.pow(2,mm)), 
             nk = (int)(Math.pow(2,mk)), 
             nq=10, 
             np, 
             node, 
             no_nodes, 
             i, 
             ik, 
             kk, 
             l, 
             k, nit, no_large_nodes,
             np_add, k_offset, j;
      int    me, nprocs, root=0, dp_type;
      boolean verified, 
              timers_enabled=true;
      String size;

      //Use in randlc..
      int KS = 0;
      double R23, R46, T23, T46;


      public Ep() {
    	  super();
      	qq = new double[10000];
	start = new double[64];
	elapsed = new double[64];
      }


      public void runTask() {
      x = new double [2*nk];
      q = new double [nq];   // [0:nq-1];

      MPI().Init();
      no_nodes = MPI().COMM_WORLD().Size();
      node     = MPI().COMM_WORLD().Rank();

      root = 0;
      if (node == root ) {
              
/*   Because the size of the problem is too large to store in a 32-bit
 *   integer for some classes, we put it into a string (for printing).
 *   Have to strip off the decimal point put in there by the floating
 *   point print statement (internal file)
 */
          getMPIDebug().println(" NAS Parallel Benchmarks 3.2 -- EP Benchmark");
          size = new String(Double.toString(Math.pow(2,m+1)));
	  //size = size.replace('.', ' ');
          getMPIDebug().println(" Number of random numbers generated: "+size);
          getMPIDebug().println(" Number of active processes: "+no_nodes);

      }
      verified = false;

/* c   Compute the number of "batches" of random number pairs generated 
   c   per processor. Adjust if the number of processors does not evenly 
   c   divide the total number
*/

       np = nn / no_nodes;
       no_large_nodes = nn % no_nodes;
       if (node < no_large_nodes) np_add = 1;
       else np_add = 0;
       np = np + np_add;

       if (np == 0) {
             getMPIDebug().println("Too many nodes:" + no_nodes + " "+  nn);
             // call mpi_abort(MPI_COMM_WORLD,ierrcode,ierr)
             System.exit(0); 
       } 

/* c   Call the random number generator functions and initialize
   c   the x-array to reduce the effects of paging on the timings.
   c   Also, call all mathematical functions that are used. Make
   c   sure these initializations cannot be eliminated as dead code.
*/

        //call vranlc(0, dum[1], dum[2], dum[3]);
        // Array indexes start at 1 in Fortran, 0 in Java
        vranlc(0, dum[0], dum[1], dum);  // appel avec n=0 donc y[] pas lu dans vranlc()

	// Change dum[1] to Object dumm //
	double[] dumm = new double[1];
	dumm[0] = dum[1];
        dum[0] = randlc(dumm,dum[2]);
	dum[1] = dumm[0];
	/////////////////////////////////
        for (i=0;i<2*nk;i++) {
              x[i] = -1e99;
              // FIXME : Mops = Math.log(Math.sqrt(Math.abs(max(1.,1.))))
	        // Do we need max ??
              // Peut etre est ce une operation "qui ne fait rien" pour prendre 
              // du temps de calcul ?
              Mops = Math.log(Math.sqrt(Math.abs(1))); // => resultat constant : Mops = 0
        }
/*
   c---------------------------------------------------------------------
   c    Synchronize before placing time stamp
   c---------------------------------------------------------------------
*/
        MPI().COMM_WORLD().Barrier();

        timer_clear(1);
        timer_clear(2);
        timer_clear(3);
        timer_start(1);
        
        t1 = a;
	//getMPIDebug().println("(ep.f:160) t1 = " + t1);
        t1 = vranlc(0, t1, a, x);
	//getMPIDebug().println("(ep.f:161) t1 = " + t1);
	
        
/* c   Compute AN = A ^ (2 * NK) (mod 2^46). */
        
        t1 = a;
	//getMPIDebug().println("(ep.f:165) t1 = " + t1);
	double[] Ot1 = new double[1]; //Object T1
        for (i=1; i <= mk+1; i++) {
	       //Change t1 to Object
	       Ot1[0] = t1;
               t2 = randlc(Ot1, Ot1[0]);
	       t1 = Ot1[0];
	       //getMPIDebug().println("(ep.f:168)[loop i=" + i +"] t1 = " + t1);
        } 
        an = t1;
	//getMPIDebug().println("(ep.f:172) s = " + s);
        tt = s;
        gc = 0.;
        sx = 0.;
        sy = 0.;
        for (i=0; i < nq ; i++) {
               q[i] = 0.;
        }

/*
    Each instance of this loop may be performed independently. We compute
    the k offsets separately to take into account the fact that some nodes
    have more numbers to generate than others
*/

      if (np_add == 1)
         k_offset = node * np -1;
      else
         k_offset = no_large_nodes*(np+1) + (node-no_large_nodes)*np -1;
     
      boolean stop = false;
      for(k = 1; k <= np; k++) {
         stop = false;
         kk = k_offset + k ;
         t1 = s;
         //getMPIDebug().println("(ep.f:193) t1 = " + t1);
         t2 = an;

//       Find starting seed t1 for this kk.

	 double[] Ot2 = new double[1];
         for (i=1;i<=100 && !stop;i++) {
            ik = kk / 2;
	    //getMPIDebug().println("(ep.f:199) ik = " +ik+", kk = " + kk);
            if (2 * ik != kk)  {
                Ot1[0] = t1;
                t3 = randlc(Ot1, t2);
                t1 = Ot1[0];
                //getMPIDebug().println("(ep.f:200) t1= " +t1 );
            }
            if (ik==0)
                stop = true;
            else {
               Ot2[0] = t2;
               t3 = randlc(Ot2, Ot2[0]);
               t2 = Ot2[0];
               kk = ik;
           }
         }
//       Compute uniform pseudorandom numbers.

         //if (timers_enabled)  timer_start(3);
	 timer_start(3);
         //call vranlc(2 * nk, t1, a, x)  --> t1 and y are modified

	//getMPIDebug().println(">>>>>>>>>>>Before vranlc(l.210)<<<<<<<<<<<<<");
	//getMPIDebug().println("2*nk = " + (2*nk));
	//getMPIDebug().println("t1 = " + t1);
	//getMPIDebug().println("a  = " + a);
	//getMPIDebug().println("x[0] = " + x[0]);
	//getMPIDebug().println(">>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<");
        
	t1 = vranlc(2 * nk, t1, a, x);

	//getMPIDebug().println(">>>>>>>>>>>After  Enter vranlc (l.210)<<<<<<");
	//getMPIDebug().println("2*nk = " + (2*nk));
	//getMPIDebug().println("t1 = " + t1);
	//getMPIDebug().println("a  = " + a);
	//getMPIDebug().println("x[0] = " + x[0]);
	//getMPIDebug().println(">>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<");
        
         //if (timers_enabled)  timer_stop(3);
	 timer_stop(3);

/*       Compute Gaussian deviates by acceptance-rejection method and 
 *       tally counts in concentric square annuli.  This loop is not 
 *       vectorizable. 
 */
         //if (timers_enabled) timer_start(2);
 	 timer_start(2);
         for(i=1; i<=nk;i++) {
            x1 = 2. * x[2*i-2] -1.0;
            x2 = 2. * x[2*i-1] - 1.0;
            t1 = x1*x1 + x2*x2;
            if (t1 <= 1.) {
               t2   = Math.sqrt(-2. * Math.log(t1) / t1);
               t3   = (x1 * t2);
               t4   = (x2 * t2);
               l    = (int)(Math.abs(t3) > Math.abs(t4) ? Math.abs(t3) : Math.abs(t4));
               q[l] = q[l] + 1.;
               sx   = sx + t3;
               sy   = sy + t4;
             }
		/*
	     if(i == 1) {
                getMPIDebug().println("x1 = " + x1);
                getMPIDebug().println("x2 = " + x2);
                getMPIDebug().println("t1 = " + t1);
                getMPIDebug().println("t2 = " + t2);
                getMPIDebug().println("t3 = " + t3);
                getMPIDebug().println("t4 = " + t4);
                getMPIDebug().println("l = " + l);
                getMPIDebug().println("q[l] = " + q[l]);
                getMPIDebug().println("sx = " + sx);
                getMPIDebug().println("sy = " + sy);
	     }
		*/
           }
         //if (timers_enabled)  timer_stop(2);
 	 timer_stop(2);
      }

      double[] Osx = new double[1];
	double[] Osy = new double[1];
         
	Osx[0] = sx;
	Osy[0] = sy;
	MPI().COMM_WORLD().Allreduce(Osx, 0, x, 0, 1, MPI.DOUBLE, MPI.SUM);
	sx = x[0]; //FIXME :  x[0] or x[1] => x[0] because fortran starts with 1
      MPI().COMM_WORLD().Allreduce(Osy, 0, x, 0, 1, MPI.DOUBLE, MPI.SUM);
      sy = x[0];
      MPI().COMM_WORLD().Allreduce(q, 0, x, 0, nq, MPI.DOUBLE, MPI.SUM);

      for(i = 0; i < nq; i++) {
		q[i] = x[i];
	}
	for(i = 0; i < nq; i++) {
		gc += q[i];
	}

	timer_stop(1);
        tm = timer_read(1);
	double[] Otm = new double[1];
	Otm[0] = tm;
	MPI().COMM_WORLD().Allreduce(Otm, 0, x, 0, 1, MPI.DOUBLE, MPI.MAX);
	tm = x[0];

	if(node == root) {
		nit = 0;
		verified = true;

		if(m == 24) {
         		sx_verify_value = -3.247834652034740E3;
	                sy_verify_value = -6.958407078382297E3;
		} else if(m == 25) {
	            	sx_verify_value = -2.863319731645753E3;
			sy_verify_value = -6.320053679109499E3;
		} else if(m == 28) {
		        sx_verify_value = -4.295875165629892E3;
			sy_verify_value = -1.580732573678431E4;
		} else if(m == 30) {
	        	sx_verify_value =  4.033815542441498E4;
	                sy_verify_value = -2.660669192809235E4;
		} else if(m == 32) {
	                sx_verify_value =  4.764367927995374E4;
                   	sy_verify_value = -8.084072988043731E4;
		} else if(m == 36) {
		        sx_verify_value =  1.982481200946593E5;
		        sy_verify_value = -1.020596636361769E5;
		} else {
			verified = false;
		}

		/*
		getMPIDebug().println("sx        = " + sx);
		getMPIDebug().println("sx_verify = " + sx_verify_value);
		getMPIDebug().println("sy        = " + sy);
		getMPIDebug().println("sy_verify = " + sy_verify_value);
		*/
		if(verified) {
			sx_err = Math.abs((sx - sx_verify_value)/sx_verify_value);
			sy_err = Math.abs((sy - sy_verify_value)/sy_verify_value);
			/*
			getMPIDebug().println("sx_err = " + sx_err);
			getMPIDebug().println("sy_err = " + sx_err);
			getMPIDebug().println("epsilon= " + epsilon);
			*/
			verified = ((sx_err < epsilon) && (sy_err < epsilon));
		}

		//Mops = (Math.pow(2.0, m+1))/tm/1000000;  //per second but tm is millisecond
		Mops = (Math.pow(2.0, m+1))/tm/1000;

		getMPIDebug().println("EP Benchmark Results:");
		getMPIDebug().println("CPU Time=" + tm);
		getMPIDebug().println("N = 2^" + m);
		getMPIDebug().println("No. Gaussain Pairs =" + gc);
		getMPIDebug().println("Sum = " + sx + " " + sy);
		getMPIDebug().println("Count:");
		for(i = 0; i < nq; i++) {
			getMPIDebug().println(""+i+"\t"+q[i]);
		}

		/*
		print_results("EP", _class, m+1, 0, 0, nit, npm, no_nodes, tm, Mops,
				"Random numbers generated", verified, npbversion,
				compiletime, cs1, cs2, cs3, cs4, cs5, cs6, cs7) */
		getMPIDebug().println("\n");
		getMPIDebug().println("EP Benchmark Completed\n");
                getMPIDebug().println("Class           = " + _class);
		getMPIDebug().println("Size            = " + size);
		getMPIDebug().println("Iteration       = " + nit);
		getMPIDebug().println("Time in seconds = " + (tm/1000));
		getMPIDebug().println("Total processes = " + no_nodes);
		getMPIDebug().println("Mops/s total    = " + Mops);
		getMPIDebug().println("Mops/s/process  = " + Mops/no_nodes);
		getMPIDebug().println("Operation type  = Random number generated");
		if(verified) {
			getMPIDebug().println("Verification    = SUCCESSFUL");
		} else {
			getMPIDebug().println("Verification    = UNSUCCESSFUL");
		}
           	getMPIDebug().println("Total time:     " + (timer_read(1)/1000));
           	getMPIDebug().println("Gaussian pairs: " + (timer_read(2)/1000));
           	getMPIDebug().println("Random numbers: " + (timer_read(3)/1000));
       	}

       MPI().Finalize();
      }
      // ----------------------- timers ---------------------
      public void timer_clear(int n) {
            elapsed[n] = 0.0;
      }

      public void timer_start(int n) {
            start[n] = MPI().Wtime();
      }

      public void timer_stop(int n) {
            double t, now;

            now = MPI().Wtime();
            t = now - start[n];
            elapsed[n] += t;
      }

      public double timer_read(int n) {
            return(elapsed[n]);
      }

       /**********************************************************************
       *****************            R A N D L C            ******************
       *****************                                   ******************
       *****************  portable random number generator ******************
       *********************************************************************/
public double randlc(Object X, double A) {

	long Lx, La;
	double d2m46 = Math.pow(0.5, 46);
      long i246m1= Long.parseLong("00003FFFFFFFFFFF",16);  //data i246m1/X'00003FFFFFFFFFFF'/

	double[] DX = (double[])X;
	Double LAX = new Double(DX[0]);
     
	Lx = LAX.longValue();
	La = (long)A;

	Lx = (Lx*La) & i246m1;
        //randlc = d2m46*Lx;
        DX[0] = (double)Lx;

	return (d2m46*Lx);
}
/*
       public double randlc(Object X, double A)
       {
            double      T1, T2, T3, T4;
            double      A1;
            double      A2;
            double      X1;
            double      X2;
            double      Z;
            int   i, j;

            double[] lX = (double[])X;

            if(KS == 0) {
                  R23 = 1.0;
                  R46 = 1.0;
                  T23 = 1.0;
                  T46 = 1.0;

                  for(i = 1; i <= 23; i++) {
                        R23 = 0.50 * R23;
                        T23 = 2.0  * T23;
                  }
                  for(i = 1; i <= 46; i++) {
                        R46 = 0.50 * R46;
                        T46 = 2.0  * T46;
                  }
                  KS = 1;
            }
           // Break A into two parts such that A = 2^23 * A1 + A2 and set X = N 
            T1 = R23 * A;
            j = (int)T1;
            A1 = j;
            A2 = A - T23 * A1;

            // Break X into two parts such that X = 2^23 * X1 + X2, compute
             //* Z = A* X2 + A2 * X1 (mod 2^23) and then
             //* X = 2^23 * Z + A2 * X2 (mod 2^46)
            T1 = R23 * lX[0];
            j  = (int)T1;
            X1 = j;
            X2 = lX[0] - T23 * X1;
            T1 = A1 * X2 + A2 * X1;

            j = (int)(R23 * T1);
            T2 = j;
            Z = T1 - T23 * T2;
            T3 = T23 * Z + A2 * X2;
            j  = (int)(R46 * T3);
            T4 = j;
            lX[0] = T3 - T46 * T4;

            return (R46 * lX[0]);
      }
	*/
      /********************************************************************
       *****************            V R A N L C          ******************
       *****************                                 *****************/           
      public double vranlc(int n, double x, double a, double [] y)
      {
        int i;
        long  i246m1, Lx, La;
        double d2m46;

// This doesn't work, because the compiler does the calculation in 32
// bits and overflows. No standard way (without f90 stuff) to specify
// that the rhs should be done in 64 bit arithmetic.
//     parameter(i246m1=2**46-1)

      i246m1= Long.parseLong("00003FFFFFFFFFFF",16);  //data i246m1/X'00003FFFFFFFFFFF'/
      d2m46=Math.pow(0.5,46);

// c Note that the v6 compiler on an R8000 does something stupid with
// c the above. Using the following instead (or various other things)
// c makes the calculation run almost 10 times as fast.
//
// c     save d2m46
// c      data d2m46/0.0d0/
// c      if (d2m46 .eq. 0.0d0) then
// c         d2m46 = 0.5d0**46
// c      endif

      Lx = (long)x;
      La = (long)a;
      //getMPIDebug().println("================== Vranlc ================");
      //getMPIDebug().println("Before Loop: Lx = " + Lx + ", La = " + La);
      Long LLX = new Long(Lx);
      for (i=0; i< n; i++) {
         Lx   = Lx*La & i246m1 ;
      	 LLX = new Long(Lx);
         y[i] = d2m46 * LLX.doubleValue();
	 /*
         if(i == 0) {
             getMPIDebug().println("After loop 0:");
	     getMPIDebug().println("Lx = " + Lx + ", La = " + La);
	     getMPIDebug().println("d2m46 = " + d2m46);
	     getMPIDebug().println("LLX(Lx) = " + LLX.doubleValue());
	     getMPIDebug().println("Y[0]" + y[0]);
         }
	 */
      }

      x = LLX.doubleValue();
      /*
      getMPIDebug().println("Change: Lx = " + Lx);
      getMPIDebug().println("=============End   Vranlc ================");
      */
      return x;
    }
}
