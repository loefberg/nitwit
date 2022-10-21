package com.github.loefberg.nitwit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class NativeGit {
    private static final Logger LOG = Logger.getLogger(NativeGit.class.getName());

    public static NativeGit newInstance() {
        return new NativeGit();
    }

    private NativeGit() {
    }

    public ExecutionResult run(Path workingDirectory, String... arguments) throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        commands.add("git");
        commands.addAll(List.of(arguments));
        ProcessBuilder pb = new ProcessBuilder(commands)
                .directory(workingDirectory.toFile());

        Process process = pb.start();

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<String> stdoutFuture = executorService.submit(new StreamReader(process.getInputStream()));
        Future<String> stderrFuture = executorService.submit(new StreamReader(process.getErrorStream()));

        executorService.shutdown();


        String stdout;
        String stderr;

        try {
            stdout = stdoutFuture.get();
            stderr = stderrFuture.get();
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }

        int errno = process.waitFor();
        if(errno != 0) {
            throw new IOException("Command exited with non zero exit value, stderr='" + stderr + "'");
        }

        LOG.info("native git: " + stdout);
        return new ExecutionResult(errno, stdout, stderr);
    }

    private static class StreamReader implements Callable<String> {
        private final InputStream input;

        public StreamReader(InputStream input) {
            this.input = input;
        }

        @Override
        public String call() throws Exception {
            StringBuilder builder = new StringBuilder();
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                reader.lines().forEach(line -> builder.append(line).append("\n"));
            }
            return builder.toString();
        }
    }

    public static class ExecutionResult {
        public final int errno;
        public final String stdout;
        public final String sederr;

        public ExecutionResult(int errno, String stdout, String sederr) {
            this.errno = errno;
            this.stdout = stdout;
            this.sederr = sederr;
        }
    }
}
