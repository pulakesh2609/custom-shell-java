import java.sql.SQLOutput;
import java.util.Scanner;

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

                if(input.startsWith("echo") || input.startsWith("type") || input.startsWith("exit")){
                    System.out.println(input +": is a shell builtin");
                }
                else{
                    System.out.println(input + ": not found");
                }
            }
            else{
                    System.out.println(input +": command not found");
                }
        }

    }
}
