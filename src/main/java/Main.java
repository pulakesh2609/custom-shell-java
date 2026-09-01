import java.util.Scanner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        while (true) {

            System.out.print("$ ");
            String input = scanner.nextLine();

            // Detect and strip stdout redirection ( > or 1> )
            String[] tokens = input.split(" ");
            List<String> commandTokens = new ArrayList<>();
            String outputFile = null;

            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].equals(">") || tokens[i].equals("1>")) {
                    outputFile = tokens[i + 1];
                    break;
                }
                commandTokens.add(tokens[i]);
            }

            String commandLine = String.join(" ", commandTokens);
            PrintStream out = System.out;
            if (outputFile != null) {
                out = new PrintStream(new FileOutputStream(outputFile));
            }

            if (commandLine.equals("exit")) {
                break;
            } else if (commandLine.startsWith("echo ")) {
                out.println(commandLine.substring(5));
            } else if (commandLine.startsWith("type")) {
                String command = commandLine.substring(5);

                if (command.equals("echo") || command.equals("type") || command.equals("exit")) {
                    out.println(command + " is a shell builtin");
                } else {
                    File executable = resolveExecutable(command);
                    if (executable != null) {
                        out.println(command + " is " + executable.getPath());
                    } else {
                        out.println(command + ": not found");
                    }
                }
            } else {
                String command = commandTokens.get(0);
                File executable = resolveExecutable(command);

                if (executable != null) {
                    ProcessBuilder pb = new ProcessBuilder(commandTokens);
                    if (outputFile != null) {
                        pb.redirectOutput(new File(outputFile));
                        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
                    } else {
                        pb.inheritIO();
                    }
                    Process process = pb.start();
                    process.waitFor();
                } else {
                    out.println(commandLine + ": command not found");
                }
            }

            if (outputFile != null) {
                out.close();
            }
        }
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