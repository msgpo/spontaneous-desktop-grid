
#include <stdio.h>

int main() {


	long value = 111111111;

	char *memrep = &value;

	int i = 0;
	for(i=0; i<sizeof(value); i++) {

		printf("%d:%d\n",i,memrep[i]);

	}

	return 0;

}
