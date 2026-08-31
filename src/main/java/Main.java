import java.util.Scanner;
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        while(true){

            System.out.print("$ ");
            String input = scanner.nextLine();
            if(input.equals ("exit")) {
                break;
            }
            else if(input.startsWith("echo ")) {
                System.out.println(input.substring(5));
            }
            else if(input.startsWith(("type"))){
                String command = input.substring(5);

                if(command.equals("echo") || command.equals("type") || command.equals("exit")){
                    System.out.println(command +" is a shell builtin");
                }

                else {
                    File executable = resolveExecutable(command);
                    if (executable != null) {
                        System.out.println(command + " is " + executable.getPath());
                    } else {
                        System.out.println(command + ": not found");
                    }
                }
            }
            else{
                String[] parts = input.split(" ");
                String command = parts[0];
                File executable = resolveExecutable(command);

                if (executable != null) {
                    ProcessBuilder pb = new ProcessBuilder(parts);
                    pb.inheritIO();
                    Process process = pb.start();
                    process.waitFor();
                } else {
                    System.out.println(input +": command not found");
                }
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