/*
 The MIT License

 Copyright (c) 2005 
   1. Distributed Systems Group, University of Portsmouth
   2. Community Grids Laboratory, Indiana University

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be included
 in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ee.ut.f2f.core.mpi.internal;

public class ProcTree {

	public static final int PROCTREE_A = 4;
	public int numChildren;
	public int[] child = new int[PROCTREE_A];
	public int parent;
	public int root;
	public boolean isRoot;

	public ProcTree() {
		isRoot = false; // it was set to true ...
		numChildren = -1;
		for (int i = 0; i < child.length; i++) {
			child[i] = -1;
		}
		root = -1;
		parent = -1;
	}

	/**
	 * Build the tree depending on a process rank (index) and the group size (extent)
	 * 
	 * @param index
	 *            the rank of the calling process
	 * @param extent
	 *            the total number of processes
	 */
	public void buildTree(int index, int extent) {
		root = 0;
		int places = ProcTree.PROCTREE_A * index;

		for (int i = 1; i <= ProcTree.PROCTREE_A; i++) {
			++places;
			int ch = (ProcTree.PROCTREE_A * index) + i + root;
			ch %= extent;

			if (places < extent) {
				child[i - 1] = ch;
				numChildren++;
			}
		}

		if (index == root) {
			isRoot = true;
		} else {
			isRoot = false;
			int pr = (index - 1) / ProcTree.PROCTREE_A;
			parent = pr;
		}
	}

}
