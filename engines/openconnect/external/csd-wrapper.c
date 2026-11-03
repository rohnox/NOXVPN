#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char **argv)
{
	const char *tmpdir = getenv("TMPDIR");
	char *script;
	char **shell_argv;
	int i;

	if (!tmpdir || asprintf(&script, "%s/csd-wrapper.sh", tmpdir) < 0) {
		fprintf(stderr, "Unable to locate CSD wrapper script\n");
		return 1;
	}

	shell_argv = calloc((size_t)argc + 2, sizeof(*shell_argv));
	if (!shell_argv) {
		free(script);
		return 1;
	}

	shell_argv[0] = (char *)"/system/bin/sh";
	shell_argv[1] = script;
	for (i = 1; i < argc; i++)
		shell_argv[i + 1] = argv[i];

	execv(shell_argv[0], shell_argv);
	fprintf(stderr, "Unable to run CSD wrapper: %s\n", strerror(errno));
	free(shell_argv);
	free(script);
	return 1;
}
