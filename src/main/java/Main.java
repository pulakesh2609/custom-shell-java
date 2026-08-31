import java.sql.SQLOutput;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.print("$ ");

        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        System.out.print(input + ": command not found");

        while(true){

            System.out.println("$ ");
            String input = scanner.nextLine();
            System.out.println(input + ": commnad not found");
        }

    }
}
