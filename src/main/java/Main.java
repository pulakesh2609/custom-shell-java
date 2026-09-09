import java.util.Scanner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    static String currentDir = System.getProperty("user.dir");

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        while (true) {

            System.out.print("$ ");
            String input = scanner.nextLine();

            List<String> tokens = tokenize(input);

            List<String> commandTokens = new ArrayList<>();
            String outputFile = null;
            String errorFile = null;
            boolean appendOutput = false;
            boolean appendError = false;

            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                if (token.equals(">") || token.equals("1>")) {
                    outputFile = tokens.get(i + 1);
                    appendOutput = false;
                    i++;
                } else if (token.equals(">>") || token.equals("1>>")) {
                    outputFile = tokens.get(i + 1);
                    appendOutput = true;
                    i++;
                } else if (token.equals("2>")) {
                    errorFile = tokens.get(i + 1);
                    appendError = false;
                    i++;
                } else if (token.equals("2>>")) {
                    errorFile = tokens.get(i + 1);
                    appendError = true;
                    i++;
                } else {
                    commandTokens.add(token);
                }
            }

            if (commandTokens.isEmpty()) {
                continue;
            }

            if (commandTokens.contains("|")) {
                runPipeline(commandTokens, outputFile, appendOutput, errorFile, appendError);
                continue;
            }

            PrintStream out = System.out;
            if (outputFile != null) {
                try {
                    out = new PrintStream(new FileOutputStream(outputFile, appendOutput));
                } catch (FileNotFoundException e) {
                    System.out.println(outputFile + ": No such file or directory");
                    continue;
                }
            }

            String command = commandTokens.get(0);

            if (command.equals("exit")) {
                break;
            }
            else if (command.equals("pwd")) {
                out.println(currentDir);
            }
            else if (command.equals("cd")) {
                String target = commandTokens.size() > 1
                        ? commandTokens.get(1)
                        : (System.getenv("HOME") != null ? System.getenv("HOME") : System.getenv("USERPROFILE"));

                if (target.equals("~")) {
                    target = System.getenv("HOME") != null ? System.getenv("HOME") : System.getenv("USERPROFILE");
                }

                File dir = new File(target).isAbsolute() ? new File(target) : new File(currentDir, target);

                if (dir.exists() && dir.isDirectory()) {
                    currentDir = dir.getCanonicalPath();
                } else {
                    System.out.println("cd: " + target + ": No such file or directory");
                }
            }
            else if (command.equals("echo")) {
                String output = String.join(" ", commandTokens.subList(1, commandTokens.size()));
                System.out.println(output);
            } else if (command.equals("type")) {
                String target = commandTokens.size() > 1 ? commandTokens.get(1) : "";
                if (target.equals("echo") || target.equals("type") || target.equals("exit")
                        || target.equals("pwd") || target.equals("cd")) {
                    System.out.println(target + " is a shell builtin");
                } else {
                    File executable = resolveExecutable(target);
                    if (executable != null) {
                        System.out.println(target + " is " + executable.getPath());
                    } else {
                        System.out.println(target + ": not found");
                    }
                }
            } else {
                File executable = resolveExecutable(command);

                if (executable != null) {
                    ProcessBuilder pb = new ProcessBuilder(commandTokens);
                    pb.directory(new File(currentDir));

                    if (outputFile != null) {
                        pb.redirectOutput(appendOutput
                                ? ProcessBuilder.Redirect.appendTo(new File(outputFile))
                                : ProcessBuilder.Redirect.to(new File(outputFile)));
                    } else {
                        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    }

                    if (errorFile != null) {
                        pb.redirectError(appendError
                                ? ProcessBuilder.Redirect.appendTo(new File(errorFile))
                                : ProcessBuilder.Redirect.to(new File(errorFile)));
                    } else {
                        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                    }

                    pb.redirectInput(ProcessBuilder.Redirect.INHERIT);

                    Process process = pb.start();
                    process.waitFor();
                } else {
                    out.println(command + ": command not found");
                }
            }

            if (outputFile != null) {
                out.close();
            }
        }
    }

    private static void runPipeline(List<String> commandTokens, String outputFile, boolean appendOutput,
                                    String errorFile, boolean appendError) throws Exception {
        List<List<String>> stages = new ArrayList<>();
        List<String> currentStage = new ArrayList<>();
        for (String token : commandTokens) {
            if (token.equals("|")) {
                stages.add(currentStage);
                currentStage = new ArrayList<>();
            } else {
                currentStage.add(token);
            }
        }
        stages.add(currentStage);

        byte[] inputData = null;

        for (int i = 0; i < stages.size(); i++) {
            List<String> stageTokens = stages.get(i);
            if (stageTokens.isEmpty()) continue;

            String command = stageTokens.get(0);
            boolean isLast = (i == stages.size() - 1);

            if (isPipelineBuiltin(command)) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                PrintStream stageOut;
                boolean closeStageOut = false;

                if (isLast) {
                    if (outputFile != null) {
                        stageOut = new PrintStream(new FileOutputStream(outputFile, appendOutput));
                        closeStageOut = true;
                    } else {
                        stageOut = System.out;
                    }
                } else {
                    stageOut = new PrintStream(buffer);
                }

                runPipelineBuiltin(stageTokens, stageOut);

                if (!isLast) {
                    stageOut.flush();
                    inputData = buffer.toByteArray();
                }
                if (closeStageOut) {
                    stageOut.close();
                }
            } else {
                File executable = resolveExecutable(command);
                if (executable == null) {
                    System.out.println(command + ": command not found");
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder(stageTokens);
                pb.directory(new File(currentDir));

                pb.redirectInput(i == 0 ? ProcessBuilder.Redirect.INHERIT : ProcessBuilder.Redirect.PIPE);

                if (isLast) {
                    pb.redirectOutput(outputFile != null
                            ? (appendOutput ? ProcessBuilder.Redirect.appendTo(new File(outputFile))
                            : ProcessBuilder.Redirect.to(new File(outputFile)))
                            : ProcessBuilder.Redirect.INHERIT);
                } else {
                    pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
                }

                if (isLast && errorFile != null) {
                    pb.redirectError(appendError
                            ? ProcessBuilder.Redirect.appendTo(new File(errorFile))
                            : ProcessBuilder.Redirect.to(new File(errorFile)));
                } else {
                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                }

                Process process = pb.start();

                if (i != 0) {
                    if (inputData != null) {
                        process.getOutputStream().write(inputData);
                    }
                    process.getOutputStream().close();
                }

                if (!isLast) {
                    inputData = process.getInputStream().readAllBytes();
                }

                process.waitFor();
            }
        }
    }

    private static boolean isPipelineBuiltin(String command) {
        return command.equals("pwd") || command.equals("cd") || command.equals("echo") || command.equals("type");
    }

    private static void runPipelineBuiltin(List<String> stageTokens, PrintStream out) throws IOException {
        String command = stageTokens.get(0);

        if (command.equals("pwd")) {
            out.println(currentDir);
        } else if (command.equals("cd")) {
            String target = stageTokens.size() > 1
                    ? stageTokens.get(1)
                    : (System.getenv("HOME") != null ? System.getenv("HOME") : System.getenv("USERPROFILE"));
            if (target.equals("~")) {
                target = System.getenv("HOME") != null ? System.getenv("HOME") : System.getenv("USERPROFILE");
            }
            File dir = new File(target).isAbsolute() ? new File(target) : new File(currentDir, target);
            if (dir.exists() && dir.isDirectory()) {
                currentDir = dir.getCanonicalPath();
            } else {
                System.out.println("cd: " + target + ": No such file or directory");
            }
        } else if (command.equals("echo")) {
            System.out.println(String.join(" ", stageTokens.subList(1, stageTokens.size())));
        } else if (command.equals("type")) {
            String target = stageTokens.size() > 1 ? stageTokens.get(1) : "";
            if (target.equals("echo") || target.equals("type") || target.equals("exit")
                    || target.equals("pwd") || target.equals("cd")) {
                System.out.println(target + " is a shell builtin");
            } else {
                File executable = resolveExecutable(target);
                if (executable != null) {
                    System.out.println(target + " is " + executable.getPath());
                } else {
                    System.out.println(target + ": not found");
                }
            }
        }
    }

    private static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean inToken = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inSingleQuotes) {
                if (c == '\'') {
                    inSingleQuotes = false;
                } else {
                    current.append(c);
                }
                continue;
            }

            if (inDoubleQuotes) {
                if (c == '"') {
                    inDoubleQuotes = false;
                } else if (c == '\\' && i + 1 < input.length() && isEscapableInDoubleQuotes(input.charAt(i + 1))) {
                    current.append(input.charAt(i + 1));
                    i++;
                } else {
                    current.append(c);
                }
                continue;
            }

            if (c == '\'') {
                inSingleQuotes = true;
                inToken = true;
            } else if (c == '"') {
                inDoubleQuotes = true;
                inToken = true;
            } else if (c == '\\' && i + 1 < input.length()) {
                current.append(input.charAt(i + 1));
                i++;
                inToken = true;
            } else if (Character.isWhitespace(c)) {
                if (inToken) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    inToken = false;
                }
            } else {
                current.append(c);
                inToken = true;
            }
        }

        if (inToken) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    private static boolean isEscapableInDoubleQuotes(char c) {
        return c == '"' || c == '\\' || c == '$' || c == '`';
    }

    private static File resolveExecutable(String command) {
        String path = System.getenv("PATH");
        String[] dirs = path.split(File.pathSeparator);

        for (String dir : dirs) {
            File file = new File(dir, command);
            if (file.exists() && file.canExecute()) {
                return file;
            }
        }
        return null;
    }
}