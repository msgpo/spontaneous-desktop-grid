package ee.ut.f2f.core.mpi.internal;

public class BinomialTree {
	int numNode;
	int maxDegree;

	public BinomialTree(int numNode) {
		this.numNode = numNode;
		maxDegree = (int) Math.ceil(Math.log(numNode) / Math.log(2));
	}

	public int getMaxDegree() {
		return maxDegree;
	}

	public int getParentChildren(int myRank, int[] children) {
		int numBranch;
		int nodeValue;
		int childNode;
		int i;
		int rootBranch = 0;
		int findIndex = myRank;

		if (myRank == 0) {
			for (i = 0; i < maxDegree; i++) {
				children[i] = (int) Math.pow(2, i);
			}
			children[i] = -1;
			return -1;
		}

		while (true) {
			numBranch = (int) Math.floor(Math.log(findIndex) / Math.log(2));
			nodeValue = (int) Math.pow(2, numBranch);
			if ((rootBranch + nodeValue) == myRank) {
				if (numBranch == 0) {
					children[0] = -1;
				} else {
					for (i = 0; i < numBranch; i++) {
						childNode = myRank + (int) Math.pow(2, i);
						if (childNode < numNode) {
							children[i] = childNode;
						} else {
							children[i] = -1;
						}
					}
					children[i] = -1;
				}
				break;
			}
			rootBranch += nodeValue;
			findIndex = myRank - rootBranch;
		}

		return rootBranch;
	}

	public int getParentChildrenInverse(int myRank, int[] children) {
		int numBranch;
		int nodeValue;
		int childNode;
		int i;
		int rootBranch = 0;
		int findIndex = myRank;

		if (myRank == 0) {
			for (i = 0; i < maxDegree; i++) {
				children[maxDegree - i - 1] = (int) Math.pow(2, i);
			}
			children[i] = -1;
			return -1;
		}

		while (true) {
			numBranch = (int) Math.floor(Math.log(findIndex) / Math.log(2));
			nodeValue = (int) Math.pow(2, numBranch);
			if ((rootBranch + nodeValue) == myRank) {
				if (numBranch == 0) {
					children[0] = -1;
				} else {
					for (i = 0; i < maxDegree; i++)
						children[i] = -1;

					for (i = numBranch - 1; i >= 0; i--) {
						childNode = myRank + (int) Math.pow(2, i);
						if (childNode < numNode) {
							children[i] = childNode;
						}
					}
				}
				break;
			}
			rootBranch += nodeValue;
			findIndex = myRank - rootBranch;
		}

		return rootBranch;
	}
}
