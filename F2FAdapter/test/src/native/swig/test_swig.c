#include <stdio.h>


int fact(int n) {
	if (n <= 1 ) return 1;
	else return (n*fact(n-1));
}

/*
void main() {
	int index = 0;
	while (index < 0) {
		printf("fact(d%) == %d", index, fact(index));
	}
}
*/
